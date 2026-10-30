package com.imrat.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LookupTest {
    @Test fun friendCodeHasValidShape() {
        repeat(200) {
            val c = Lookup.newFriendCode()
            assertEquals(Lookup.CODE_LENGTH, c.length)
            assertTrue(Lookup.isValidFriendCode(c))
        }
    }

    @Test fun friendCodeRejectsBadInput() {
        assertFalse(Lookup.isValidFriendCode("ABC"))
        assertFalse(Lookup.isValidFriendCode("ABCDEFG0")) // 0 not in alphabet
        assertFalse(Lookup.isValidFriendCode("abcdefgh")) // not normalized
        assertTrue(Lookup.isValidFriendCode(Lookup.normalizeCode(" abcdefgh ")))
    }

    @Test fun emailKeyIsCaseAndSpaceInsensitiveSha256() {
        assertEquals(Lookup.emailKey("Test@Example.com"), Lookup.emailKey("  test@example.com "))
        // sha256("a@b.co")
        assertEquals(64, Lookup.emailKey("a@b.co").length)
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            Lookup.emailKey("")
        )
    }

    @Test fun emailDetection() {
        assertTrue(Lookup.looksLikeEmail("a@b.co"))
        assertFalse(Lookup.looksLikeEmail("ABCDEFGH"))
        assertFalse(Lookup.looksLikeEmail("a@@b.co"))
        assertFalse(Lookup.looksLikeEmail("@b.co"))
    }
}

class LockSessionTest {
    private val base = mapOf<String, Any?>(
        "createdBy" to "obs", "mode" to "apps", "kind" to "instant",
        "packages" to listOf("a.b", "c.d"), "startAt" to 10L, "endAt" to 20, "status" to "active",
        "pwHash" to "h", "pwSalt" to "s", "remoteInstant" to true,
    )

    @Test fun parsesValidMap() {
        val s = LockSession.fromMap("id1", base)
        assertNotNull(s)
        assertEquals(LockMode.APPS, s!!.mode)
        assertEquals(20L, s.endAtMs)
        assertEquals(listOf("a.b", "c.d"), s.packages)
        assertTrue(s.remoteInstant)
        assertFalse(s.passwordUsed)
        assertNull(s.endedBy)
    }

    @Test fun rejectsUnknownEnumOrMissingFields() {
        assertNull(LockSession.fromMap("x", base + ("mode" to "weird")))
        assertNull(LockSession.fromMap("x", base - "endAt"))
        assertNull(LockSession.fromMap("x", base - "createdBy"))
    }

    @Test fun createMapRoundTrips() {
        val s = LockSession.fromMap("id1", base)!!
        val back = LockSession.fromMap("id1", s.toCreateMap())
        assertEquals(s, back)
    }
}
