package com.imrat.focuslock.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** `links/{userUid}`: one Observer per User. Permissions are enforced by firestore.rules, not here. */
class LinkRepository(private val db: FirebaseFirestore) {

    private fun doc(userUid: String) = db.collection("links").document(userUid)

    fun parse(snap: DocumentSnapshot): Link? {
        if (!snap.exists()) return null
        val status = LinkStatus.parse(snap.getString("status")) ?: return null
        val observer = snap.getString("observerUid") ?: return null
        return Link(snap.id, observer, status, snap.getTimestamp("removalRequestedAt")?.toDate()?.time)
    }

    suspend fun get(userUid: String): Link? = parse(doc(userUid).get().await())

    /** Live link of this User (null = no Observer). */
    fun observe(userUid: String): Flow<Link?> = doc(userUid).snapshots().map { parse(it) }

    /** Live list of the Users this Observer watches or has asked to watch. */
    fun observeUsersOf(observerUid: String): Flow<List<Link>> =
        db.collection("links").whereEqualTo("observerUid", observerUid).snapshots()
            .map { snap -> snap.documents.mapNotNull { parse(it) } }

    /** Observer says no to a removal request: link goes back to active. */
    suspend fun declineRemoval(userUid: String) {
        doc(userUid).update(
            mapOf("status" to LinkStatus.ACTIVE.wire, "removalRequestedAt" to FieldValue.delete())
        ).await()
    }

    /**
     * Observer asks to watch [targetUid]. Always completes quietly when the account is missing, already has
     * an Observer, or is the caller, so nobody can probe which accounts exist (LINK-3).
     */
    suspend fun request(observerUid: String, targetUid: String?) {
        if (targetUid == null || targetUid == observerUid) return
        try {
            doc(targetUid).set(
                mapOf("observerUid" to observerUid, "status" to LinkStatus.PENDING.wire, "createdAt" to FieldValue.serverTimestamp())
            ).await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
        }
    }

    /** User accepts a pending request. */
    suspend fun accept(userUid: String) {
        doc(userUid).update("status", LinkStatus.ACTIVE.wire).await()
    }

    /** User declines a pending request, or Observer cancels one. */
    suspend fun deletePending(userUid: String) {
        doc(userUid).delete().await()
    }

    /** User asks to be released (LINK-6). Caller must first check no lock is active (LINK-7). */
    suspend fun requestRemoval(userUid: String) {
        doc(userUid).update(
            mapOf("status" to LinkStatus.REMOVAL_REQUESTED.wire, "removalRequestedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    /** Observer accepts removal, or the User completes it after 7 days of silence. Same delete, rules decide who may. */
    suspend fun completeRemoval(userUid: String) {
        doc(userUid).delete().await()
    }

    companion object {
        const val AUTO_REMOVE_AFTER_MS = 7L * 24 * 60 * 60 * 1000

        fun autoRemovalDue(link: Link, nowMs: Long): Boolean =
            link.status == LinkStatus.REMOVAL_REQUESTED &&
                link.removalRequestedAtMs != null &&
                nowMs >= link.removalRequestedAtMs + AUTO_REMOVE_AFTER_MS
    }
}
