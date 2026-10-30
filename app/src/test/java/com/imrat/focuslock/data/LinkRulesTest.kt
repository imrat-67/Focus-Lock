package com.imrat.focuslock.data

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class LinkRulesTest {
    private val day = 24L * 60 * 60 * 1000
    private fun link(status: LinkStatus, askedAt: Long? = null) = Link("u", "o", status, askedAt)

    @Test fun removalNeedsActiveLinkAndNoLock() {
        assertTrue(LinkRules.canRequestRemoval(link(LinkStatus.ACTIVE), false))
        assertFalse(LinkRules.canRequestRemoval(link(LinkStatus.ACTIVE), true))
        assertFalse(LinkRules.canRequestRemoval(link(LinkStatus.PENDING), false))
        assertFalse(LinkRules.canRequestRemoval(link(LinkStatus.REMOVAL_REQUESTED, 0), false))
        assertFalse(LinkRules.canRequestRemoval(null, false))
    }

    @Test fun daysLeftRoundsUp() {
        val l = link(LinkStatus.REMOVAL_REQUESTED, 0)
        assertEquals(7, LinkRules.daysUntilAutoRemoval(l, 0))
        assertEquals(1, LinkRules.daysUntilAutoRemoval(l, 6 * day + 1))
        assertEquals(0, LinkRules.daysUntilAutoRemoval(l, 7 * day))
    }

    @Test fun autoCompleteWaitsForLockEnd() {
        val l = link(LinkStatus.REMOVAL_REQUESTED, 0)
        assertFalse(LinkRules.shouldAutoComplete(l, 7 * day - 1, false))
        assertFalse(LinkRules.shouldAutoComplete(l, 8 * day, true))
        assertTrue(LinkRules.shouldAutoComplete(l, 8 * day, false))
    }
}
