package com.imrat.focuslock.data

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

sealed interface ProfileResult {
    data class Ready(val profile: UserProfile) : ProfileResult
    /** AUTH-2: this account already manages another phone. */
    data object OtherDevice : ProfileResult
}

class UserRepository(private val db: FirebaseFirestore) {

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    /** Creates `users/{uid}` + lookup docs on first sign-in; on later sign-ins checks the device (AUTH-2). */
    suspend fun ensureProfile(user: FirebaseUser, deviceId: String): ProfileResult {
        val email = user.email.orEmpty()
        val name = user.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')
        val existing = userDoc(user.uid).get().await()
        if (existing.exists()) {
            if (existing.getString("deviceId") != deviceId) return ProfileResult.OtherDevice
            if (existing.getString("displayName") != name || existing.getString("email") != email) {
                userDoc(user.uid).update(mapOf("displayName" to name, "email" to email)).await()
            }
            return ProfileResult.Ready(
                UserProfile(user.uid, name, email, existing.getString("friendCode").orEmpty(), deviceId)
            )
        }
        repeat(MAX_CODE_TRIES) {
            val code = Lookup.newFriendCode()
            if (db.collection("lookup").document(code).get().await().exists()) return@repeat
            val batch = db.batch()
            batch.set(
                userDoc(user.uid),
                mapOf(
                    "displayName" to name,
                    "email" to email,
                    "friendCode" to code,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "deviceId" to deviceId,
                ),
            )
            batch.set(db.collection("lookup").document(code), mapOf("uid" to user.uid))
            if (email.isNotEmpty()) {
                batch.set(db.collection("lookup").document(Lookup.emailKey(email)), mapOf("uid" to user.uid))
            }
            try {
                batch.commit().await()
                return ProfileResult.Ready(UserProfile(user.uid, name, email, code, deviceId))
            } catch (e: FirebaseFirestoreException) {
                // A racing sign-up can take the same code; rules then deny the lookup write. Try another code.
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) throw e
            }
        }
        error("Could not allocate a friend code")
    }

    /** Resolves an email or friend code to a uid, or null. Callers must never reveal which case it was (LINK-3). */
    suspend fun findUid(emailOrCode: String): String? {
        val key = if (Lookup.looksLikeEmail(emailOrCode)) {
            Lookup.emailKey(emailOrCode)
        } else {
            Lookup.normalizeCode(emailOrCode).takeIf(Lookup::isValidFriendCode) ?: return null
        }
        return try {
            db.collection("lookup").document(key).get().await().getString("uid")
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) null else throw e
        }
    }

    /** Display name of a linked person, or null when the rules do not let us read it (e.g. a pending request). */
    suspend fun displayName(uid: String): String? = try {
        userDoc(uid).get().await().getString("displayName")
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) null else throw e
    }

    private companion object {
        const val MAX_CODE_TRIES = 5
    }
}
