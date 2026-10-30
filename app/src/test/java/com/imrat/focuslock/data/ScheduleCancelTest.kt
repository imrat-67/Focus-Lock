package com.imrat.focuslock.data

import com.imrat.focuslock.security.LocalSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleCancelTest {
    private fun ls(id: String, by: String) = LocalSession(id, by, LockMode.FULL, emptyList(), 0, 0, null, null)
    private fun sch(id: String, enabled: Boolean) =
        Schedule(id, "obs", LockMode.FULL, emptyList(), emptySet(), "2026-01-01", 540, 600, "UTC", enabled)

    @Test fun parsesScheduleSessionIds() {
        assertEquals("abc", ScheduleCancel.scheduleIdOf("abc_1700000000000"))
        assertNull(ScheduleCancel.scheduleIdOf("randomFirestoreId"))
        assertNull(ScheduleCancel.scheduleIdOf("abc_xyz"))
    }

    @Test fun deletedOrDisabledObserverScheduleEndsSession() {
        val local = listOf(ls("s1_100", "obs"), ls("s2_100", "obs"), ls("s3_100", "obs"))
        val ids = ScheduleCancel.idsToEnd(local, listOf(sch("s1", true), sch("s2", false)), "me")
        assertEquals(listOf("s2_100", "s3_100"), ids)
    }

    @Test fun neverEndsOwnOrNonScheduleSessions() {
        val local = listOf(ls("s9_100", "me"), ls("instantId", "obs"))
        assertEquals(emptyList<String>(), ScheduleCancel.idsToEnd(local, emptyList(), "me"))
    }
}
