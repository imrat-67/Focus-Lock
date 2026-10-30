package com.imrat.focuslock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.imrat.focuslock.lock.SessionEngine
import com.imrat.focuslock.security.KeystoreBox
import com.imrat.focuslock.security.SessionStore
import java.io.File
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.imrat.focuslock.MainActivity
import com.imrat.focuslock.R

/**
 * Always-on foreground service (NOT-4: one silent, minimal notification).
 * Skeleton in T05; Firestore listeners, alarms and session restore are added in T08/T10.
 */
class CoreService : Service() {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    private var appSync: AppListSync? = null
    private var heartbeat: kotlinx.coroutines.Job? = null
    private var scheduleJob: kotlinx.coroutines.Job? = null
    private var watcherJob: kotlinx.coroutines.Job? = null
    private var remoteJob: kotlinx.coroutines.Job? = null
    private var alertJob: kotlinx.coroutines.Job? = null
    private var cancelJob: kotlinx.coroutines.Job? = null
    private var poolJob: kotlinx.coroutines.Job? = null
    private val pendingStarts = mutableMapOf<String, kotlinx.coroutines.Job>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        appSync?.stop()
        scope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        restoreSessions()
        if (appSync == null) appSync = AppListSync(this, scope).also { it.start() } else appSync?.requestSync()
        return START_STICKY
    }

    /** Loads the encrypted session cache (creates it empty on first run). Enforcement hooks in during T06/T08. */
    private fun restoreSessions() {
        val file = File(filesDir, "sessions.bin")
        val store = SessionStore(file, KeystoreBox.create())
        if (!file.exists()) store.save(emptyList())
        Log.i("CoreService", "restored ${store.load().size} session(s)")
        val engine = SessionEngine(this)
        engine.rearmAlarms()
        val scheduler = ScheduleScheduler(this)
        scheduler.rearm()
        if (scheduleJob == null) scheduleJob = scope.launch { watchSchedules(scheduler) }
        if (remoteJob == null) remoteJob = scope.launch { watchRemote(engine) }
        if (cancelJob == null) cancelJob = scope.launch { watchScheduleCancels(engine) }
        if (alertJob == null) alertJob = scope.launch { watchLinkAlerts() }
        if (poolJob == null) poolJob = scope.launch { watchPasswordPools() }
        if (watcherJob == null) {
            val watcher = com.imrat.focuslock.lock.ForegroundWatcher(this, com.imrat.focuslock.lock.SessionCache(this))
            watcherJob = scope.launch { while (true) { delay(runCatching { watcher.step() }.getOrDefault(3_000L)) } }
        }
        if (heartbeat == null) {
            heartbeat = scope.launch {
                var tick = 0
                while (true) {
                    runCatching {
                        engine.heartbeat()
                        if (tick % 20 == 0 && engine.syncClock()) scheduler.rearm() // ~every 10 min, and at start
                    }
                    tick++
                    delay(30_000)
                }
            }
        }
    }

    /** Mirrors this user's schedules (own + Observer-created) into the local cache and re-arms alarms (SCH-3). */
    private suspend fun watchSchedules(scheduler: ScheduleScheduler) {
        while (true) {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                runCatching {
                    com.imrat.focuslock.data.ScheduleRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance())
                        .observe(uid).collect { list -> scheduler.save(list); scheduler.rearm(list) }
                }.onFailure { Log.w("CoreService", "schedule listener: ${it.message}") }
            }
            delay(60_000) // not signed in yet, or listener failed: retry
        }
    }

    /**
     * Mirrors sessions an Observer made for this phone: 1-minute warning, delayed local start, local end when the
     * Observer unlocks (UNL-1, NOT-2). Needs the phone to be online; enforcement itself stays local.
     */
    private suspend fun watchRemote(engine: SessionEngine) {
        val repo = com.imrat.focuslock.data.SessionRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance())
        while (true) {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                runCatching {
                    repo.observeLive(uid, engine.clock.now() - 60_000).collect { list ->
                        val hashes = engine.local().associate { it.id to it.pwHash }
                        val plan = com.imrat.focuslock.data.RemoteSync.plan(list, engine.localIds(), uid, engine.clock.now(), hashes)
                        plan.endIds.forEach { engine.endLocal(it) }
                        plan.rehash.forEach { engine.updateHash(it.id, it.hash, it.salt) }
                        val wanted = plan.starts.map { it.session.id }.toSet()
                        pendingStarts.filterKeys { it !in wanted }.forEach { (id, job) -> job.cancel(); pendingStarts.remove(id) }
                        plan.starts.filter { it.session.id !in pendingStarts }.forEach { st ->
                            pendingStarts[st.session.id] = scope.launch { startAfterWarning(engine, st) }
                        }
                    }
                }.onFailure { Log.w("CoreService", "session listener: ${it.message}") }
            }
            delay(60_000)
        }
    }

    /** SCH-4: an Observer deleting or disabling their schedule ends its running session (needs a server-confirmed list). */
    private suspend fun watchScheduleCancels(engine: SessionEngine) {
        val repo = com.imrat.focuslock.data.ScheduleRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance())
        while (true) {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                runCatching {
                    repo.observeConfirmed(uid).collect { list ->
                        com.imrat.focuslock.data.ScheduleCancel.idsToEnd(engine.local(), list, uid).forEach { engine.endLocal(it) }
                    }
                }.onFailure { Log.w("CoreService", "schedule cancel listener: ${it.message}") }
            }
            delay(60_000)
        }
    }

    /**
     * UNL-6 (Observer side): whenever this phone is online, make sure every Observer-made schedule of every User it
     * watches has a password for each of its next 14 occurrences. Runs at start, then every 6 hours.
     */
    private suspend fun watchPasswordPools() {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val links = com.imrat.focuslock.data.LinkRepository(db)
        val schedules = com.imrat.focuslock.data.ScheduleRepository(db)
        val topUp = (application as com.imrat.focuslock.FocusLockApp).container.poolTopUp
        while (true) {
            val me = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (me != null) {
                runCatching {
                    val users = links.observeUsersOf(me).first()
                        .filter { it.status != com.imrat.focuslock.data.LinkStatus.PENDING }.map { it.userUid }
                    for (u in users) for (s in schedules.get(u)) {
                        runCatching { topUp.topUp(u, me, s) }.onFailure { Log.w("CoreService", "pool top-up: ${it.message}") }
                    }
                }.onFailure { Log.w("CoreService", "pool job: ${it.message}") }
            }
            delay(6 * 60 * 60_000L)
        }
    }

    /** NOT-3: local notification when a link request or a removal request arrives. Each event once. */
    private suspend fun watchLinkAlerts() {
        val repo = com.imrat.focuslock.data.LinkRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance())
        val prefs = getSharedPreferences("link_alerts", Context.MODE_PRIVATE)
        while (true) {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                runCatching {
                    kotlinx.coroutines.flow.combine(repo.observe(uid), repo.observeUsersOf(uid)) { mine, people -> mine to people }
                        .collect { (mine, people) ->
                            val seen = prefs.getStringSet("seen", emptySet()).orEmpty()
                            val r = com.imrat.focuslock.data.LinkAlerts.plan(mine, people, seen)
                            r.fresh.forEach { notifyAlert(it) }
                            prefs.edit().putStringSet("seen", r.currentKeys).apply()
                        }
                }.onFailure { Log.w("CoreService", "link alerts: ${it.message}") }
            }
            delay(60_000)
        }
    }

    private fun notifyAlert(a: com.imrat.focuslock.data.LinkAlerts.Alert) {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("alerts_v2", getString(R.string.alert_channel), NotificationManager.IMPORTANCE_HIGH))
        val request = a.kind == com.imrat.focuslock.data.LinkAlerts.Kind.NEW_REQUEST
        val open = android.app.PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), android.app.PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, "alerts_v2")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(if (request) R.string.alert_request_title else R.string.alert_removal_title))
            .setContentText(getString(if (request) R.string.alert_request_body else R.string.alert_removal_body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(a.key.hashCode(), n) }
    }

    private suspend fun startAfterWarning(engine: SessionEngine, st: com.imrat.focuslock.data.RemoteSync.Start) {
        if (st.delayMs > 0) warn()
        delay(st.delayMs)
        val started = engine.startRemote(st.session)
        if (started != null) {
            if (started.mode == com.imrat.focuslock.data.LockMode.FULL) runCatching { startActivity(com.imrat.focuslock.lock.LockActivity.intent(this)) }
            else notifyAppsLock()
        }
        pendingStarts.remove(st.session.id)
    }

    private fun notifyAppsLock() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("alerts_v2", getString(R.string.alert_channel), NotificationManager.IMPORTANCE_HIGH))
        val n = NotificationCompat.Builder(this, "alerts_v2")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.toast_apps_lock_on))
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(7001, n) }
    }

    private fun warn() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("warn", getString(R.string.warn_channel), NotificationManager.IMPORTANCE_HIGH))
        val n = NotificationCompat.Builder(this, "warn")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.rwarn_title))
            .setContentText(getString(R.string.rwarn_body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(2, n) }
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.service_channel), NotificationManager.IMPORTANCE_MIN)
                .apply { setShowBadge(false) }
        )
        val open = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.service_notification))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "core"
        private const val NOTIFICATION_ID = 1

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, CoreService::class.java))
        }
    }
}
