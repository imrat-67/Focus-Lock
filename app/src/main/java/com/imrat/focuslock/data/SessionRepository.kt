package com.imrat.focuslock.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** `users/{uid}/sessions/{sid}`. Rules decide who may write what; see firestore.rules. */
class SessionRepository(private val db: FirebaseFirestore) {

    private fun col(userUid: String) = db.collection("users").document(userUid).collection("sessions")

    /** Creates a session on [userUid]'s phone. [session.createdBy] must be the signed-in uid. Returns the new id. */
    suspend fun create(userUid: String, session: LockSession): String {
        val ref = col(userUid).document()
        ref.set(session.toCreateMap()).await()
        return ref.id
    }

    /** Live sessions of [userUid] that have not yet passed their end time at [sinceMs]. */
    fun observeLive(userUid: String, sinceMs: Long): Flow<List<LockSession>> =
        col(userUid).whereGreaterThan("endAt", sinceMs).snapshots()
            .map { snap -> snap.documents.mapNotNull { d -> d.data?.let { LockSession.fromMap(d.id, it) } } }

    fun newId(userUid: String): String = col(userUid).document().id

    /** Same as [create] but with an id chosen earlier, so the phone can enforce locally first (offline-safe). */
    suspend fun createWithId(userUid: String, id: String, session: LockSession) {
        col(userUid).document(id).set(session.toCreateMap()).await()
    }

    /** Observer remote-unlock / cancel (UNL-1, SCH-4). The rules force `endedBy` to the caller. */
    suspend fun endByObserver(userUid: String, sessionId: String, observerUid: String) {
        col(userUid).document(sessionId).update(mapOf("status" to SessionStatus.ENDED.wire, "endedBy" to observerUid)).await()
    }

    /** UNL-5: Observer replaces the password hash of a live session they created (rules: only pwHash/pwSalt change). */
    suspend fun regenerate(userUid: String, sessionId: String, hash: String, salt: String) {
        col(userUid).document(sessionId).update(mapOf("pwHash" to hash, "pwSalt" to salt)).await()
    }

    /** Self-lock ended by the User (emergency exit, UNL-7). */
    suspend fun endBySelf(userUid: String, sessionId: String) = endByObserver(userUid, sessionId, userUid)

    /** Informational: the emergency password was used on the phone (only this field is writable by the User). */
    suspend fun markPasswordUsed(userUid: String, sessionId: String) {
        col(userUid).document(sessionId).update("passwordUsed", true).await()
    }
}
