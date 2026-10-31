package com.imrat.focuslock.data

/** Pure decision: what should this phone do about sessions the server knows about? (UNL-1, UNL-5, NOT-2) */
object RemoteSync {
    data class Start(val session: LockSession, val delayMs: Long)
    data class Rehash(val id: String, val hash: String, val salt: String)
    data class Plan(val starts: List<Start>, val endIds: List<String>, val rehash: List<Rehash> = emptyList())

    /**
     * [server]: this User's sessions from Firestore. [localIds]: sessions already enforced here (or finished here).
     * [localHashes]: password hash of each session currently enforced here. Only sessions made by someone else are
     * started here; own self-locks already run locally. A changed server hash (Observer regenerated, UNL-5) is adopted.
     */
    fun plan(
        server: List<LockSession>, localIds: Set<String>, me: String, serverNowMs: Long,
        localHashes: Map<String, String?> = emptyMap(),
    ): Plan {
        val starts = server
            .filter { it.createdBy != me && it.status != SessionStatus.ENDED && !it.passwordUsed && it.endAtMs > serverNowMs && it.id !in localIds }
            .map { Start(it, (it.startAtMs - serverNowMs).coerceAtLeast(0)) }
        val ends = server.filter { it.status == SessionStatus.ENDED && it.id in localIds }.map { it.id }
        val rehash = server.mapNotNull { s ->
            val hash = s.pwHash ?: return@mapNotNull null
            val salt = s.pwSalt ?: return@mapNotNull null
            if (s.createdBy != me && s.status != SessionStatus.ENDED && s.id in localHashes && localHashes[s.id] != hash) Rehash(s.id, hash, salt) else null
        }
        return Plan(starts, ends, rehash)
    }
}
