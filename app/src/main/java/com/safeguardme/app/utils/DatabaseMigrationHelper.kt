// utils/DatabaseMigrationHelper.kt - Fix Existing User Documents
package com.safeguardme.app.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseMigrationHelper @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val TAG = "DatabaseMigration"
        private const val MIGRATION_VERSION_KEY = "migrationVersion"
        private const val CURRENT_MIGRATION_VERSION = 1
    }

    /**
     * ✅ Run all necessary database migrations
     */
    suspend fun runMigrations(): Result<Unit> = try {
        Log.d(TAG, "🔄 Starting database migrations...")

        // Check current migration version
        val currentVersion = getCurrentMigrationVersion()
        Log.d(TAG, "📊 Current migration version: $currentVersion")

        if (currentVersion < CURRENT_MIGRATION_VERSION) {
            // Run migrations sequentially
            if (currentVersion < 1) {
                migrateUserTimestamps()
                setMigrationVersion(1)
            }

            Log.i(TAG, "✅ Database migrations completed successfully")
        } else {
            Log.d(TAG, "✅ Database is up to date")
        }

        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "❌ Database migration failed", e)
        Result.failure(e)
    }

    /**
     * ✅ Migration 1: Fix user document timestamp and field issues
     */
    private suspend fun migrateUserTimestamps() {
        Log.d(TAG, "🔧 Running Migration 1: User timestamp and field fixes")

        try {
            val usersCollection = firestore.collection("users")
            val documents = usersCollection.get().await()

            var processedCount = 0
            var errorCount = 0

            for (document in documents) {
                try {
                    val data = document.data.toMutableMap()
                    var needsUpdate = false

                    // ✅ FIX: Ensure required fields exist
                    if (!data.containsKey("valid")) {
                        data["valid"] = true
                        needsUpdate = true
                    }

                    if (!data.containsKey("emailVerified")) {
                        data["emailVerified"] = data["isEmailVerified"] ?: false
                        needsUpdate = true
                    }

                    if (!data.containsKey("audioFileSize")) {
                        data["audioFileSize"] = null
                        needsUpdate = true
                    }

                    if (!data.containsKey("profileSecurityLevel")) {
                        data["profileSecurityLevel"] = null
                        needsUpdate = true
                    }

                    // ✅ FIX: Ensure timestamps are properly formatted
                    val createdAt = data["createdAt"]
                    if (createdAt != null && createdAt !is com.google.firebase.Timestamp) {
                        // Convert to long timestamp for consistency
                        data["createdAt"] = when (createdAt) {
                            is Long -> createdAt
                            is Number -> createdAt.toLong()
                            else -> System.currentTimeMillis()
                        }
                        needsUpdate = true
                    }

                    val lastActiveAt = data["lastActiveAt"]
                    if (lastActiveAt != null && lastActiveAt !is com.google.firebase.Timestamp) {
                        data["lastActiveAt"] = when (lastActiveAt) {
                            is Long -> lastActiveAt
                            is Number -> lastActiveAt.toLong()
                            else -> System.currentTimeMillis()
                        }
                        needsUpdate = true
                    }

                    // ✅ FIX: Ensure safety status is a string
                    val safetyStatus = data["safetyStatus"]
                    if (safetyStatus != null && safetyStatus !is String) {
                        data["safetyStatus"] = "DISABLED"
                        needsUpdate = true
                    }

                    // ✅ UPDATE: Apply changes if needed
                    if (needsUpdate) {
                        usersCollection.document(document.id)
                            .set(data, SetOptions.merge())
                            .await()

                        Log.d(TAG, "✅ Updated user document: ${document.id}")
                        processedCount++
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to migrate user document ${document.id}", e)
                    errorCount++
                }
            }

            Log.i(TAG, "✅ Migration 1 completed: $processedCount updated, $errorCount errors")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Migration 1 failed", e)
            throw e
        }
    }

    /**
     * ✅ Get current migration version from Firestore
     */
    private suspend fun getCurrentMigrationVersion(): Int {
        return try {
            val doc = firestore.collection("system")
                .document("migrations")
                .get()
                .await()

            if (doc.exists()) {
                (doc.data?.get(MIGRATION_VERSION_KEY) as? Long)?.toInt() ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get migration version, assuming 0", e)
            0
        }
    }

    /**
     * ✅ Set migration version in Firestore
     */
    private suspend fun setMigrationVersion(version: Int) {
        try {
            firestore.collection("system")
                .document("migrations")
                .set(mapOf(
                    MIGRATION_VERSION_KEY to version,
                    "lastMigrationAt" to System.currentTimeMillis()
                ), SetOptions.merge())
                .await()

            Log.d(TAG, "✅ Set migration version to: $version")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set migration version", e)
            throw e
        }
    }

    /**
     * ✅ Emergency fix for specific user document
     */
    suspend fun fixUserDocument(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🚨 Emergency fix for user: $userId")

            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                return Result.failure(Exception("User document not found"))
            }

            val data = userDoc.data?.toMutableMap() ?: mutableMapOf()

            // Apply all fixes
            data["valid"] = true
            data["emailVerified"] = data["isEmailVerified"] ?: false
            data["audioFileSize"] = null
            data["profileSecurityLevel"] = null

            // Fix timestamps
            data["createdAt"] = data["createdAt"] ?: System.currentTimeMillis()
            data["lastActiveAt"] = System.currentTimeMillis()

            // Ensure safety status is string
            if (data["safetyStatus"] !is String) {
                data["safetyStatus"] = "DISABLED"
            }

            firestore.collection("users")
                .document(userId)
                .set(data, SetOptions.merge())
                .await()

            Log.i(TAG, "✅ Emergency fix completed for user: $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Emergency fix failed for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ Validate user document structure
     */
    suspend fun validateUserDocument(userId: String): Result<List<String>> {
        return try {
            Log.d(TAG, "🔍 Validating user document: $userId")

            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                return Result.failure(Exception("User document not found"))
            }

            val data = userDoc.data ?: emptyMap()
            val issues = mutableListOf<String>()

            // Check required fields
            if (!data.containsKey("valid")) issues.add("Missing 'valid' field")
            if (!data.containsKey("emailVerified")) issues.add("Missing 'emailVerified' field")
            if (!data.containsKey("createdAt")) issues.add("Missing 'createdAt' field")
            if (!data.containsKey("lastActiveAt")) issues.add("Missing 'lastActiveAt' field")

            // Check timestamp types
            val createdAt = data["createdAt"]
            if (createdAt != null && createdAt !is com.google.firebase.Timestamp && createdAt !is Long && createdAt !is Number) {
                issues.add("Invalid 'createdAt' type: ${createdAt::class.java}")
            }

            val lastActiveAt = data["lastActiveAt"]
            if (lastActiveAt != null && lastActiveAt !is com.google.firebase.Timestamp && lastActiveAt !is Long && lastActiveAt !is Number) {
                issues.add("Invalid 'lastActiveAt' type: ${lastActiveAt::class.java}")
            }

            // Check safety status
            val safetyStatus = data["safetyStatus"]
            if (safetyStatus != null && safetyStatus !is String) {
                issues.add("Invalid 'safetyStatus' type: ${safetyStatus::class.java}")
            }

            Log.d(TAG, "✅ Validation completed: ${issues.size} issues found")
            Result.success(issues)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Validation failed for user: $userId", e)
            Result.failure(e)
        }
    }
}