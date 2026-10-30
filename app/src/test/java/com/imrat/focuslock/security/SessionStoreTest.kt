package com.imrat.focuslock.security

import com.imrat.focuslock.data.LockMode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import javax.crypto.KeyGenerator

class SessionStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val box = AesGcmBox { key }

    private fun session(id: String, over: (LocalSession) -> LocalSession = { it }) = over(
        LocalSession(id, "obs", LockMode.APPS, listOf("a.b", "c.d"), 2_000L, 1_000L, "hash", "salt")
    )

    @Test fun boxRoundTripsAndUsesFreshIv() {
        val a = box.seal("secret".toByteArray())
        val b = box.seal("secret".toByteArray())
        assertNotEquals(a.toList(), b.toList())
        assertArrayEquals("secret".toByteArray(), box.open(a))
    }

    @Test fun boxDetectsTamperingAndWrongKey() {
        val blob = box.seal("secret".toByteArray())
        blob[blob.size - 1] = (blob[blob.size - 1].toInt() xor 1).toByte()
        assertNull(box.open(blob))
        val other = AesGcmBox { KeyGenerator.getInstance("AES").apply { init(256) }.generateKey() }
        assertNull(other.open(box.seal("x".toByteArray())))
        assertNull(box.open(ByteArray(5)))
    }

    @Test fun storeRoundTrip() {
        val store = SessionStore(tmp.newFile("s.bin"), box)
        val list = listOf(session("1"), session("2") { it.copy(mode = LockMode.FULL, packages = emptyList(), pwHash = null, pwSalt = null) })
        store.save(list)
        assertEquals(list, store.load())
    }

    @Test fun updateAndRemove() {
        val store = SessionStore(tmp.newFile("s.bin"), box)
        store.save(listOf(session("1"), session("2")))
        store.update("1") { it.copy(failedAttempts = 3, cooldownUntilElapsedMs = 9_000L) }
        assertEquals(3, store.load().first { it.id == "1" }.failedAttempts)
        store.remove("2")
        assertEquals(listOf("1"), store.load().map { it.id })
    }

    @Test fun missingOrCorruptFileMeansNoSessions() {
        val f = tmp.newFile("s.bin")
        f.delete()
        assertEquals(emptyList<LocalSession>(), SessionStore(f, box).load())
        f.writeBytes(ByteArray(40) { it.toByte() })
        assertEquals(emptyList<LocalSession>(), SessionStore(f, box).load())
    }
}
