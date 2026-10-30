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
