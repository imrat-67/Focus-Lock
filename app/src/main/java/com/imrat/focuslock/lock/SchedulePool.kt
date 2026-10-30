package com.imrat.focuslock.lock

import com.imrat.focuslock.data.Schedule
import com.imrat.focuslock.time.ScheduleExpander

/**
 * UNL-6: an Observer-made schedule carries a pool of password hashes, one per upcoming occurrence.
 * Pool key = occurrence start (epoch ms as text); value = "hash:salt" (Base64 has no ':'). Pure logic, no Android.
 */
object SchedulePool {
    const val SIZE = 14
    private const val KEEP_PAST_MS = 25L * 60 * 60 * 1000 // longest lock is 24 h; the Observer may still want to see it

    fun encode(hash: String, salt: String) = "$hash:$salt"

    /** Returns (hash, salt) or null when malformed. */
    fun parse(entry: String?): Pair<String, String>? {
        val i = entry?.indexOf(':') ?: return null
        if (i <= 0 || i == entry.length - 1) return null
        return entry.substring(0, i) to entry.substring(i + 1)
    }

    /** Key shared by the pool, the Observer's vault and the session id (same as [Planned.key]). */
    fun vaultKey(scheduleId: String, startMs: Long) = "${scheduleId}_$startMs"

    /** Starts of the next [SIZE] occurrences (the expander looks at most 14 days ahead). Empty if disabled/unusable. */
    fun upcoming(s: Schedule, nowMs: Long): List<Long> {
        if (!s.enabled || !s.isUsable()) return emptyList()
        return ScheduleExpander.next(s.toSpec(), nowMs, SchedulePlan.zone(s.tz), SIZE).map { it.startMs }
    }

    /** Upcoming starts that have no pool entry yet. */
    fun missing(pool: Map<String, String>, upcoming: List<Long>): List<Long> =
        upcoming.filter { pool[it.toString()] == null }

    /** Pool without entries that are long past. */
    fun pruned(pool: Map<String, String>, nowMs: Long): Map<String, String> =
        pool.filterKeys { (it.toLongOrNull() ?: 0L) >= nowMs - KEEP_PAST_MS }

    /** Entry for the occurrence starting at [startMs], or null (remote unlock only). */
    fun entryFor(s: Schedule, startMs: Long): Pair<String, String>? = parse(s.pwPool[startMs.toString()])
}
