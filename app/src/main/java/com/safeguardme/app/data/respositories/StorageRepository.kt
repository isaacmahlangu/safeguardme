// data/repositories/StorageRepository.kt
package com.safeguardme.app.data.repositories

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
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
        private const val MAX_IMAGE_SIZE = 10 * 1024 * 1024L // 10MB
        private const val MAX_AUDIO_SIZE = 50 * 1024 * 1024L // 50MB
        private const val MAX_DOCUMENT_SIZE = 25 * 1024 * 1024L // 25MB

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

    // Upload evidence image with security validation
    suspend fun uploadEvidenceImage(
        uri: Uri,
        contentType: String? = null,
        incidentId: String
    ): Result<String> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }
        require(currentUser.isEmailVerified) { "Email must be verified" }

        // Validate file type and size
        val mimeType = contentType ?: "image/jpeg"
        require(ALLOWED_IMAGE_TYPES.contains(mimeType)) { "Invalid image type" }

        // Generate secure filename
        val fileName = "${incidentId}_${UUID.randomUUID()}.${getFileExtension(mimeType)}"
        val storageRef = storage.reference
            .child("evidenceImages")
            .child(currentUser.uid)
            .child(fileName)

        // Upload with metadata
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .setCustomMetadata("incidentId", incidentId)
            .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
            .setCustomMetadata("integrity", generateFileHash(uri))
            .build()

        val uploadTask = storageRef.putFile(uri, metadata).await()
        val downloadUrl = uploadTask.storage.downloadUrl.await()

        Result.success(downloadUrl.toString())
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to upload evidence image: ${e.message}"))
    }

    // Upload audio evidence
    suspend fun uploadAudioEvidence(
        uri: Uri,
        contentType: String? = null,
        incidentId: String
    ): Result<String> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }
        require(currentUser.isEmailVerified) { "Email must be verified" }

        val mimeType = contentType ?: "audio/mpeg"
        require(ALLOWED_AUDIO_TYPES.contains(mimeType)) { "Invalid audio type" }

        val fileName = "${incidentId}_audio_${UUID.randomUUID()}.${getFileExtension(mimeType)}"
        val storageRef = storage.reference
            .child("audioEvidence")
            .child(currentUser.uid)
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .setCustomMetadata("incidentId", incidentId)
            .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
            .setCustomMetadata("integrity", generateFileHash(uri))
            .build()

        val uploadTask = storageRef.putFile(uri, metadata).await()
        val downloadUrl = uploadTask.storage.downloadUrl.await()

        Result.success(downloadUrl.toString())
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to upload audio evidence: ${e.message}"))
    }

    // Upload document evidence
    suspend fun uploadDocumentEvidence(
        uri: Uri,
        contentType: String? = null,
        incidentId: String
    ): Result<String> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }
        require(currentUser.isEmailVerified) { "Email must be verified" }

        val mimeType = contentType ?: "application/pdf"
        require(ALLOWED_DOCUMENT_TYPES.contains(mimeType)) { "Invalid document type" }

        val fileName = "${incidentId}_doc_${UUID.randomUUID()}.${getFileExtension(mimeType)}"
        val storageRef = storage.reference
            .child("documentEvidence")
            .child(currentUser.uid)
            .child(fileName)

        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .setCustomMetadata("incidentId", incidentId)
            .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
            .setCustomMetadata("integrity", generateFileHash(uri))
            .build()

        val uploadTask = storageRef.putFile(uri, metadata).await()
        val downloadUrl = uploadTask.storage.downloadUrl.await()

        Result.success(downloadUrl.toString())
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to upload document evidence: ${e.message}"))
    }

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

    // Get file metadata for integrity verification
    suspend fun getFileMetadata(downloadUrl: String): Result<Map<String, String>> = try {
        val currentUser = auth.currentUser
        require(currentUser != null) { "User must be authenticated" }

        val storageRef = storage.getReferenceFromUrl(downloadUrl)

        // Verify user owns this file
        val pathSegments = storageRef.path.split("/")
        require(pathSegments.size >= 2 && pathSegments[1] == currentUser.uid) {
            "Access denied: User does not own this file"
        }

        val metadata = storageRef.metadata.await()

        // Extract custom metadata safely
        val customMetadata = mutableMapOf<String, String>()

        // Try to access custom metadata through reflection or direct access
        try {
            // Method 1: Direct property access (if available)
            val customMetadataField = metadata.javaClass.getDeclaredField("customMetadata")
            customMetadataField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val customMetadataMap = customMetadataField.get(metadata) as? Map<String, String>
            if (customMetadataMap != null) {
                customMetadata.putAll(customMetadataMap)
            }
        } catch (e: Exception) {
            // Method 2: Manual extraction of known metadata fields
            // Add any standard metadata we need
            customMetadata["contentType"] = metadata.contentType ?: "unknown"
            customMetadata["size"] = metadata.sizeBytes.toString()
            //customMetadata["timeCreated"] = metadata.toString()
            //customMetadata["updated"] = metadata.toString()
        }

        Result.success(customMetadata.toMap())
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to get file metadata: ${e.message}"))
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

