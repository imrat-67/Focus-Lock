package com.imrat.focuslock.data

/** Pure decision: what should this phone do about sessions the server knows about? (UNL-1, NOT-2) */
object RemoteSync {
    data class Start(val session: LockSession, val delayMs: Long)
    data class Plan(val starts: List<Start>, val endIds: List<String>)

    /**
     * [server]: this User's sessions from Firestore. [localIds]: sessions already enforced here.
     * Only sessions made by someone else are started here; own self-locks already run locally.
     */
    fun plan(server: List<LockSession>, localIds: Set<String>, me: String, serverNowMs: Long): Plan {
        val starts = server
            .filter { it.createdBy != me && it.status != SessionStatus.ENDED && !it.passwordUsed && it.endAtMs > serverNowMs && it.id !in localIds }
            .map { Start(it, (it.startAtMs - serverNowMs).coerceAtLeast(0)) }
        val ends = server.filter { it.status == SessionStatus.ENDED && it.id in localIds }.map { it.id }
        return Plan(starts, ends)
    }
}
