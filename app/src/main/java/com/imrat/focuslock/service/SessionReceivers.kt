package com.imrat.focuslock.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.imrat.focuslock.lock.SessionEngine

/** Fired by the exact end alarm: clean up expired sessions. */
class SessionEndReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        SessionEngine(ctx).heartbeat()
    }
}

/** Reboot: rebase sessions onto the new monotonic clock, restart the service, then re-sync server time (LOCK-7, LOCK-8). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val engine = SessionEngine(ctx)
        engine.onBoot()
        ScheduleScheduler(ctx).rearm()
        CoreService.start(ctx)
        val pending = goAsync()
        Thread {
            try { if (engine.syncClock()) ScheduleScheduler(ctx).rearm() } finally { pending.finish() }
        }.start()
    }
}
