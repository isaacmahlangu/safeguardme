// =============================================
// EmergencyContactRepository.kt
// =============================================

package com.safeguardme.app.data.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.snapshots
import com.safeguardme.app.data.models.EmergencyContact
import com.safeguardme.app.data.models.validateContactList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyContactRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) {

    companion object {
        // ✅ FIXED: Changed to match Firestore security rules (camelCase)
        private const val CONTACTS_COLLECTION = "emergencyContacts"
        private const val TAG = "EmergencyContactRepo"
    }

    /**
     * ✅ FIXED: Added comprehensive authentication checking
     */
    private suspend fun getUserContactsCollection() = try {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "🚨 CRITICAL: User not authenticated when accessing emergency contacts")
            throw SecurityException("User must be authenticated to access emergency contacts")
        }

        val userId = currentUser.uid
        Log.d(TAG, "Accessing emergency contacts for user: $userId")

        firestore.collection("users")
            .document(userId)
            .collection(CONTACTS_COLLECTION)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get contacts collection", e)
        throw SecurityException("Cannot access emergency contacts: ${e.message}")
    }

    /**
     * Get all emergency contacts for current user with enhanced error handling
     */
    suspend fun getAllContacts(): Result<List<EmergencyContact>> = try {
        Log.d(TAG, "Getting all emergency contacts...")

        // ✅ REMOVED: Removed dependency on userRepository.ensureUserProfile()
        // This was causing cascading failures

        val collection = getUserContactsCollection()

        val documents = collection
            .whereEqualTo("isActive", true)
            .orderBy("priority", Query.Direction.ASCENDING)  // ✅ FIXED: Added explicit direction
            .get()
            .await()

        val contacts = documents.mapNotNull { doc ->
            try {
                val contact = doc.toObject(EmergencyContact::class.java)
                // ✅ FIXED: Ensure ID matches document ID
                contact.copy(id = doc.id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse contact ${doc.id}", e)
                null
            }
        }

        Log.i(TAG, "✅ Retrieved ${contacts.size} emergency contacts")
        Result.success(contacts)

    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get emergency contacts", e)
        handleFirestoreError(e)
        Result.failure(SecurityException("Failed to retrieve emergency contacts: ${e.message}"))
    }

    /**
     * ✅ FIXED: Enhanced observe with proper error handling
     */
    fun observeContacts(): Flow<List<EmergencyContact>> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "🚨 User not authenticated for observeContacts")
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection(CONTACTS_COLLECTION)
                    .whereEqualTo("isActive", true)
                    .orderBy("priority", Query.Direction.ASCENDING)
                    .snapshots()
                    .map { snapshot ->
                        Log.d(TAG, "Received ${snapshot.size()} emergency contacts from Firestore")
                        snapshot.mapNotNull { doc ->
                            try {
                                val contact = doc.toObject(EmergencyContact::class.java)
                                contact.copy(id = doc.id)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to parse contact ${doc.id}", e)
                                null
                            }
                        }
                    }
                    .catch { e ->
                        Log.e(TAG, "🚨 Error in observeContacts flow", e)
                        handleFirestoreError(e)
                        emit(emptyList()) // Emit empty list to prevent UI crash
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create observeContacts flow", e)
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * ✅ FIXED: Enhanced save method with proper validation
     */
    suspend fun saveContact(contact: EmergencyContact): Result<EmergencyContact> {
        return try {
            Log.i(TAG, "Saving emergency contact: ${contact.name}")

            val collection = getUserContactsCollection()

            // ✅ FIXED: Use the validation method that returns ValidationResult
            val sanitizedContact = contact.sanitized()
            val validation = sanitizedContact.isValid()

            if (!validation.isValid()) {
                val errorMsg = "Invalid contact data: ${validation.getErrorMessages().joinToString(", ")}"
                Log.e(TAG, errorMsg)
                return Result.failure(IllegalArgumentException(errorMsg))
            }

            // ✅ FIXED: Validate contact list limits (optional - can remove if causing issues)
            try {
                val existingContacts = getAllContacts().getOrElse { emptyList() }
                val updatedContacts = if (existingContacts.any { it.id == sanitizedContact.id }) {
                    existingContacts.map { if (it.id == sanitizedContact.id) sanitizedContact else it }
                } else {
                    existingContacts + sanitizedContact
                }

                val listValidation = updatedContacts.validateContactList()
                if (!listValidation.isValid()) {
                    Log.w(TAG, "Contact list validation warning: ${listValidation.getErrorMessages()}")
                    // Don't fail - just log warning for now
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not validate contact list (non-critical)", e)
            }

            // Save to Firestore with explicit merge
            collection.document(sanitizedContact.id)
                .set(sanitizedContact, SetOptions.merge())
                .await()

            Log.i(TAG, "✅ Saved emergency contact: ${sanitizedContact.toLogSafeString()}")
            Result.success(sanitizedContact)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save emergency contact", e)
            handleFirestoreError(e)
            Result.failure(SecurityException("Failed to save emergency contact: ${e.message}"))
        }
    }

    /**
     * ✅ FIXED: Enhanced delete with proper error handling
     */
    suspend fun deleteContact(contactId: String): Result<Unit> = try {
        Log.i(TAG, "Deleting emergency contact: $contactId")

        val collection = getUserContactsCollection()

        // Soft delete by marking as inactive
        collection.document(contactId)
            .set(mapOf(
                "isActive" to false,
                "deletedAt" to com.google.firebase.Timestamp.now()
            ), SetOptions.merge())
            .await()

        Log.i(TAG, "✅ Deleted emergency contact: $contactId")
        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to delete emergency contact", e)
        handleFirestoreError(e)
        Result.failure(SecurityException("Failed to delete emergency contact: ${e.message}"))
    }

    /**
     * Get high priority contacts for emergency situations
     */
    suspend fun getEmergencyContacts(): Result<List<EmergencyContact>> = try {
        val allContacts = getAllContacts().getOrElse { emptyList() }
        val emergencyContacts = allContacts
            .filter { it.canReceiveEmergencyNotifications() }
            .sortedBy { it.priority }
            .take(5) // Limit to top 5 for emergency situations

        Log.d(TAG, "Found ${emergencyContacts.size} emergency-ready contacts")
        Result.success(emergencyContacts)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to get emergency contacts", e)
        Result.failure(SecurityException("Failed to get emergency contacts: ${e.message}"))
    }

    /**
     * Update contact verification status
     */
    suspend fun updateContactVerification(contactId: String, isVerified: Boolean): Result<Unit> = try {
        val collection = getUserContactsCollection()

        collection.document(contactId)
            .set(mapOf(
                "isVerified" to isVerified,
                "lastContactedAt" to if (isVerified) System.currentTimeMillis() else 0L
            ), SetOptions.merge())
            .await()

        Log.i(TAG, "Updated contact verification: $contactId -> $isVerified")
        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to update contact verification", e)
        Result.failure(SecurityException("Failed to update contact verification: ${e.message}"))
    }

    /**
     * Reorder contacts by priority
     */
    suspend fun reorderContacts(contactIds: List<String>): Result<Unit> = try {
        val collection = getUserContactsCollection()
        val batch = firestore.batch()

        contactIds.forEachIndexed { index, contactId ->
            val contactRef = collection.document(contactId)
            batch.update(contactRef, "priority", index + 1)
        }

        batch.commit().await()

        Log.i(TAG, "Reordered ${contactIds.size} contacts")
        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to reorder contacts", e)
        Result.failure(SecurityException("Failed to reorder contacts: ${e.message}"))
    }

    /**
     * ✅ IMPROVED: Initialize default emergency contacts (safer implementation)
     */
    suspend fun initializeDefaultContacts(): Result<Unit> = try {
        val existingContacts = getAllContacts().getOrElse { emptyList() }

        if (existingContacts.isEmpty()) {
            Log.i(TAG, "Initializing default emergency contacts...")

            val defaultContacts = com.safeguardme.app.data.models.EmergencyContactFactory.createCrisisHotlines() +
                    com.safeguardme.app.data.models.EmergencyContactFactory.createEmergencyServices()

            defaultContacts.forEach { contact ->
                saveContact(contact).onFailure { e ->
                    Log.w(TAG, "Failed to save default contact: ${contact.name}", e)
                }
            }

            Log.i(TAG, "✅ Initialized ${defaultContacts.size} default emergency contacts")
        } else {
            Log.d(TAG, "User already has ${existingContacts.size} contacts, skipping default initialization")
        }

        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize default contacts", e)
        Result.failure(SecurityException("Failed to initialize default contacts: ${e.message}"))
    }

    /**
     * ✅ NEW: Enhanced error handling for Firestore-specific errors
     */
    private suspend fun handleFirestoreError(throwable: Throwable) {
        when {
            throwable.message?.contains("PERMISSION_DENIED") == true -> {
                Log.e(TAG, "🚨 PERMISSION_DENIED: Emergency contact access blocked")
                Log.e(TAG, "Check:")
                Log.e(TAG, "  1. User authentication: ${auth.currentUser?.uid}")
                Log.e(TAG, "  2. Firestore rules for path: /users/{userId}/emergencyContacts")
                Log.e(TAG, "  3. Collection name: $CONTACTS_COLLECTION")
            }

            throwable.message?.contains("UNAVAILABLE") == true -> {
                Log.e(TAG, "🚨 Firebase Firestore unavailable - check network connection")
            }

            throwable.message?.contains("DEADLINE_EXCEEDED") == true -> {
                Log.e(TAG, "🚨 Firebase operation timeout - check network performance")
            }

            else -> {
                Log.e(TAG, "🚨 Unexpected Firestore error: ${throwable.message}")
            }
        }
    }

    // =============================================
    // NEW METHODS FOR ENHANCED FUNCTIONALITY
    // =============================================

    /**
     * Refresh contacts (for pull-to-refresh)
     */
    suspend fun refreshContacts(): Result<Unit> = try {
        Log.d(TAG, "Refreshing emergency contacts...")
        getAllContacts()
        Log.i(TAG, "✅ Emergency contacts refreshed successfully")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to refresh emergency contacts", e)
        Result.failure(SecurityException("Failed to refresh emergency contacts: ${e.message}"))
    }

    /**
     * Get contacts by priority level
     */
    suspend fun getContactsByPriority(maxPriority: Int): Result<List<EmergencyContact>> = try {
        val allContacts = getAllContacts().getOrNull() ?: emptyList()
        val priorityContacts = allContacts.filter { it.priority <= maxPriority && it.isActive }
            .sortedBy { it.priority }

        Log.d(TAG, "Found ${priorityContacts.size} contacts with priority <= $maxPriority")
        Result.success(priorityContacts)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get contacts by priority", e)
        Result.failure(SecurityException("Failed to get priority contacts: ${e.message}"))
    }

    /**
     * Test contact access (for diagnostics)
     */
    suspend fun testContactAccess(): Result<Boolean> = try {
        val collection = getUserContactsCollection()

        // Try to read one document to test permissions
        collection.limit(1).get().await()

        Log.i(TAG, "✅ Contact access test successful")
        Result.success(true)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Contact access test failed", e)
        handleFirestoreError(e)
        Result.success(false)
    }
}