package com.imrat.focuslock.lock

import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.security.LocalSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPolicyTest {
    private fun s(mode: LockMode, end: Long, pk: List<String> = emptyList()) =
        LocalSession("s", "u", mode, pk, 0L, end, null, null)

    private val env = LockPolicy.Env("me.app", callActive = false, imePackages = setOf("ime.pkg"))
    private fun block(pkg: String, list: List<LocalSession>, now: Long = 100, e: LockPolicy.Env = env, cls: String? = null) =
        LockPolicy.shouldBlock(pkg, cls, list, now, e)

    @Test fun fullBlocksEverythingElse() = assertTrue(block("com.x", listOf(s(LockMode.FULL, 500))))
    @Test fun noSessionBlocksNothing() = assertFalse(block("com.x", emptyList()))
    @Test fun expiredSessionIgnored() = assertFalse(block("com.x", listOf(s(LockMode.FULL, 100))))
    @Test fun ownSystemUiAndImeAllowed() {
        val l = listOf(s(LockMode.FULL, 500))
        assertFalse(block("me.app", l)); assertFalse(block("com.android.systemui", l)); assertFalse(block("ime.pkg", l))
    }
    @Test fun dialerOnlyDuringCall() {
        val l = listOf(s(LockMode.FULL, 500))
        assertTrue(block("com.android.incallui", l))
        assertFalse(block("com.android.incallui", l, e = env.copy(callActive = true)))
    }
    @Test fun ringingAlarmAllowed() {
        val l = listOf(s(LockMode.FULL, 500))
        assertFalse(block("com.android.deskclock", l, cls = "com.android.deskclock.alarms.AlarmAlertFullScreen"))
        assertTrue(block("com.android.deskclock", l, cls = "com.android.deskclock.DeskClock"))
    }
    @Test fun appsLockBlocksOnlyListed() {
        val l = listOf(s(LockMode.APPS, 500, listOf("com.insta")))
        assertTrue(block("com.insta", l)); assertFalse(block("com.settings", l))
    }
    @Test fun displayedIsLatestEnding() =
        assertEquals(900L, LockPolicy.displayed(listOf(s(LockMode.FULL, 500), s(LockMode.FULL, 900)), 100)?.endElapsedMs)
}
