package com.imrat.focuslock.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** One concrete run of a schedule, in epoch millis (wall clock; converted to server time by the engine). */
data class Occurrence(val startMs: Long, val endMs: Long)

/**
 * Schedule definition (SCH-1). [oneTimeDate] != null => one-time on that date; otherwise repeats on [days].
 * Minutes are minutes-from-midnight, local time. If end <= start the occurrence ends the next day.
 */
data class ScheduleSpec(
    val days: Set<DayOfWeek>,
    val startMinute: Int,
    val endMinute: Int,
    val oneTimeDate: LocalDate? = null,
) {
    fun isValid(): Boolean =
        startMinute in 0..1439 && endMinute in 0..1439 &&
            durationMinutes() in 1..(TimeModel.MAX_SESSION_MS / 60_000) &&
            (oneTimeDate != null || days.isNotEmpty())

    fun durationMinutes(): Long {
        val d = (endMinute - startMinute).toLong()
        return if (d > 0) d else d + 24 * 60
    }
}

object ScheduleExpander {
    /**
     * Next occurrences whose END is after [fromMs] (so a running one is included), earliest first, up to [limit].
     * Looks at most 14 days ahead.
     */
    fun next(spec: ScheduleSpec, fromMs: Long, zone: ZoneId, limit: Int = 2): List<Occurrence> {
        if (!spec.isValid()) return emptyList()
        val out = ArrayList<Occurrence>()
        val today = java.time.Instant.ofEpochMilli(fromMs).atZone(zone).toLocalDate()
        for (offset in -1L..14L) {
            val date = today.plusDays(offset)
            val matches = if (spec.oneTimeDate != null) date == spec.oneTimeDate else date.dayOfWeek in spec.days
            if (!matches) continue
            val start = date.atTime(LocalTime.of(spec.startMinute / 60, spec.startMinute % 60)).atZone(zone).toInstant().toEpochMilli()
            val end = start + spec.durationMinutes() * 60_000
            if (end > fromMs) out += Occurrence(start, end)
            if (out.size >= limit) break
        }
        return out
    }
}
