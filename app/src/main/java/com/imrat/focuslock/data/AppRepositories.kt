package com.imrat.focuslock.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** `users/{uid}/apps/list` (FMT-3). Only the User's own phone writes it; the Observer reads it. */
class AppListRepository(private val db: FirebaseFirestore) {
    private fun doc(uid: String) = db.collection("users").document(uid).collection("apps").document("list")

    suspend fun upload(uid: String, apps: List<AppEntry>) {
        doc(uid).set(mapOf("items" to AppListCodec.toItems(apps), "updatedAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun get(uid: String): List<AppEntry> = AppListCodec.fromItems(doc(uid).get().await().get("items"))
}

/** `users/{uid}/formats/{fid}` (FMT-1, FMT-2). Both the User and their Observer may edit. */
class FormatRepository(private val db: FirebaseFirestore) {
    private fun col(uid: String) = db.collection("users").document(uid).collection("formats")

    fun observe(uid: String): Flow<List<Format>> = col(uid).snapshots().map { snap ->
        snap.documents.mapNotNull { d -> d.data?.let { FormatCodec.fromMap(d.id, it) } }.sortedBy { it.name.lowercase() }
    }

    suspend fun create(uid: String, name: String, packages: Collection<String>, byUid: String): String {
        val ref = col(uid).document()
        ref.set(FormatCodec.toMap(name, packages, byUid)).await()
        return ref.id
    }

    /** createdBy stays as originally written (rules enforce it), so it is passed through. */
    suspend fun update(uid: String, f: Format, name: String, packages: Collection<String>) {
        col(uid).document(f.id).set(FormatCodec.toMap(name, packages, f.createdBy)).await()
    }

    suspend fun delete(uid: String, formatId: String) {
        col(uid).document(formatId).delete().await()
    }
}
