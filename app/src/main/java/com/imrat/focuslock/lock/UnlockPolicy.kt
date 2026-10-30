package com.imrat.focuslock.lock

import com.imrat.focuslock.security.LocalSession
import com.imrat.focuslock.security.PasswordCrypto

/** Pure unlock rules: password attempts with cooldown (UNL-3, UNL-4). */
object UnlockPolicy {
    const val MAX_ATTEMPTS = 5
    const val COOLDOWN_MS = 10 * 60_000L

    sealed interface Outcome {
        data object Success : Outcome
        data class Wrong(val attemptsLeft: Int) : Outcome
        data class Cooldown(val leftMs: Long) : Outcome
        data object NoPassword : Outcome
    }

    data class Result(val session: LocalSession, val outcome: Outcome)

    fun attempt(s: LocalSession, entered: String, nowElapsedMs: Long): Result {
        val hash = s.pwHash
        val salt = s.pwSalt
        if (hash == null || salt == null) return Result(s, Outcome.NoPassword)
        if (s.cooldownUntilElapsedMs > nowElapsedMs) return Result(s, Outcome.Cooldown(s.cooldownUntilElapsedMs - nowElapsedMs))
        if (PasswordCrypto.verify(entered, salt, hash)) return Result(s.copy(failedAttempts = 0), Outcome.Success)
        val fails = s.failedAttempts + 1
        return if (fails >= MAX_ATTEMPTS) {
            Result(s.copy(failedAttempts = 0, cooldownUntilElapsedMs = nowElapsedMs + COOLDOWN_MS), Outcome.Cooldown(COOLDOWN_MS))
        } else {
            Result(s.copy(failedAttempts = fails), Outcome.Wrong(MAX_ATTEMPTS - fails))
        }
    }

    /** After a reboot the monotonic clock restarted: keep punishments, never shorten them. */
    fun afterBoot(s: LocalSession, nowElapsedMs: Long): LocalSession = s.copy(
        cooldownUntilElapsedMs = if (s.cooldownUntilElapsedMs > 0) nowElapsedMs + COOLDOWN_MS else 0L,
    )
}
