// data/repositories/StorageRepository.kt
package com.safeguardme.app.data.repositories

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {

    companion object {

        private const val TAG = "StorageRepo"
        private const val MAX_AUTH_RETRY_ATTEMPTS = 3
        private const val AUTH_RETRY_DELAY_MS = 1000L
        private const val EVIDENCE_IMAGES_PATH = "evidenceImages"
        private const val AUDIO_EVIDENCE_PATH = "audioEvidence"
        private const val DOCUMENT_EVIDENCE_PATH = "documentEvidence"
        private const val PROFILE_IMAGES_PATH = "profileImages"


        private const val MAX_IMAGE_SIZE = 10 * 1024 * 1024L // 10MB
        private const val MAX_AUDIO_SIZE = 50 * 1024 * 1024L // 50MB
        private const val MAX_DOCUMENT_SIZE = 25 * 1024 * 1024L // 25MB
        private const val MAX_PROFILE_IMAGE_SIZE = 2 * 1024 * 1024L // 2MB

        private val ALLOWED_IMAGE_TYPES = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
        )
        private val ALLOWED_AUDIO_TYPES = setOf(
            "audio/mpeg", "audio/mp4", "audio/wav", "audio/3gpp"
        )
        private val ALLOWED_DOCUMENT_TYPES = setOf(
            "application/pdf", "text/plain", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
    }

    /**
     * ✅ PRIVATE: Auth state validation helper (internal use only)
     */
    private suspend fun ensureAuthState(): AuthValidationResult {
        return try {
            val currentUser = auth.currentUser
                ?: return AuthValidationResult.NotAuthenticated

            // ✅ CRITICAL: Force token refresh to ensure validity
            val token = currentUser.getIdToken(true).await()

            if (token.token.isNullOrEmpty()) {
                return AuthValidationResult.InvalidToken
            }

            // ✅ VERIFICATION: Double-check auth state consistency
            val refreshedUser = auth.currentUser
            if (refreshedUser == null || refreshedUser.uid != currentUser.uid) {
                return AuthValidationResult.StateInconsistent
            }

            Log.d(TAG, "✅ Auth state validated for user: ${currentUser.uid}")
            AuthValidationResult.Valid(currentUser.uid, token.token!!)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Auth state validation failed", e)
            AuthValidationResult.ValidationFailed(e)
        }
    }

    /**
     * ✅ PRIVATE: Retry upload with auth validation (internal use only)
     */
    private suspend fun <T> retryWithAuthValidation(
        operation: suspend (String) -> T
    ): Result<T> {
        var authAttempts = 0

        while (authAttempts < MAX_AUTH_RETRY_ATTEMPTS) {
            try {
                val authResult = ensureAuthState()

                when (authResult) {
                    is AuthValidationResult.Valid -> {
                        val result = operation(authResult.userId)
                        return Result.success(result)
                    }

                    is AuthValidationResult.NotAuthenticated -> {
                        return Result.failure(SecurityException("User must be authenticated"))
                    }

                    is AuthValidationResult.InvalidToken -> {
                        authAttempts++
                        Log.w(TAG, "⚠️ Invalid token, attempt $authAttempts/$MAX_AUTH_RETRY_ATTEMPTS")

                        if (authAttempts < MAX_AUTH_RETRY_ATTEMPTS) {
                            delay(AUTH_RETRY_DELAY_MS * authAttempts)
                            continue
                        } else {
                            return Result.failure(SecurityException("Authentication token refresh failed"))
                        }
                    }

                    is AuthValidationResult.StateInconsistent -> {
                        return Result.failure(SecurityException("Authentication state inconsistent"))
                    }

                    is AuthValidationResult.ValidationFailed -> {
                        return Result.failure(SecurityException("Authentication validation failed: ${authResult.error.message}"))
                    }
                }

            } catch (e: Exception) {
                authAttempts++
                Log.e(TAG, "❌ Operation attempt $authAttempts failed", e)

                if (authAttempts >= MAX_AUTH_RETRY_ATTEMPTS) {
                    return Result.failure(SecurityException("Operation failed after $authAttempts attempts: ${e.message}"))
                }

                delay(AUTH_RETRY_DELAY_MS * authAttempts)
            }
        }

        return Result.failure(SecurityException("Operation failed - max auth attempts exceeded"))
    }



    // Upload evidence image with security validation
    suspend fun uploadEvidenceImage(
        imageUri: Uri,
        incidentId: String,
        contentType: String = "image/jpeg"
    ): Result<String> = retryWithAuthValidation { userId ->

        val timestamp = System.currentTimeMillis()
        val fileExtension = when (contentType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val fileName = "evidence_${incidentId}_${timestamp}.$fileExtension"

        Log.d(TAG, "🔒 Uploading evidence image with validated auth: $fileName for user: $userId")

        // ✅ CRITICAL: Storage path MUST match Firebase rules exactly
        val imageRef = storage.reference
            .child(EVIDENCE_IMAGES_PATH)  // Must match storage rules
            .child(userId)                // Must match authenticated user ID
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .setCustomMetadata("type", "evidence_image")
            .setCustomMetadata("incidentId", incidentId)
            .setCustomMetadata("userId", userId)
            .setCustomMetadata("uploadedAt", timestamp.toString())
            .build()

        val uploadTask = imageRef.putFile(imageUri, metadata).await()
        val downloadUrl = imageRef.downloadUrl.await().toString()

        Log.i(TAG, "✅ Evidence image uploaded: $fileName")
        downloadUrl
    }.mapCatching { it }


    // Upload audio evidence
    suspend fun uploadAudioEvidence(
        uri: Uri,
        contentType: String,
        incidentId: String
    ): Result<String> = retryWithAuthValidation { userId ->

        val timestamp = System.currentTimeMillis()
        val fileExtension = when (contentType) {
            "audio/mp4", "audio/mpeg" -> "m4a"
            "audio/wav" -> "wav"
            "audio/3gpp" -> "3gp"
            else -> "m4a"
        }
        val fileName = "audio_evidence_${incidentId}_${timestamp}.$fileExtension"

        Log.d(TAG, "🔒 Uploading audio evidence with validated auth: $fileName for user: $userId")

        // ✅ CRITICAL: Storage path MUST match Firebase rules exactly
        val audioRef = storage.reference
            .child(AUDIO_EVIDENCE_PATH)
            .child(userId)
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .setCustomMetadata("type", "audio_evidence")
            .setCustomMetadata("incidentId", incidentId)
            .setCustomMetadata("userId", userId)
            .setCustomMetadata("uploadedAt", timestamp.toString())
            .build()

        val uploadTask = audioRef.putFile(uri, metadata).await()
        val downloadUrl = audioRef.downloadUrl.await().toString()

        Log.i(TAG, "✅ Audio evidence uploaded: $fileName")
        downloadUrl
    }.mapCatching { it }

    suspend fun uploadProfileImage(
        imageUri: Uri,
        contentType: String = "image/jpeg"
    ): Result<String> = retryWithAuthValidation { userId ->

        val timestamp = System.currentTimeMillis()
        val fileExtension = when (contentType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val fileName = "profile_${timestamp}.$fileExtension"

        Log.d(TAG, "🔒 Uploading profile image with validated auth: $fileName for user: $userId")

        val imageRef = storage.reference
            .child(PROFILE_IMAGES_PATH)
            .child(userId)
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .setCustomMetadata("type", "profile_image")
            .setCustomMetadata("userId", userId)
            .setCustomMetadata("uploadedAt", timestamp.toString())
            .build()

        val uploadTask = imageRef.putFile(imageUri, metadata).await()
        val downloadUrl = imageRef.downloadUrl.await().toString()

        Log.i(TAG, "✅ Profile image uploaded: $fileName")
        downloadUrl
    }.mapCatching { it }




    // Upload document evidence
    suspend fun uploadDocumentEvidence(
        uri: Uri,
        contentType: String? = null,
        incidentId: String
    ): Result<String> = retryWithAuthValidation { userId ->

        val mimeType = contentType ?: "application/pdf"
        require(ALLOWED_DOCUMENT_TYPES.contains(mimeType)) { "Invalid document type" }

        val fileName = "${incidentId}_doc_${UUID.randomUUID()}.${getFileExtension(mimeType)}"
        val storageRef = storage.reference
            .child(DOCUMENT_EVIDENCE_PATH)
            .child(userId)
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .setCustomMetadata("incidentId", incidentId)
            .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
            .setCustomMetadata("integrity", generateFileHash(uri))
            .build()

        val uploadTask = storageRef.putFile(uri, metadata).await()
        val downloadUrl = uploadTask.storage.downloadUrl.await()

        downloadUrl.toString()
    }.mapCatching { it }

    // Delete evidence file
    suspend fun deleteEvidenceFile(downloadUrl: String): Result<Unit> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        val storageRef = storage.getReferenceFromUrl(downloadUrl)

        // Verify user owns this file by checking path
        val pathSegments = storageRef.path.split("/")
        require(pathSegments.size >= 2 && pathSegments[1] == currentUser.uid) {
            "Access denied: User does not own this file"
        }

        storageRef.delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to delete evidence file: ${e.message}"))
    }


    // Emergency: Delete all user files
    suspend fun emergencyDeleteAllUserFiles(): Result<Unit> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        val userFolders = listOf("evidenceImages", "audioEvidence", "documentEvidence", "profileImages")

        for (folder in userFolders) {
            try {
                val folderRef = storage.reference.child(folder).child(currentUser.uid)
                val listResult = folderRef.listAll().await()

                for (item in listResult.items) {
                    item.delete().await()
                }
            } catch (e: Exception) {
                // Continue with other folders even if one fails
            }
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to delete user files: ${e.message}"))
    }

    /**
     * ✅ NEW: Upload voice trigger audio to Firebase Storage
     * Aligned with storage rules: /audioEvidence/{userId}/{fileName}
     */
    suspend fun uploadVoiceTriggerAudio(
        audioUri: Uri,
        keyword: String
    ): Result<String> = retryWithAuthValidation { userId ->

        val timestamp = System.currentTimeMillis()
        val fileName = "voice_trigger_${keyword}_${timestamp}.m4a"

        Log.d(TAG, "🔒 Uploading voice trigger audio with validated auth: $fileName for user: $userId")

        val audioRef = storage.reference
            .child(AUDIO_EVIDENCE_PATH)
            .child(userId)
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType("audio/mpeg")
            .setCustomMetadata("type", "voice_trigger")
            .setCustomMetadata("keyword", keyword)
            .setCustomMetadata("userId", userId)
            .setCustomMetadata("uploadedAt", timestamp.toString())
            .build()

        val uploadTask = audioRef.putFile(audioUri, metadata).await()
        val downloadUrl = audioRef.downloadUrl.await().toString()

        Log.i(TAG, "✅ Voice trigger audio uploaded successfully: $fileName")
        downloadUrl
    }.mapCatching { it }

    private suspend fun performGenericUpload(
        uri: Uri,
        contentType: String,
        incidentId: String,
        pathPrefix: String,
        filePrefix: String,
        userId: String
    ): Result<String> = try {

        val timestamp = System.currentTimeMillis()
        val extension = getFileExtension(contentType)
        val fileName = "${filePrefix}_${incidentId}_${timestamp}.$extension"

        val fileRef = storage.reference
            .child(pathPrefix)
            .child(userId)
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType(contentType)
            .setCustomMetadata("type", pathPrefix)
            .setCustomMetadata("incidentId", incidentId)
            .setCustomMetadata("userId", userId)
            .setCustomMetadata("uploadedAt", timestamp.toString())
            .build()

        val uploadTask = fileRef.putFile(uri, metadata).await()
        val downloadUrl = fileRef.downloadUrl.await().toString()

        Log.i(TAG, "✅ $pathPrefix uploaded: $fileName")
        Result.success(downloadUrl)

    } catch (e: Exception) {
        Log.e(TAG, "❌ $pathPrefix upload failed", e)
        Result.failure(SecurityException("$pathPrefix upload failed: ${e.message}"))
    }

    suspend fun deleteVoiceTriggerAudio(downloadUrl: String): Result<Unit> = try {
        val audioRef = storage.getReferenceFromUrl(downloadUrl)
        audioRef.delete().await()

        Log.i(TAG, "✅ Voice trigger audio deleted")
        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to delete voice trigger audio", e)
        Result.failure(SecurityException("Failed to delete voice trigger audio: ${e.message}"))
    }

    suspend fun getFileMetadata(downloadUrl: String): Result<StorageMetadata> = try {
        val fileRef = storage.getReferenceFromUrl(downloadUrl)
        val metadata = fileRef.metadata.await()

        Log.d(TAG, "✅ Retrieved file metadata")
        Result.success(metadata)

    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get file metadata", e)
        Result.failure(SecurityException("Failed to get file metadata: ${e.message}"))
    }

    suspend fun listVoiceTriggerFiles(): Result<List<String>> = try {
        val currentUser = auth.currentUser
            ?: return Result.failure(SecurityException("User must be authenticated"))

        val userId = currentUser.uid
        val audioRef = storage.reference
            .child(AUDIO_EVIDENCE_PATH)
            .child(userId)

        val listResult = audioRef.listAll().await()
        val voiceTriggerFiles = listResult.items
            .filter { it.name.contains("voice_trigger") }
            .map { it.downloadUrl.await().toString() }

        Log.d(TAG, "✅ Found ${voiceTriggerFiles.size} voice trigger files")
        Result.success(voiceTriggerFiles)

    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to list voice trigger files", e)
        Result.failure(SecurityException("Failed to list voice trigger files: ${e.message}"))
    }

    fun validateFileSize(uri: Uri, maxSize: Long): Boolean {
        return try {
            val inputStream = auth.app.applicationContext.contentResolver.openInputStream(uri)
            val size = inputStream?.available()?.toLong() ?: 0L
            inputStream?.close()
            size <= maxSize
        } catch (e: Exception) {
            Log.w(TAG, "Could not determine file size", e)
            false
        }
    }

    fun getMaxFileSize(contentType: String): Long {
        return when {
            contentType.startsWith("image/") -> MAX_IMAGE_SIZE
            contentType.startsWith("audio/") -> MAX_AUDIO_SIZE
            contentType.startsWith("application/") -> MAX_DOCUMENT_SIZE
            else -> MAX_DOCUMENT_SIZE
        }
    }

    // Utility functions
    private fun getFileExtension(mimeType: String): String = when (mimeType) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "audio/mpeg" -> "mp3"
        "audio/mp4" -> "mp4"
        "audio/wav" -> "wav"
        "audio/3gpp" -> "3gp"
        "application/pdf" -> "pdf"
        "text/plain" -> "txt"
        "application/msword" -> "doc"
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
        else -> "bin"
    }

    private fun generateFileHash(uri: Uri): String = try {
        // This would need to be implemented with actual file content reading
        // For now, return a placeholder
        "hash_${System.currentTimeMillis()}"
    } catch (e: Exception) {
        "hash_unknown"
    }
}


sealed class AuthValidationResult {
    data class Valid(val userId: String, val token: String) : AuthValidationResult()
    object NotAuthenticated : AuthValidationResult()
    object InvalidToken : AuthValidationResult()
    object StateInconsistent : AuthValidationResult()
    data class ValidationFailed(val error: Exception) : AuthValidationResult()
}
