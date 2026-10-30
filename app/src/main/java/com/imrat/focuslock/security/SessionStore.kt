package com.imrat.focuslock.security

import com.imrat.focuslock.data.LockMode
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Session as the phone enforces it. Enforcement reads ONLY this (works offline, LOCK-6).
 * [endElapsedMs] is on the monotonic `elapsedRealtime` clock; [endAtServerMs] is the server-time end for re-deriving after reboot (LOCK-8).
 */
data class LocalSession(
    val id: String,
    val createdBy: String,
    val mode: LockMode,
    val packages: List<String>,
    val endAtServerMs: Long,
    val endElapsedMs: Long,
    val pwHash: String?,
    val pwSalt: String?,
    val failedAttempts: Int = 0,
    val cooldownUntilElapsedMs: Long = 0L,
)

internal object SessionCodec {
    fun encode(list: List<LocalSession>): ByteArray {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(
                JSONObject()
                    .put("id", s.id).put("by", s.createdBy).put("mode", s.mode.wire)
                    .put("pkgs", JSONArray(s.packages))
                    .put("endSrv", s.endAtServerMs).put("endEl", s.endElapsedMs)
                    .put("h", s.pwHash ?: JSONObject.NULL).put("s", s.pwSalt ?: JSONObject.NULL)
                    .put("fails", s.failedAttempts).put("cool", s.cooldownUntilElapsedMs)
            )
        }
        return JSONObject().put("v", 1).put("sessions", arr).toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(bytes: ByteArray): List<LocalSession>? = try {
        val arr = JSONObject(String(bytes, Charsets.UTF_8)).getJSONArray("sessions")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val pk = o.getJSONArray("pkgs")
            LocalSession(
                id = o.getString("id"),
                createdBy = o.getString("by"),
                mode = LockMode.parse(o.getString("mode")) ?: return null,
                packages = (0 until pk.length()).map { pk.getString(it) },
                endAtServerMs = o.getLong("endSrv"),
                endElapsedMs = o.getLong("endEl"),
                pwHash = if (o.isNull("h")) null else o.getString("h"),
                pwSalt = if (o.isNull("s")) null else o.getString("s"),
                failedAttempts = o.getInt("fails"),
                cooldownUntilElapsedMs = o.getLong("cool"),
            )
        }
    } catch (_: org.json.JSONException) {
        null
    }
}

/** Encrypted file holding the active sessions. Writes are atomic (temp file + rename). */
class SessionStore(private val file: File, private val box: SecretBox) {

    @Synchronized
    fun load(): List<LocalSession> {
        if (!file.exists()) return emptyList()
        val plain = box.open(file.readBytes()) ?: return emptyList()
        return SessionCodec.decode(plain) ?: emptyList()
    }

    @Synchronized
    fun save(sessions: List<LocalSession>) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(box.seal(SessionCodec.encode(sessions)))
        if (!tmp.renameTo(file)) {
            file.delete()
            check(tmp.renameTo(file)) { "Could not replace session file" }
        }
    }

    @Synchronized
    fun update(sessionId: String, change: (LocalSession) -> LocalSession) =
        save(load().map { if (it.id == sessionId) change(it) else it })

    @Synchronized
    fun remove(sessionId: String) = save(load().filterNot { it.id == sessionId })
}
