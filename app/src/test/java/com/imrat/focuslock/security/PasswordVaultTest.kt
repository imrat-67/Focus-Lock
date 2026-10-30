package com.imrat.focuslock.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.KeyGenerator

class PasswordVaultTest {
    @get:Rule val tmp = TemporaryFolder()
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private fun vault(f: File) = PasswordVault(f, AesGcmBox { key })

    @Test fun putGetRemoveAndPersist() {
        val f = File(tmp.root, "v.bin")
        val v = vault(f)
        assertNull(v.get("a"))
        v.put("a", "012345")
        v.put("b", "999999")
        assertEquals("012345", vault(f).get("a")) // survives a new instance
        v.put("a", "111111") // regenerate overwrites
        assertEquals("111111", v.get("a"))
        v.remove("a")
        assertNull(v.get("a"))
        assertEquals("999999", v.get("b"))
    }

    @Test fun fileIsEncryptedAndCorruptionIsSafe() {
        val f = File(tmp.root, "v.bin")
        val v = vault(f)
        v.put("a", "424242")
        assertFalse(String(f.readBytes(), Charsets.ISO_8859_1).contains("424242"))
        f.writeBytes(ByteArray(40) { 1 })
        assertNull(vault(f).get("a"))
    }

    @Test fun retainOnlyDropsOthers() {
        val v = vault(File(tmp.root, "v.bin"))
        v.put("a", "111111"); v.put("b", "222222")
        v.retainOnly(setOf("b"))
        assertNull(v.get("a"))
        assertEquals("222222", v.get("b"))
    }
}
