package com.imrat.focuslock.lock

import com.imrat.focuslock.data.Schedule
import com.imrat.focuslock.time.ScheduleExpander
import java.time.ZoneId

/** One concrete run of a schedule. [key] doubles as the local/Firestore session id (idempotent starts). */
data class Planned(val scheduleId: String, val startMs: Long, val endMs: Long) {
    val key: String get() = "${scheduleId}_$startMs"
    fun noticeMs(): Long = startMs - NOTICE_MS
    fun isRunning(serverNowMs: Long): Boolean = startMs <= serverNowMs && serverNowMs < endMs

    companion object { const val NOTICE_MS = 10L * 60 * 1000 } // NOT-1
}

object SchedulePlan {
    fun zone(tz: String): ZoneId = runCatching { ZoneId.of(tz) }.getOrDefault(ZoneId.systemDefault())

    /** Next (or currently running) occurrence of an enabled, usable schedule; null otherwise. */
    fun next(s: Schedule, serverNowMs: Long): Planned? {
        if (!s.enabled || !s.isUsable()) return null
        val o = ScheduleExpander.next(s.toSpec(), serverNowMs, zone(s.tz), 1).firstOrNull() ?: return null
        return Planned(s.id, o.startMs, o.endMs)
    }

    fun nextAll(list: List<Schedule>, serverNowMs: Long): List<Pair<Schedule, Planned>> =
        list.mapNotNull { s -> next(s, serverNowMs)?.let { s to it } }
}
