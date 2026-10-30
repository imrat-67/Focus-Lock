package com.imrat.focuslock.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Emergency password helpers (UNL-2). Pure JVM, so they are unit-testable. */
object PasswordCrypto {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    const val DIGITS = 6

    /** New random 6-digit password, leading zeros kept. */
    fun newPassword(random: SecureRandom = SecureRandom()): String =
        (1..DIGITS).joinToString("") { random.nextInt(10).toString() }

    fun newSalt(random: SecureRandom = SecureRandom()): String =
        Base64.getEncoder().encodeToString(ByteArray(16).also(random::nextBytes))

    fun hash(password: String, saltB64: String): String {
        val spec = PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(saltB64), ITERATIONS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(bytes)
    }

    /** Constant-time comparison of the hash of [entered] with [expectedHash]. */
    fun verify(entered: String, saltB64: String, expectedHash: String): Boolean = try {
        MessageDigest.isEqual(hash(entered, saltB64).toByteArray(), expectedHash.toByteArray())
    } catch (e: IllegalArgumentException) {
        false // malformed salt/hash
    }

    /** A fresh password with its salt and hash. [plain] must only ever stay on the Observer's phone. */
    data class Fresh(val plain: String, val salt: String, val hash: String)

    fun fresh(random: SecureRandom = SecureRandom()): Fresh {
        val pw = newPassword(random)
        val salt = newSalt(random)
        return Fresh(pw, salt, hash(pw, salt))
    }
}
