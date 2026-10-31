package com.imrat.focuslock.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.imrat.focuslock.R
import com.imrat.focuslock.data.Schedule
import com.imrat.focuslock.data.ScheduleCodec
import com.imrat.focuslock.lock.Planned
import com.imrat.focuslock.lock.SchedulePlan
import com.imrat.focuslock.lock.SessionEngine
import com.imrat.focuslock.time.TimeModel
import java.io.File

/**
 * Keeps a local copy of this phone's schedules and arms elapsed-time exact alarms:
 * a 10-minute notice (NOT-1) and the start (SCH-3). Works offline and survives reboot via [rearm].
 */
class ScheduleScheduler(ctx: Context) {
    private val app = ctx.applicationContext
    private val file = File(app.filesDir, "schedules.json")
    private val engine = SessionEngine(app)
    private val prefs = app.getSharedPreferences("sched_armed", Context.MODE_PRIVATE)

    @Synchronized fun load(): List<Schedule> = if (file.exists()) ScheduleCodec.fromJson(file.readText()) else emptyList()

    @Synchronized fun save(list: List<Schedule>) { file.writeText(ScheduleCodec.toJson(list)) }

    /** Starts anything running now, then arms the next notice + start for every schedule. */
    @Synchronized
    fun rearm(list: List<Schedule> = load()) {
        val am = app.getSystemService(AlarmManager::class.java) ?: return
        prefs.getStringSet("ids", emptySet()).orEmpty().forEach { cancel(am, it) }
        val srv = engine.clock.now()
        val el = SystemClock.elapsedRealtime()
        val armed = mutableSetOf<String>()
        for (s in list) {
            var p = SchedulePlan.next(s, srv) ?: continue
            if (p.isRunning(srv)) {
                engine.startScheduled(s, p)
                p = SchedulePlan.next(s, p.endMs) ?: continue
            }
            if (p.noticeMs() > srv) set(am, s.id, KIND_NOTICE, TimeModel.endElapsed(p.noticeMs(), srv, el))
            set(am, s.id, KIND_START, TimeModel.endElapsed(p.startMs, srv, el))
            armed += s.id
        }
        prefs.edit().putStringSet("ids", armed).apply()
    }

    fun onAlarm(kind: String) {
        if (kind == KIND_NOTICE) notifyNotice() else rearm()
    }

    private fun code(id: String, kind: String) = id.hashCode() * 2 + if (kind == KIND_NOTICE) 1 else 0

    private fun pi(id: String, kind: String): PendingIntent = PendingIntent.getBroadcast(
        app, code(id, kind), Intent(app, ScheduleAlarmReceiver::class.java).putExtra("kind", kind).putExtra("id", id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun cancel(am: AlarmManager, id: String) { am.cancel(pi(id, KIND_NOTICE)); am.cancel(pi(id, KIND_START)) }

    private fun set(am: AlarmManager, id: String, kind: String, atElapsed: Long) {
        if (am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsed, pi(id, kind))
        else am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsed, pi(id, kind))
    }

    private fun notifyNotice() {
        if (app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val nm = app.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(WARN_CHANNEL, app.getString(R.string.warn_channel), NotificationManager.IMPORTANCE_HIGH))
        nm.notify(
            WARN_ID,
            NotificationCompat.Builder(app, WARN_CHANNEL).setSmallIcon(R.drawable.ic_stat_logo)
                .setContentTitle(app.getString(R.string.warn_title)).setContentText(app.getString(R.string.warn_text))
                .setAutoCancel(true).build(),
        )
    }

    companion object {
        const val KIND_NOTICE = "notice"
        const val KIND_START = "start"
        private const val WARN_CHANNEL = "warn"
        private const val WARN_ID = 2
    }
}

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try { ScheduleScheduler(ctx).onAlarm(intent.getStringExtra("kind") ?: ScheduleScheduler.KIND_START) } finally { pending.finish() }
        }.start()
    }
}
