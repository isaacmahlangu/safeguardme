package com.safeguardme.app.data.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.safeguardme.app.data.models.SafetyStatus
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads / writes the `safetyStatus` field inside
 *  /users/{uid}  (root doc – no sub-collection)
 */
@Singleton
class FirebaseSafetyDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : SafetyDataSource {

    private val userDoc
        get() = firestore.collection("users")
            .document(requireNotNull(auth.currentUser?.uid) { "User not signed in" })

    override suspend fun updateSafetyStatus(status: SafetyStatus) {
        userDoc.update("safetyStatus", status.name).await()
    }

    override suspend fun getCurrentSafetyStatus(): SafetyStatus {
        val snapshot = userDoc.get().await()
        val raw = snapshot.getString("safetyStatus") ?: SafetyStatus.DISABLED.name
        return runCatching { SafetyStatus.valueOf(raw) }.getOrDefault(SafetyStatus.DISABLED)
    }
}
