package com.imrat.focuslock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppsTest {
    @Test fun normalizeDedupesSortsAndExcludesSelf() {
        val out = AppListCodec.normalize(
            listOf(AppEntry("zeta", "z.z"), AppEntry("Alpha", "a.a"), AppEntry("dup", "a.a"), AppEntry("Me", "me.app"), AppEntry("x", "")),
            excludePkg = "me.app",
        )
        assertEquals(listOf("a.a", "z.z"), out.map { it.pkg })
    }

    @Test fun itemsRoundTrip() {
        val apps = listOf(AppEntry("Alpha", "a.a"), AppEntry("Beta", "b.b"))
        assertEquals(apps, AppListCodec.fromItems(AppListCodec.toItems(apps)))
    }

    @Test fun fromItemsToleratesGarbage() {
        val out = AppListCodec.fromItems(listOf(mapOf("p" to "ok"), "bad", mapOf("n" to "noPkg"), null))
        assertEquals(listOf(AppEntry("ok", "ok")), out)
        assertTrue(AppListCodec.fromItems(null).isEmpty())
    }

    @Test fun fingerprintChangesWithList() =
        assertFalse(AppListCodec.fingerprint(listOf(AppEntry("a", "a"))) == AppListCodec.fingerprint(listOf(AppEntry("a", "a"), AppEntry("b", "b"))))

    @Test fun formatMapRoundTripAndLimits() {
        val m = FormatCodec.toMap("  Study  ", listOf("a", "a", "b"), "u1")
        assertEquals("Study", m["name"]); assertEquals(listOf("a", "b"), m["packages"])
        val f = FormatCodec.fromMap("id", m)
        assertEquals(Format("id", "Study", listOf("a", "b"), "u1"), f)
        assertNull(FormatCodec.fromMap("id", mapOf("name" to "x")))
    }

    @Test fun validation() {
        assertTrue(FormatCodec.isValid("Study", listOf("a")))
        assertFalse(FormatCodec.isValid("  ", listOf("a")))
        assertFalse(FormatCodec.isValid("x", emptyList()))
        assertFalse(FormatCodec.isValid("x".repeat(61), listOf("a")))
    }
}
