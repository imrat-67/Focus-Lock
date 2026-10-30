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
        if (heartbeat == null) {
            heartbeat = scope.launch {
                var tick = 0
                while (true) {
                    runCatching {
                        engine.heartbeat()
                        if (tick % 20 == 0) engine.syncClock() // ~every 10 min, and at start
                    }
                    tick++
                    delay(30_000)
                }
            }
        }
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
