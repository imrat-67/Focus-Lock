package com.imrat.focuslock.data

/** Plain data models. Maps use Firestore wire names; no Firebase types so they are unit-testable. */

enum class LinkStatus(val wire: String) {
    PENDING("pending"), ACTIVE("active"), REMOVAL_REQUESTED("removal_requested");

    companion object {
        fun parse(s: String?): LinkStatus? = entries.firstOrNull { it.wire == s }
    }
}

enum class LockMode(val wire: String) {
    FULL("full"), APPS("apps");

    companion object {
        fun parse(s: String?): LockMode? = entries.firstOrNull { it.wire == s }
    }
}

enum class SessionKind(val wire: String) {
    INSTANT("instant"), SCHEDULED("scheduled");

    companion object {
        fun parse(s: String?): SessionKind? = entries.firstOrNull { it.wire == s }
    }
}

enum class SessionStatus(val wire: String) {
    PENDING("pending"), ACTIVE("active"), ENDED("ended");

    companion object {
        fun parse(s: String?): SessionStatus? = entries.firstOrNull { it.wire == s }
    }
}

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val friendCode: String,
    val deviceId: String,
)

data class Link(
    val userUid: String,
    val observerUid: String,
    val status: LinkStatus,
    val removalRequestedAtMs: Long?,
)

data class LockSession(
    val id: String,
    val createdBy: String,
    val mode: LockMode,
    val kind: SessionKind,
    val packages: List<String>,
    val startAtMs: Long,
    val endAtMs: Long,
    val status: SessionStatus,
    val pwHash: String?,
    val pwSalt: String?,
    val endedBy: String?,
    val remoteInstant: Boolean,
    val passwordUsed: Boolean,
) {
    fun toCreateMap(): Map<String, Any?> = buildMap {
        put("createdBy", createdBy)
        put("mode", mode.wire)
        put("kind", kind.wire)
        put("packages", packages)
        put("startAt", startAtMs)
        put("endAt", endAtMs)
        put("status", status.wire)
        put("remoteInstant", remoteInstant)
        if (pwHash != null) put("pwHash", pwHash)
        if (pwSalt != null) put("pwSalt", pwSalt)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, m: Map<String, Any?>): LockSession? {
            val mode = LockMode.parse(m["mode"] as? String) ?: return null
            val kind = SessionKind.parse(m["kind"] as? String) ?: return null
            val status = SessionStatus.parse(m["status"] as? String) ?: return null
            val createdBy = m["createdBy"] as? String ?: return null
            val start = (m["startAt"] as? Number)?.toLong() ?: return null
            val end = (m["endAt"] as? Number)?.toLong() ?: return null
            return LockSession(
                id = id,
                createdBy = createdBy,
                mode = mode,
                kind = kind,
                packages = (m["packages"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                startAtMs = start,
                endAtMs = end,
                status = status,
                pwHash = m["pwHash"] as? String,
                pwSalt = m["pwSalt"] as? String,
                endedBy = m["endedBy"] as? String,
                remoteInstant = m["remoteInstant"] as? Boolean ?: false,
                passwordUsed = m["passwordUsed"] as? Boolean ?: false,
            )
        }
    }
}
