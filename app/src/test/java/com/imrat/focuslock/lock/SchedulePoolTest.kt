package com.imrat.focuslock.lock

import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.data.Schedule
import com.imrat.focuslock.data.ScheduleCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SchedulePoolTest {
    private val now = Instant.parse("2026-10-30T00:00:00Z").toEpochMilli()
    private fun daily(pool: Map<String, String> = emptyMap(), enabled: Boolean = true) = Schedule(
        "s1", "obs", LockMode.FULL, emptyList(), (1..7).toSet(), null, 9 * 60, 10 * 60, "UTC", enabled, pool,
    )

    @Test fun entryEncodingRoundTripsAndRejectsGarbage() {
        val e = SchedulePool.encode("a+b/c=", "s/a+l=")
        assertEquals("a+b/c=" to "s/a+l=", SchedulePool.parse(e))
        assertNull(SchedulePool.parse(null))
        assertNull(SchedulePool.parse("nocolon"))
        assertNull(SchedulePool.parse(":x"))
        assertNull(SchedulePool.parse("x:"))
    }

    @Test fun dailyScheduleHasUpToFourteenUpcoming() {
        val up = SchedulePool.upcoming(daily(), now)
        assertTrue(up.size in 13..14)
        assertEquals(up.sorted(), up)
        assertTrue(SchedulePool.upcoming(daily(enabled = false), now).isEmpty())
    }

    @Test fun missingOnlyListsStartsWithoutEntry() {
        val up = SchedulePool.upcoming(daily(), now)
        val pool = mapOf(up[0].toString() to "h:s", up[2].toString() to "h:s")
        val miss = SchedulePool.missing(pool, up)
        assertEquals(up.size - 2, miss.size)
        assertTrue(up[0] !in miss && up[2] !in miss && up[1] in miss)
    }

    @Test fun prunedDropsOldKeepsRecentAndFuture() {
        val old = now - 26L * 3600_000
        val recent = now - 2L * 3600_000
        val fut = now + 3600_000
        val p = SchedulePool.pruned(mapOf(old.toString() to "x:y", recent.toString() to "x:y", fut.toString() to "x:y"), now)
        assertEquals(setOf(recent.toString(), fut.toString()), p.keys)
    }

    @Test fun entryForUsesStartKey() {
        val s = daily(mapOf("1000" to "h:s"))
        assertEquals("h" to "s", SchedulePool.entryFor(s, 1000))
        assertNull(SchedulePool.entryFor(s, 2000))
    }

    @Test fun poolSurvivesJsonCacheRoundTrip() {
        val s = daily(mapOf("1000" to "h:s", "2000" to "h2:s2"))
        val back = ScheduleCodec.fromJson(ScheduleCodec.toJson(listOf(s))).single()
        assertEquals(s.pwPool, back.pwPool)
        assertEquals(s, back)
        // old caches without a pool still load
        val legacy = ScheduleCodec.toJson(listOf(daily())).replace(",\"pool\":{}", "")
        assertTrue(ScheduleCodec.fromJson(legacy).single().pwPool.isEmpty())
    }

    @Test fun firestoreMapCarriesPoolOnlyWhenPresent() {
        assertTrue("pwPool" !in ScheduleCodec.toMap(daily()))
        val m = ScheduleCodec.toMap(daily(mapOf("1" to "a:b")))
        assertEquals(mapOf("1" to "a:b"), m["pwPool"])
        assertEquals(mapOf("1" to "a:b"), ScheduleCodec.fromMap("s1", m)?.pwPool)
    }
}
