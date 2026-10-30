package com.imrat.focuslock.security

import org.json.JSONException
import org.json.JSONObject
import java.io.File

/**
 * Observer's encrypted local store of emergency passwords, keyed by session id (UNL-2).
 * The plaintext never leaves this phone; only salted hashes go to Firestore.
 */
class PasswordVault(private val file: File, private val box: SecretBox) {

    private fun load(): JSONObject {
        if (!file.exists()) return JSONObject()
        val plain = box.open(file.readBytes()) ?: return JSONObject()
        return try { JSONObject(String(plain, Charsets.UTF_8)) } catch (_: JSONException) { JSONObject() }
    }

    private fun save(o: JSONObject) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(box.seal(o.toString().toByteArray(Charsets.UTF_8)))
        if (!tmp.renameTo(file)) {
            file.delete()
            check(tmp.renameTo(file)) { "Could not replace vault file" }
        }
    }

    @Synchronized
    fun put(key: String, password: String) = save(load().put(key, password))

    @Synchronized
    fun get(key: String): String? = load().optString(key, "").ifEmpty { null }

    @Synchronized
    fun remove(key: String) = save(load().also { it.remove(key) })

    /** Drops every entry whose key is not in [keep] (cleanup of ended sessions). */
    @Synchronized
    fun retainOnly(keep: Set<String>) {
        val o = load()
        val drop = o.keys().asSequence().filter { it !in keep }.toList()
        if (drop.isNotEmpty()) { drop.forEach(o::remove); save(o) }
    }
}
