package com.imrat.focuslock.lock

import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.time.TimeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPlanTest {
    private fun mk(dur: Long = 60_000, srv: Long = 1_000_000, el: Long = 5_000) =
        SessionPlan.build("s", "u", LockMode.FULL, emptyList(), dur, srv, el)

    @Test fun buildSetsBothEnds() {
        val s = mk()
        assertEquals(1_060_000L, s.endAtServerMs); assertEquals(65_000L, s.endElapsedMs); assertEquals(60_000L, s.lastRemainingMs)
    }
    @Test fun buildCapsAt12h() = assertEquals(TimeModel.MAX_SESSION_MS, mk(dur = TimeModel.MAX_SESSION_MS * 2).lastRemainingMs)
    @Test fun rebaseSyncedUsesServerTime() {
        val r = SessionPlan.rebaseAfterBoot(listOf(mk()), serverNowMs = 1_030_000, elapsedNowMs = 2_000, synced = true)
        assertEquals(32_000L, r.single().endElapsedMs); assertEquals(30_000L, r.single().lastRemainingMs)
    }
    @Test fun rebaseOfflineClockForwardDoesNotShorten() {
        val s = mk().copy(lastRemainingMs = 40_000)
        val r = SessionPlan.rebaseAfterBoot(listOf(s), serverNowMs = 9_999_999, elapsedNowMs = 2_000, synced = false)
        assertEquals(42_000L, r.single().endElapsedMs)
    }
    @Test fun rebaseDropsExpired() =
        assertTrue(SessionPlan.rebaseAfterBoot(listOf(mk()), 2_000_000, 2_000, true).isEmpty())
    @Test fun pruneAndExpired() {
        val a = mk(); val b = a.copy(id = "b", endElapsedMs = 10)
        assertEquals(listOf("s"), SessionPlan.prune(listOf(a, b), 100).map { it.id })
        assertEquals(listOf("b"), SessionPlan.expiredIds(listOf(a, b), 100))
    }
    @Test fun refreshRemaining() = assertEquals(25_000L, SessionPlan.refreshRemaining(listOf(mk()), 40_000).single().lastRemainingMs)
    @Test fun resyncKeepsCooldownAndCorrectsEnd() {
        val s = mk().copy(cooldownUntilElapsedMs = 77)
        val r = SessionPlan.resync(listOf(s), serverNowMs = 1_040_000, elapsedNowMs = 9_000).single()
        assertEquals(29_000L, r.endElapsedMs); assertEquals(77L, r.cooldownUntilElapsedMs); assertEquals(20_000L, r.lastRemainingMs)
    }
}
