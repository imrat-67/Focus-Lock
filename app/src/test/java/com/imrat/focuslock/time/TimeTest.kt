package com.imrat.focuslock.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class TimeModelTest {
    @Test fun serverNowAdvancesWithElapsedOnly() =
        assertEquals(1_000_000L + 500 + 3_000, TimeModel.serverNow(500, 10_000, 1_000_000, 13_000))

    @Test fun endElapsedFromServerEnd() = assertEquals(5_000L + 60_000, TimeModel.endElapsed(160_000, 100_000, 5_000))
    @Test fun clampCapsAt12h() {
        assertEquals(TimeModel.MAX_SESSION_MS, TimeModel.clampDuration(TimeModel.MAX_SESSION_MS * 3))
        assertEquals(0L, TimeModel.clampDuration(-5))
    }
    @Test fun syncedRebaseTrustsServer() = assertEquals(1_000L + 20_000, TimeModel.rebaseAfterBoot(120_000, 100_000, 90_000, 1_000, true))
    @Test fun offlineRebaseNeverShortensBelowLastRemaining() {
        // user moved clock forward: byClock = 0, last remaining 90s => keeps 90s
        assertEquals(1_000L + 90_000, TimeModel.rebaseAfterBoot(100_000, 100_000, 90_000, 1_000, false))
    }
    @Test fun rebaseNeverNegative() = assertEquals(1_000L, TimeModel.rebaseAfterBoot(50_000, 100_000, 0, 1_000, true))
    @Test fun remainingClamped() {
        assertEquals(0L, TimeModel.remainingMs(10, 20))
        assertEquals(10L, TimeModel.remainingMs(30, 20))
    }
}

class ScheduleExpanderTest {
    private val zone = ZoneId.of("Asia/Dhaka")
    private fun ms(y: Int, m: Int, d: Int, h: Int, min: Int) = ZonedDateTime.of(y, m, d, h, min, 0, 0, zone).toInstant().toEpochMilli()

    // 2026-11-02 is a Monday
    private val weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)

    @Test fun repeatingPicksNextMatchingDay() {
        val spec = ScheduleSpec(weekdays, 9 * 60, 11 * 60)
        val r = ScheduleExpander.next(spec, ms(2026, 11, 3, 12, 0), zone, 2) // Tuesday
        assertEquals(ms(2026, 11, 4, 9, 0), r[0].startMs)
        assertEquals(ms(2026, 11, 4, 11, 0), r[0].endMs)
        assertEquals(ms(2026, 11, 9, 9, 0), r[1].startMs)
    }
    @Test fun runningOccurrenceIncluded() {
        val spec = ScheduleSpec(weekdays, 9 * 60, 11 * 60)
        val r = ScheduleExpander.next(spec, ms(2026, 11, 2, 10, 0), zone, 1)
        assertEquals(ms(2026, 11, 2, 9, 0), r[0].startMs)
    }
    @Test fun overnightEndsNextDay() {
        val spec = ScheduleSpec(setOf(DayOfWeek.MONDAY), 22 * 60, 6 * 60)
        val r = ScheduleExpander.next(spec, ms(2026, 11, 3, 1, 0), zone, 1) // Tue 01:00, still in Mon night run
        assertEquals(ms(2026, 11, 2, 22, 0), r[0].startMs)
        assertEquals(ms(2026, 11, 3, 6, 0), r[0].endMs)
    }
    @Test fun oneTimeOnlyOnce() {
        val spec = ScheduleSpec(emptySet(), 8 * 60, 9 * 60, LocalDate.of(2026, 11, 5))
        assertEquals(1, ScheduleExpander.next(spec, ms(2026, 11, 1, 0, 0), zone, 5).size)
        assertTrue(ScheduleExpander.next(spec, ms(2026, 11, 6, 0, 0), zone, 5).isEmpty())
    }
    @Test fun invalidSpecs() {
        assertFalse(ScheduleSpec(weekdays, 0, 0).isValid()) // 24h > 12h cap
        assertFalse(ScheduleSpec(weekdays, 8 * 60, 21 * 60).isValid()) // 13h
        assertFalse(ScheduleSpec(emptySet(), 60, 120).isValid())
        assertTrue(ScheduleSpec(weekdays, 8 * 60, 20 * 60).isValid()) // exactly 12h
        assertTrue(ScheduleExpander.next(ScheduleSpec(weekdays, 0, 0), 0, zone).isEmpty())
    }
}
