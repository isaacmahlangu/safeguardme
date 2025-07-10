// data/repositories/UserRepository.kt


package com.safeguardme.app.data.repositories

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.snapshots
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.data.models.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton



@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val usersCollection = firestore.collection("users")
    private val _currentUserFlow = MutableStateFlow<User?>(null)



    private suspend fun createUserProfileFromAuth(firebaseUser: FirebaseUser): Result<User> = try {
        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            fullName = firebaseUser.displayName ?: "",
            phoneNumber = "", // Will be updated later
            profilePhotoUrl = firebaseUser.photoUrl?.toString(),
            isEmailVerified = firebaseUser.isEmailVerified,

            // Safety-specific defaults
            safetyStatus = com.safeguardme.app.data.models.SafetyStatus.DISABLED,
            emergencyContacts = emptyList(),
            triggerKeyword = "help me", // Default keyword
        ).sanitized()

        // Use set() to guarantee creation
        usersCollection.document(user.uid)
            .set(user)
            .await()

        // Fetch the document to get the server-generated timestamps
        val createdDoc = usersCollection.document(user.uid).get().await()
        val createdUser = createdDoc.toObject(User::class.java)

        if (createdUser != null) {
            _currentUserFlow.value = createdUser
            Log.i("UserRepository", "✅ Created user profile for ${user.uid}")
            Result.success(createdUser)
        } else {
            throw Exception("Failed to retrieve created user profile")
        }
    } catch (e: Exception) {
        Log.e("UserRepository", "Failed to create user profile from auth", e)
        Result.failure(SecurityException("Failed to create user profile: ${e.message}"))
    }

    suspend fun ensureUserProfile(): Result<User> = try {
        val currentUser = auth.currentUser ?: return Result.failure(
            SecurityException("No authenticated user")
        )

        val uid = currentUser.uid
        val userDocRef = usersCollection.document(uid)
        val userDoc = userDocRef.get().await()

        if (userDoc.exists()) {
            // Profile exists, return it
            val user = userDoc.toObject(User::class.java)
            if (user != null && user.isValid()) {
                _currentUserFlow.value = user
                Result.success(user)
            } else {
                // Document exists but is malformed, recreate
                Log.w("UserRepository", "User document malformed, recreating...")
                createUserProfileFromAuth(currentUser)
            }
        } else {
            // Profile doesn't exist, create it
            Log.i("UserRepository", "User profile doesn't exist, creating...")
            createUserProfileFromAuth(currentUser)
        }
    } catch (e: Exception) {
        Log.e("UserRepository", "❌ Failed to ensure user profile", e)
        Result.failure(SecurityException("Failed to ensure user profile: ${e.message}"))
    }

    suspend fun createUserProfile(user: User): Result<Unit> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        val sanitizedUser = user.sanitized().copy(
            uid = currentUser.uid,
            email = currentUser.email ?: "",
            isEmailVerified = currentUser.isEmailVerified
        )

        require(sanitizedUser.isValid()) { "Invalid user data" }

        // FIXED: Use merge with lastActiveAt update
        val updateData = sanitizedUser.copy(
            lastActiveAt = null // Let Firebase set this via @ServerTimestamp
        )

        usersCollection.document(currentUser.uid)
            .set(updateData, SetOptions.merge())
            .await()

        // Update local cache
        _currentUserFlow.value = sanitizedUser
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("UserRepository", "❌ Failed to create/update user profile", e)
        Result.failure(SecurityException("Failed to create user profile: ${e.message}"))
    }

    // Get current user profile
    suspend fun getCurrentUserProfile(): Result<User?> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        val document = usersCollection.document(currentUser.uid).get().await()
        val user = document.toObject(User::class.java)

        _currentUserFlow.value = user
        Result.success(user)
    } catch (e: Exception) {
        Log.e("UserRepository", "❌ Failed to get user profile", e)
        Result.failure(SecurityException("Failed to get user profile: ${e.message}"))
    }

    // Observe user profile changes
    fun observeCurrentUserProfile(): Flow<User?> {
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            usersCollection.document(currentUser.uid)
                .snapshots()
                .map { snapshot ->
                    try {
                        val user = snapshot.toObject(User::class.java)
                        _currentUserFlow.value = user // Keep cache synchronized
                        user
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error parsing user document", e)
                        null
                    }
                }
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }



    suspend fun updateSafetyStatus(status: SafetyStatus): Result<Unit> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        // FIXED: Use Timestamp.now() for lastActiveAt in updates
        usersCollection.document(currentUser.uid)
            .set(mapOf(
                "safetyStatus" to status,
                "lastActiveAt" to Timestamp.now() // Use Timestamp instead of ServerTimestamp for updates
            ), SetOptions.merge())
            .await()

        // Update local cache
        val currentUserData = _currentUserFlow.value
        if (currentUserData != null) {
            _currentUserFlow.value = currentUserData.copy(safetyStatus = status)
        }

        Log.i("UserRepository", "✅ Updated safety status to $status")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("UserRepository", "❌ Failed to update safety status", e)
        Result.failure(SecurityException("Failed to update safety status: ${e.message}"))
    }

    // ✅ FIXED: Update trigger keyword with merge strategy
    suspend fun updateTriggerKeyword(keyword: String): Result<Unit> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }
        require(keyword.length >= User.MIN_TRIGGER_KEYWORD_LENGTH) { "Trigger keyword too short" }
        require(keyword.length <= User.MAX_TRIGGER_KEYWORD_LENGTH) { "Trigger keyword too long" }

        val sanitizedKeyword = keyword.trim().lowercase()

        usersCollection.document(currentUser.uid)
            .set(mapOf(
                "triggerKeyword" to sanitizedKeyword,
                "lastActiveAt" to Timestamp.now()
            ), SetOptions.merge())
            .await()

        // Update local cache
        val currentUserData = _currentUserFlow.value
        if (currentUserData != null) {
            _currentUserFlow.value = currentUserData.copy(triggerKeyword = sanitizedKeyword)
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("UserRepository", "❌ Failed to update trigger keyword", e)
        Result.failure(SecurityException("Failed to update trigger keyword: ${e.message}"))
    }
    // ✅ ADDED: Safe field update helper
    suspend fun updateUserField(field: String, value: Any): Result<Unit> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        usersCollection.document(currentUser.uid)
            .set(mapOf(
                field to value,
                "lastActiveAt" to Timestamp.now()
            ), SetOptions.merge())
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("UserRepository", "❌ Failed to update user field", e)
        Result.failure(SecurityException("Failed to update user field: ${e.message}"))
    }

    /**
     * Get current user as a Flow for reactive UI updates
     */
    fun getCurrentUser(): Flow<User?> = _currentUserFlow.asStateFlow()

    /**
     * Refresh current user data from Firestore
     */
    suspend fun refreshCurrentUser(): Result<Unit> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        val document = usersCollection.document(currentUser.uid).get().await()
        val user = document.toObject(User::class.java)

        _currentUserFlow.value = user
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("UserRepository", "❌ Failed to refresh current user", e)
        Result.failure(SecurityException("Failed to refresh user: ${e.message}"))
    }

    /**
     * Update user safety status (alias for consistency)
     */
    suspend fun updateUserSafetyStatus(status: SafetyStatus): Result<Unit> {
        return updateSafetyStatus(status)
    }

    /**
     * Initialize cache with current user data
     */
    suspend fun initializeUserCache(): Result<Unit> = try {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val document = usersCollection.document(currentUser.uid).get().await()
            val user = document.toObject(User::class.java)
            _currentUserFlow.value = user
        } else {
            _currentUserFlow.value = null
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("UserRepository", "❌ Failed to initialize user cache", e)
        Result.failure(SecurityException("Failed to initialize cache: ${e.message}"))
    }

    /**
     * Clear user cache (useful for sign-out)
     */
    fun clearUserCache() {
        _currentUserFlow.value = null
    }

    /**
     * Emergency delete user data (for emergency situations)
     */
    suspend fun emergencyDeleteUserData(): Result<Unit> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        val batch = firestore.batch()

        // Delete user document
        batch.delete(usersCollection.document(currentUser.uid))

        // TODO: Delete subcollections (contacts, incidents) in production
        // This requires recursive delete function

        batch.commit().await()

        _currentUserFlow.value = null
        Log.w("UserRepository", "⚠️ Emergency deleted user data for ${currentUser.uid}")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("UserRepository", "❌ Failed to delete user data", e)
        Result.failure(SecurityException("Failed to delete user data: ${e.message}"))
    }

    /**
     * Update emergency contacts list
     */
    suspend fun updateEmergencyContacts(contactIds: List<String>): Result<Unit> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        usersCollection.document(currentUser.uid)
            .set(mapOf(
                "emergencyContacts" to contactIds,
                "lastActiveAt" to Timestamp.now()
            ), SetOptions.merge())
            .await()

        // Update local cache
        val currentUserData = _currentUserFlow.value
        if (currentUserData != null) {
            _currentUserFlow.value = currentUserData.copy(emergencyContacts = contactIds)
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e("UserRepository", "❌ Failed to update emergency contacts", e)
        Result.failure(SecurityException("Failed to update emergency contacts: ${e.message}"))
    }
}