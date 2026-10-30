package com.imrat.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkAlertsTest {
    private fun l(user: String, obs: String, st: LinkStatus) = Link(user, obs, st, null)

    @Test fun pendingRequestNotifiesOnce() {
        val first = LinkAlerts.plan(l("me", "o", LinkStatus.PENDING), emptyList(), emptySet())
        assertEquals(listOf(LinkAlerts.Kind.NEW_REQUEST), first.fresh.map { it.kind })
        val again = LinkAlerts.plan(l("me", "o", LinkStatus.PENDING), emptyList(), first.currentKeys)
        assertTrue(again.fresh.isEmpty())
    }

    @Test fun removalAskFromUserNotifiesObserver() {
        val r = LinkAlerts.plan(null, listOf(l("u1", "me", LinkStatus.REMOVAL_REQUESTED), l("u2", "me", LinkStatus.ACTIVE)), emptySet())
        assertEquals(listOf("rm:u1"), r.fresh.map { it.key })
    }

    @Test fun repeatedEventNotifiesAgainAfterItCleared() {
        val seen = LinkAlerts.plan(l("me", "o", LinkStatus.PENDING), emptyList(), emptySet()).currentKeys
        val cleared = LinkAlerts.plan(null, emptyList(), seen)
        assertTrue(cleared.currentKeys.isEmpty())
        assertEquals(1, LinkAlerts.plan(l("me", "o", LinkStatus.PENDING), emptyList(), cleared.currentKeys).fresh.size)
    }
}
