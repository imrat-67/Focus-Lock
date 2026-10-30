package com.imrat.focuslock.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Authenticated encryption for small local blobs (SEC-5). */
interface SecretBox {
    fun seal(plain: ByteArray): ByteArray
    /** Returns null when the blob is corrupted or was not produced by this box. */
    fun open(blob: ByteArray): ByteArray?
}

/** AES-256-GCM; output = 12-byte IV + ciphertext/tag. The key comes from [key] so tests can use a plain JVM key. */
class AesGcmBox(private val key: () -> SecretKey) : SecretBox {
    override fun seal(plain: ByteArray): ByteArray {
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.ENCRYPT_MODE, key())
        return c.iv + c.doFinal(plain)
    }

    override fun open(blob: ByteArray): ByteArray? {
        if (blob.size <= IV_LEN) return null
        return try {
            val c = Cipher.getInstance(TRANSFORM)
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, blob, 0, IV_LEN))
            c.doFinal(blob, IV_LEN, blob.size - IV_LEN)
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    private companion object {
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_LEN = 12
        const val TAG_BITS = 128
    }
}

/** Key lives in the Android Keystore (hardware-backed where available) and never leaves it. */
object KeystoreBox {
    private const val ALIAS = "focuslock_local_v1"

    fun create(): SecretBox = AesGcmBox(::key)

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }
}
