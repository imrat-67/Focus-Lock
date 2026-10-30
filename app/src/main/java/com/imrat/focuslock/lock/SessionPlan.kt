package com.imrat.focuslock.lock

import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.security.LocalSession
import com.imrat.focuslock.time.TimeModel

/** Pure session bookkeeping (no Android): build, rebase after boot, prune, refresh remaining. */
object SessionPlan {
    fun build(
        id: String, createdBy: String, mode: LockMode, packages: List<String>,
        durationMs: Long, serverNowMs: Long, elapsedNowMs: Long,
        pwHash: String? = null, pwSalt: String? = null,
    ): LocalSession {
        val d = TimeModel.clampDuration(durationMs)
        return LocalSession(
            id = id, createdBy = createdBy, mode = mode, packages = packages,
            endAtServerMs = serverNowMs + d, endElapsedMs = elapsedNowMs + d,
            pwHash = pwHash, pwSalt = pwSalt, lastRemainingMs = d,
        )
    }

    /** After reboot: rebase every session onto the new monotonic clock (LOCK-7, LOCK-8). Expired ones are dropped. */
    fun rebaseAfterBoot(list: List<LocalSession>, serverNowMs: Long, elapsedNowMs: Long, synced: Boolean): List<LocalSession> =
        list.map { s ->
            val end = TimeModel.rebaseAfterBoot(s.endAtServerMs, serverNowMs, s.lastRemainingMs, elapsedNowMs, synced)
            UnlockPolicy.afterBoot(s.copy(endElapsedMs = end, lastRemainingMs = TimeModel.remainingMs(end, elapsedNowMs)), elapsedNowMs)
        }.filter { it.endElapsedMs > elapsedNowMs }

    /** After a successful server sync: re-derive ends from server time. Keeps cooldowns. Drops expired. */
    fun resync(list: List<LocalSession>, serverNowMs: Long, elapsedNowMs: Long): List<LocalSession> =
        list.map { s ->
            val end = TimeModel.endElapsed(s.endAtServerMs, serverNowMs, elapsedNowMs)
            s.copy(endElapsedMs = end, lastRemainingMs = TimeModel.remainingMs(end, elapsedNowMs))
        }.filter { it.endElapsedMs > elapsedNowMs }

    fun prune(list: List<LocalSession>, elapsedNowMs: Long): List<LocalSession> = list.filter { it.endElapsedMs > elapsedNowMs }

    fun refreshRemaining(list: List<LocalSession>, elapsedNowMs: Long): List<LocalSession> =
        list.map { it.copy(lastRemainingMs = TimeModel.remainingMs(it.endElapsedMs, elapsedNowMs)) }

    fun expiredIds(list: List<LocalSession>, elapsedNowMs: Long): List<String> =
        list.filter { it.endElapsedMs <= elapsedNowMs }.map { it.id }
}
