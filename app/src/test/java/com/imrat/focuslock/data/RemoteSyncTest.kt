package com.imrat.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSyncTest {
    private fun s(id: String, by: String, status: SessionStatus, start: Long, end: Long) = LockSession(
        id, by, LockMode.FULL, SessionKind.INSTANT, emptyList(), start, end, status, null, null, null, true, false,
    )

    @Test fun observerSessionStartsAfterWarningDelay() {
        val p = RemoteSync.plan(listOf(s("a", "obs", SessionStatus.PENDING, 160_000, 900_000)), emptySet(), "me", 100_000)
        assertEquals(1, p.starts.size)
        assertEquals(60_000L, p.starts[0].delayMs)
    }

    @Test fun lateSessionStartsNow() {
        val p = RemoteSync.plan(listOf(s("a", "obs", SessionStatus.ACTIVE, 0, 900_000)), emptySet(), "me", 100_000)
        assertEquals(0L, p.starts[0].delayMs)
    }

    @Test fun ignoresOwnKnownExpiredAndEnded() {
        val list = listOf(
            s("own", "me", SessionStatus.ACTIVE, 0, 900_000),
            s("known", "obs", SessionStatus.ACTIVE, 0, 900_000),
            s("old", "obs", SessionStatus.PENDING, 0, 50_000),
            s("done", "obs", SessionStatus.ENDED, 0, 900_000),
        )
        val p = RemoteSync.plan(list, setOf("known"), "me", 100_000)
        assertTrue(p.starts.isEmpty())
    }

    @Test fun usedPasswordNeverRestarts() {
        val used = s("a", "obs", SessionStatus.ACTIVE, 0, 900_000).copy(passwordUsed = true)
        assertTrue(RemoteSync.plan(listOf(used), emptySet(), "me", 100_000).starts.isEmpty())
    }

    @Test fun changedServerHashIsAdopted() {
        val srv = s("a", "obs", SessionStatus.ACTIVE, 0, 900_000).copy(pwHash = "new", pwSalt = "salt2")
        val p = RemoteSync.plan(listOf(srv), setOf("a"), "me", 100_000, mapOf("a" to "old"))
        assertEquals(listOf(RemoteSync.Rehash("a", "new", "salt2")), p.rehash)
        // same hash, unknown local session, or ended session: nothing to do
        assertTrue(RemoteSync.plan(listOf(srv), setOf("a"), "me", 100_000, mapOf("a" to "new")).rehash.isEmpty())
        assertTrue(RemoteSync.plan(listOf(srv), emptySet(), "me", 100_000, emptyMap()).rehash.isEmpty())
        val ended = srv.copy(status = SessionStatus.ENDED)
        assertTrue(RemoteSync.plan(listOf(ended), setOf("a"), "me", 100_000, mapOf("a" to "old")).rehash.isEmpty())
    }

    @Test fun endedOnServerEndsLocal() {
        val p = RemoteSync.plan(listOf(s("a", "obs", SessionStatus.ENDED, 0, 900_000)), setOf("a", "b"), "me", 100_000)
        assertEquals(listOf("a"), p.endIds)
    }
}
