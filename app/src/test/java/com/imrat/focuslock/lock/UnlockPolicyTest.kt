package com.imrat.focuslock.lock

import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.security.LocalSession
import com.imrat.focuslock.security.PasswordCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockPolicyTest {
    private val fresh = PasswordCrypto.fresh()
    private fun locked(by: String = "obs", pw: Boolean = true) = LocalSession(
        "s", by, LockMode.FULL, emptyList(), 0, 1_000_000,
        if (pw) fresh.hash else null, if (pw) fresh.salt else null,
    )

    @Test fun passwordShapeAndHashing() {
        assertEquals(6, fresh.plain.length)
        assertTrue(fresh.plain.all(Char::isDigit))
        assertTrue(PasswordCrypto.verify(fresh.plain, fresh.salt, fresh.hash))
        assertFalse(PasswordCrypto.verify("000000x", fresh.salt, fresh.hash))
        assertNotEquals(PasswordCrypto.newSalt(), PasswordCrypto.newSalt())
        assertFalse(PasswordCrypto.verify("123456", "not base64!!", fresh.hash))
    }

    @Test fun correctPasswordSucceeds() {
        assertEquals(UnlockPolicy.Outcome.Success, UnlockPolicy.attempt(locked(), fresh.plain, 10).outcome)
    }

    @Test fun fiveWrongAttemptsStartTenMinuteCooldown() {
        val wrong = if (fresh.plain == "111111") "222222" else "111111"
        var s = locked()
        repeat(4) {
            val r = UnlockPolicy.attempt(s, wrong, 100); s = r.session
            assertEquals(UnlockPolicy.Outcome.Wrong(4 - it), r.outcome)
        }
        val r5 = UnlockPolicy.attempt(s, wrong, 100); s = r5.session
        assertEquals(UnlockPolicy.Outcome.Cooldown(UnlockPolicy.COOLDOWN_MS), r5.outcome)
        // Even the right password is refused during the cooldown, and works after it.
        assertTrue(UnlockPolicy.attempt(s, fresh.plain, 200).outcome is UnlockPolicy.Outcome.Cooldown)
        assertEquals(UnlockPolicy.Outcome.Success, UnlockPolicy.attempt(s, fresh.plain, 100 + UnlockPolicy.COOLDOWN_MS).outcome)
    }

    @Test fun noPasswordMeansNoUnlock() {
        assertEquals(UnlockPolicy.Outcome.NoPassword, UnlockPolicy.attempt(locked(pw = false), "123456", 0).outcome)
    }

    @Test fun emergencyExitOnlyForOwnPasswordlessLock() {
        assertTrue(UnlockPolicy.canEmergencyExit(locked("me", pw = false), "me"))
        assertFalse(UnlockPolicy.canEmergencyExit(locked("obs", pw = false), "me"))
        assertFalse(UnlockPolicy.canEmergencyExit(locked("me", pw = true), "me"))
        assertFalse(UnlockPolicy.canEmergencyExit(locked("me", pw = false), null))
    }

    @Test fun exitCountdownIsFiveMinutesAndNotRestartedByRepeatedTaps() {
        val s1 = UnlockPolicy.startExit(locked("me", false), 1_000)
        assertEquals(1_000 + UnlockPolicy.EXIT_COUNTDOWN_MS, s1.exitAtElapsedMs)
        assertEquals(s1, UnlockPolicy.startExit(s1, 50_000))
        assertFalse(UnlockPolicy.exitDue(s1, 2_000))
        assertTrue(UnlockPolicy.exitDue(s1, s1.exitAtElapsedMs))
    }

    @Test fun rebootNeverShortensCooldownOrExit() {
        val s = locked().copy(cooldownUntilElapsedMs = 5, exitAtElapsedMs = 6)
        val b = UnlockPolicy.afterBoot(s, 1_000)
        assertEquals(1_000 + UnlockPolicy.COOLDOWN_MS, b.cooldownUntilElapsedMs)
        assertEquals(1_000 + UnlockPolicy.EXIT_COUNTDOWN_MS, b.exitAtElapsedMs)
        assertEquals(0L, UnlockPolicy.afterBoot(locked(), 1_000).cooldownUntilElapsedMs)
    }
}
