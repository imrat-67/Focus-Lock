package com.imrat.focuslock.time

/**
 * Tamper-resistant time (LOCK-8). Pure: no Android calls.
 * serverNow = anchorWall + offset + (elapsedNow - anchorElapsed).
 */
object TimeModel {
    const val MAX_SESSION_MS = 24L * 60 * 60 * 1000 // LOCK-5 (24 h, user decision T14)

    fun serverNow(offsetMs: Long, anchorElapsedMs: Long, anchorWallMs: Long, elapsedNowMs: Long): Long =
        anchorWallMs + offsetMs + (elapsedNowMs - anchorElapsedMs)

    /** Monotonic end for a session whose server-time end is known. */
    fun endElapsed(endAtServerMs: Long, serverNowMs: Long, elapsedNowMs: Long): Long =
        elapsedNowMs + (endAtServerMs - serverNowMs)

    fun clampDuration(ms: Long): Long = ms.coerceIn(0L, MAX_SESSION_MS)

    /**
     * After reboot elapsedRealtime restarts. Rebase a stored end onto the new monotonic clock.
     * Synced: trust server time. Offline: never end earlier than the last persisted remaining time.
     */
    fun rebaseAfterBoot(endAtServerMs: Long, serverNowMs: Long, lastRemainingMs: Long, elapsedNowMs: Long, synced: Boolean): Long {
        val byClock = endAtServerMs - serverNowMs
        val remaining = if (synced) byClock else maxOf(byClock, lastRemainingMs)
        return elapsedNowMs + remaining.coerceAtLeast(0L)
    }

    fun remainingMs(endElapsedMs: Long, elapsedNowMs: Long): Long = (endElapsedMs - elapsedNowMs).coerceAtLeast(0L)
}
