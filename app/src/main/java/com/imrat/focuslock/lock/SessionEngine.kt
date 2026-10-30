package com.imrat.focuslock.lock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.data.LockSession
import com.imrat.focuslock.data.Schedule
import com.imrat.focuslock.data.SessionKind
import com.imrat.focuslock.data.SessionRepository
import com.imrat.focuslock.data.SessionStatus
import com.imrat.focuslock.security.KeystoreBox
import com.imrat.focuslock.security.LocalSession
import com.imrat.focuslock.security.SessionStore
import com.imrat.focuslock.service.SessionEndReceiver
import com.imrat.focuslock.time.ServerClock
import java.io.File
import kotlinx.coroutines.launch

/**
 * Starts, restores and ends sessions on this phone. Local store is the truth for enforcement (LOCK-6);
 * Firestore is informed best-effort. All ends use the monotonic clock (LOCK-8).
 */
class SessionEngine(ctx: Context) {
    private val app = ctx.applicationContext
    private val store = SessionStore(File(app.filesDir, "sessions.bin"), KeystoreBox.create())
    val clock = ServerClock(app)

    /** Self instant lock (LOCK-1). Enforced immediately from local state; uploaded when possible. */
    @Synchronized
    fun startSelfInstant(userUid: String, mode: LockMode, packages: List<String>, durationMs: Long): LocalSession? {
        if (durationMs <= 0 || (mode == LockMode.APPS && packages.isEmpty())) return null
        val repo = SessionRepository(FirebaseFirestore.getInstance())
        val id = repo.newId(userUid)
        val nowEl = SystemClock.elapsedRealtime()
        val srvNow = clock.now()
        val s = SessionPlan.build(id, userUid, mode, packages, durationMs, srvNow, nowEl)
        store.save(SessionPlan.prune(store.load(), nowEl) + s)
        scheduleEnd(s)
        val doc = LockSession(
            id, userUid, mode, SessionKind.INSTANT, packages, srvNow, s.endAtServerMs, SessionStatus.ACTIVE,
            null, null, null, remoteInstant = false, passwordUsed = false,
        )
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { repo.createWithId(userUid, id, doc) }.onFailure { Log.w(TAG, "upload failed: ${it.message}") }
        }
        return s
    }

    /** Starts a session an Observer made for this phone (UNL-1, NOT-2). Idempotent: known ids are skipped. */
    @Synchronized
    fun startRemote(s: LockSession): LocalSession? {
        val srv = clock.now()
        val el = SystemClock.elapsedRealtime()
        if (s.endAtMs <= srv || store.load().any { it.id == s.id }) return null
        val local = SessionPlan.build(s.id, s.createdBy, s.mode, s.packages, s.endAtMs - srv, srv, el, s.pwHash, s.pwSalt)
        store.save(SessionPlan.prune(store.load(), el) + local)
        scheduleEnd(local)
        return local
    }

    private val donePrefs = app.getSharedPreferences("done_sessions", Context.MODE_PRIVATE)

    /** Sessions that already ended here; a server doc that still looks live must never restart them. */
    fun doneIds(): Set<String> = donePrefs.getStringSet("ids", emptySet()).orEmpty()

    private fun markDone(id: String) {
        donePrefs.edit().putStringSet("ids", (doneIds() + id).toList().takeLast(200).toSet()).apply()
    }

    /** Observer ended it on the server: drop it from the local cache so the lock lifts. */
    @Synchronized
    fun endLocal(id: String) {
        store.remove(id)
        markDone(id)
    }

    /** Lock-screen password try (UNL-3, UNL-4). Success ends the session for good. Verifies off the main thread, please. */
    @Synchronized
    fun attemptUnlock(id: String, entered: String): UnlockPolicy.Outcome {
        val all = store.load()
        val s = all.firstOrNull { it.id == id } ?: return UnlockPolicy.Outcome.NoPassword
        val r = UnlockPolicy.attempt(s, entered, SystemClock.elapsedRealtime())
        if (r.outcome is UnlockPolicy.Outcome.Success) {
            store.remove(id)
            markDone(id)
            val me = FirebaseAuth.getInstance().currentUser?.uid
            if (me != null) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    runCatching { SessionRepository(FirebaseFirestore.getInstance()).markPasswordUsed(me, id) }
                }
            }
        } else {
            store.save(all.map { if (it.id == id) r.session else it })
        }
        return r.outcome
    }

    /** UNL-7: begin the 5-minute emergency-exit countdown of a self-lock. */
    @Synchronized
    fun startExit(id: String) = store.update(id) { UnlockPolicy.startExit(it, SystemClock.elapsedRealtime()) }

    /** UNL-7: end self-locks whose emergency-exit countdown finished. */
    @Synchronized
    fun processExits() {
        val el = SystemClock.elapsedRealtime()
        store.load().filter { UnlockPolicy.exitDue(it, el) }.forEach {
            store.remove(it.id)
            markDone(it.id)
            markEnded(it)
        }
    }

    fun local(): List<LocalSession> = store.load()

    fun localIds(): Set<String> = store.load().map { it.id }.toSet() + doneIds()

    /**
     * Starts the occurrence [p] of [sch] if it is running now and was never started before (idempotent, SCH-3).
     * Ends at the occurrence end. Only the creator's own phone-side schedule is uploaded (rules: createdBy == auth uid).
     */
    @Synchronized
    fun startScheduled(sch: Schedule, p: Planned): LocalSession? {
        val srv = clock.now()
        val el = SystemClock.elapsedRealtime()
        if (!p.isRunning(srv)) return null
        val prefs = app.getSharedPreferences("sched_started", Context.MODE_PRIVATE)
        val done = prefs.getStringSet("keys", emptySet()).orEmpty()
        if (p.key in done) return null
        prefs.edit().putStringSet("keys", (done + p.key).toList().takeLast(60).toSet()).apply()
        val s = SessionPlan.build(p.key, sch.createdBy, sch.mode, sch.packages, p.endMs - srv, srv, el)
        store.save(SessionPlan.prune(store.load(), el) + s)
        scheduleEnd(s)
        val me = FirebaseAuth.getInstance().currentUser?.uid
        if (me != null && sch.createdBy == me) {
            val doc = LockSession(
                p.key, me, sch.mode, SessionKind.SCHEDULED, sch.packages, p.startMs, p.endMs, SessionStatus.ACTIVE,
                null, null, null, remoteInstant = false, passwordUsed = false,
            )
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                runCatching { SessionRepository(FirebaseFirestore.getInstance()).createWithId(me, p.key, doc) }
            }
        }
        return s
    }

    /** Boot: monotonic clock restarted, rebase every stored session (LOCK-7, LOCK-8). */
    @Synchronized
    fun onBoot() {
        clock.invalidate()
        val el = SystemClock.elapsedRealtime()
        val list = SessionPlan.rebaseAfterBoot(store.load(), clock.now(), el, synced = false)
        store.save(list)
        list.forEach(::scheduleEnd)
    }

    /** Heartbeat: persist remaining time, drop expired sessions, mark own self-locks ended. */
    @Synchronized
    fun heartbeat() {
        val el = SystemClock.elapsedRealtime()
        processExits()
        val all = store.load()
        if (all.isEmpty()) return
        val expired = all.filter { it.endElapsedMs <= el }
        store.save(SessionPlan.refreshRemaining(SessionPlan.prune(all, el), el))
        expired.forEach(::markEnded)
    }

    /** Learn server time (blocking). On success re-derive session ends from it. */
    @Synchronized
    fun syncClock(): Boolean {
        if (!clock.sync()) return false
        val el = SystemClock.elapsedRealtime()
        val all = store.load()
        if (all.isNotEmpty()) store.save(SessionPlan.resync(all, clock.now(), el))
        return true
    }

    /** Re-arm end alarms for everything stored (service start, boot). */
    @Synchronized
    fun rearmAlarms() = store.load().forEach(::scheduleEnd)

    private fun markEnded(s: LocalSession) {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (s.createdBy != me) return // only the creator-User self-lock may be ended by the phone (rules)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { SessionRepository(FirebaseFirestore.getInstance()).endBySelf(me, s.id) }
        }
    }

    private fun scheduleEnd(s: LocalSession) {
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            app, s.id.hashCode(), Intent(app, SessionEndReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        if (am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, s.endElapsedMs, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, s.endElapsedMs, pi)
        }
    }

    private companion object { const val TAG = "SessionEngine" }
}
