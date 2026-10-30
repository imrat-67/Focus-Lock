package com.imrat.focuslock.data

import java.security.MessageDigest
import java.security.SecureRandom

/** Pure helpers for lookup keys. No Android/Firebase dependency so they are unit-testable. */
object Lookup {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val CODE_LENGTH = 8

    fun newFriendCode(random: SecureRandom = SecureRandom()): String =
        buildString(CODE_LENGTH) {
            repeat(CODE_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }

    fun normalizeCode(input: String): String = input.trim().uppercase()

    fun isValidFriendCode(code: String): Boolean =
        code.length == CODE_LENGTH && code.all { it in ALPHABET }

    fun looksLikeEmail(input: String): Boolean {
        val s = input.trim()
        val at = s.indexOf('@')
        return at > 0 && at == s.lastIndexOf('@') && s.indexOf('.', at) > at + 1 && !s.endsWith(".")
    }

    /** Document id of `lookup/{id}` for an email: lowercase hex SHA-256 of the trimmed, lowercased address. */
    fun emailKey(email: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(email.trim().lowercase().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
