package com.imrat.focuslock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.imrat.focuslock.MainActivity
import com.imrat.focuslock.R

/**
 * Always-on foreground service (NOT-4: one silent, minimal notification).
 * Skeleton in T05; Firestore listeners, alarms and session restore are added in T08/T10.
 */
class CoreService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        return START_STICKY
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
