package com.imrat.focuslock.lock

import com.imrat.focuslock.data.LockMode
import com.imrat.focuslock.data.Schedule
import com.imrat.focuslock.data.ScheduleCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SchedulePlanTest {
    private val zone = ZoneId.of("Asia/Dhaka")
    private fun ms(d: Int, h: Int, m: Int) = ZonedDateTime.of(2026, 11, d, h, m, 0, 0, zone).toInstant().toEpochMilli()
    private fun sch(days: Set<Int> = setOf(1, 3), st: Int = 540, en: Int = 660, on: Boolean = true, mode: LockMode = LockMode.FULL, pk: List<String> = emptyList(), date: String? = null) =
        Schedule("sc", "u", mode, pk, if (date == null) days else emptySet(), date, st, en, "Asia/Dhaka", on)

    @Test fun nextMondayOccurrence() {
        val p = SchedulePlan.next(sch(), ms(1, 12, 0))!! // Sunday 2026-11-01
        assertEquals(ms(2, 9, 0), p.startMs); assertEquals("sc_${ms(2, 9, 0)}", p.key)
        assertEquals(ms(2, 8, 50), p.noticeMs())
    }
    @Test fun runningDetected() {
        val p = SchedulePlan.next(sch(), ms(2, 10, 0))!!
        assertTrue(p.isRunning(ms(2, 10, 0))); assertFalse(p.isRunning(ms(2, 11, 0)))
    }
    @Test fun disabledOrUnusableGivesNull() {
        assertNull(SchedulePlan.next(sch(on = false), 0))
        assertNull(SchedulePlan.next(sch(mode = LockMode.APPS), 0)) // no apps
        assertNotNull(SchedulePlan.next(sch(mode = LockMode.APPS, pk = listOf("a.b")), ms(1, 0, 0)))
    }
    @Test fun oneTime() {
        assertNotNull(SchedulePlan.next(sch(date = "2026-11-05"), ms(1, 0, 0)))
        assertNull(SchedulePlan.next(sch(date = "2026-11-05"), ms(6, 0, 0)))
    }
    @Test fun badTimezoneFallsBack() = assertNotNull(SchedulePlan.zone("Not/AZone"))
    @Test fun codecRoundTrip() {
        val s = sch(mode = LockMode.APPS, pk = listOf("a.b", "c.d"))
        assertEquals(s, ScheduleCodec.fromMap("sc", ScheduleCodec.toMap(s)))
        assertEquals(listOf(s), ScheduleCodec.fromJson(ScheduleCodec.toJson(listOf(s))))
        val one = sch(date = "2026-11-05")
        assertEquals(listOf(one), ScheduleCodec.fromJson(ScheduleCodec.toJson(listOf(one))))
    }
}
