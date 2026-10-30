package com.imrat.focuslock.data

import com.imrat.focuslock.security.LocalSession

/** Pure SCH-4: running sessions of an Observer's schedule end when the Observer deletes or disables that schedule. */
object ScheduleCancel {
    /** Schedule sessions are named "<scheduleId>_<startMs>" (see Planned.key). Null for any other id. */
    fun scheduleIdOf(sessionId: String): String? {
        val cut = sessionId.lastIndexOf('_')
        if (cut <= 0) return null
        val tail = sessionId.substring(cut + 1)
        return if (tail.isNotEmpty() && tail.all { it.isDigit() }) sessionId.substring(0, cut) else null
    }

    /** Only sessions made by someone else are ended this way, so a User cannot cancel their own lock by deleting a schedule. */
    fun idsToEnd(local: List<LocalSession>, schedules: List<Schedule>, me: String): List<String> {
        val live = schedules.filter { it.enabled }.map { it.id }.toSet()
        return local.filter { it.createdBy != me }
            .filter { s -> scheduleIdOf(s.id)?.let { it !in live } ?: false }
            .map { it.id }
    }
}
