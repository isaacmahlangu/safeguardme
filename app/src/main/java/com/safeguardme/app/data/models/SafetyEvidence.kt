// data/models/SafetyEvidence.kt - Data Models for Safety Evidence
package com.safeguardme.app.data.models

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Core safety evidence data model
 */
data class SafetyEvidence(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val type: EvidenceType,
    val timestamp: Long = System.currentTimeMillis(),
    val filePath: String? = null,
    val metadata: Map<String, Any> = emptyMap(),
    val localPath: String? = null,
    val firebaseStorageUrl: String? = null,
    val uploadStatus: String = "pending", // pending, uploading, completed, failed
    val uploadedAt: Long? = null,
    val priority: EvidencePriority = EvidencePriority.NORMAL,
    val verified: Boolean = false
) {

    fun toJson(): String {
        return gson.toJson(this)
    }

    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "sessionId" to sessionId,
            "type" to type.name,
            "timestamp" to timestamp,
            "filePath" to (filePath ?: ""),
            "metadata" to metadata,
            "localPath" to (localPath ?: ""),
            "firebaseStorageUrl" to (firebaseStorageUrl ?: ""),
            "uploadStatus" to uploadStatus,
            "uploadedAt" to (uploadedAt ?: 0L),
            "priority" to priority.name,
            "verified" to verified
        )
    }

    fun getFormattedTimestamp(): String {
        return SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    fun getHumanReadableType(): String {
        return when (type) {
            EvidenceType.LOCATION -> "📍 Location"
            EvidenceType.PHOTO -> "📷 Photo"
            EvidenceType.AUDIO -> "🎤 Audio Recording"
            EvidenceType.TRANSCRIPTION -> "📝 Audio Transcription"
            EvidenceType.SENSOR -> "📊 Sensor Data"
            EvidenceType.SYSTEM_LOG -> "⚙️ System Log"
            EvidenceType.USER_INPUT -> "👤 User Input"
        }
    }

    companion object {
        private val gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .create()

        fun fromJson(json: String): SafetyEvidence {
            return gson.fromJson(json, SafetyEvidence::class.java)
        }

        fun fromFirestoreMap(map: Map<String, Any>, documentId: String): SafetyEvidence {
            return SafetyEvidence(
                id = map["id"] as? String ?: documentId,
                sessionId = map["sessionId"] as? String ?: "",
                type = EvidenceType.valueOf(map["type"] as? String ?: "SYSTEM_LOG"),
                timestamp = map["timestamp"] as? Long ?: 0L,
                filePath = (map["filePath"] as? String)?.takeIf { it.isNotBlank() },
                metadata = map["metadata"] as? Map<String, Any> ?: emptyMap(),
                localPath = (map["localPath"] as? String)?.takeIf { it.isNotBlank() },
                firebaseStorageUrl = (map["firebaseStorageUrl"] as? String)?.takeIf { it.isNotBlank() },
                uploadStatus = map["uploadStatus"] as? String ?: "pending",
                uploadedAt = (map["uploadedAt"] as? Long)?.takeIf { it > 0L },
                priority = EvidencePriority.valueOf(map["priority"] as? String ?: "NORMAL"),
                verified = map["verified"] as? Boolean ?: false
            )
        }

        // Factory methods for different evidence types

        fun createLocationEvidence(
            sessionId: String,
            latitude: Double,
            longitude: Double,
            accuracy: Float,
            timestamp: Long = System.currentTimeMillis(),
            address: String? = null
        ): SafetyEvidence {
            return SafetyEvidence(
                sessionId = sessionId,
                type = EvidenceType.LOCATION,
                timestamp = timestamp,
                metadata = mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "accuracy" to accuracy,
                    "address" to (address ?: ""),
                    "mapsUrl" to "https://maps.google.com/?q=$latitude,$longitude"
                ),
                priority = EvidencePriority.HIGH
            )
        }

        fun createPhotoEvidence(
            sessionId: String,
            filePath: String,
            timestamp: Long = System.currentTimeMillis(),
            imageSize: String? = null,
            cameraFacing: String? = null
        ): SafetyEvidence {
            return SafetyEvidence(
                sessionId = sessionId,
                type = EvidenceType.PHOTO,
                timestamp = timestamp,
                filePath = filePath,
                metadata = mapOf(
                    "imageSize" to (imageSize ?: "unknown"),
                    "cameraFacing" to (cameraFacing ?: "unknown"),
                    "captureMethod" to "automatic_safety_monitoring"
                ),
                priority = EvidencePriority.HIGH
            )
        }

        fun createAudioEvidence(
            sessionId: String,
            filePath: String,
            timestamp: Long = System.currentTimeMillis(),
            duration: Long? = null,
            format: String = "wav"
        ): SafetyEvidence {
            return SafetyEvidence(
                sessionId = sessionId,
                type = EvidenceType.AUDIO,
                timestamp = timestamp,
                filePath = filePath,
                metadata = mapOf(
                    "duration" to (duration ?: 0L),
                    "format" to format,
                    "sampleRate" to 44100,
                    "channels" to 1,
                    "recordingMethod" to "background_continuous"
                ),
                priority = EvidencePriority.CRITICAL
            )
        }

        fun createTranscriptionEvidence(
            sessionId: String,
            transcription: String,
            confidence: Float,
            timestamp: Long = System.currentTimeMillis(),
            language: String = "en",
            keywords: List<String> = emptyList()
        ): SafetyEvidence {
            return SafetyEvidence(
                sessionId = sessionId,
                type = EvidenceType.TRANSCRIPTION,
                timestamp = timestamp,
                metadata = mapOf(
                    "transcription" to transcription,
                    "confidence" to confidence,
                    "language" to language,
                    "keywords" to keywords,
                    "wordCount" to transcription.split(" ").size,
                    "hasDistressKeywords" to keywords.any {
                        listOf("help", "emergency", "danger", "scared", "hurt").contains(it.lowercase())
                    }
                ),
                priority = if (keywords.isNotEmpty()) EvidencePriority.CRITICAL else EvidencePriority.NORMAL
            )
        }

        fun createSensorEvidence(
            sessionId: String,
            sensorType: String,
            sensorData: Map<String, Any>,
            timestamp: Long = System.currentTimeMillis()
        ): SafetyEvidence {
            return SafetyEvidence(
                sessionId = sessionId,
                type = EvidenceType.SENSOR,
                timestamp = timestamp,
                metadata = mapOf(
                    "sensorType" to sensorType,
                    "sensorData" to sensorData
                ),
                priority = EvidencePriority.LOW
            )
        }

        fun createSystemLogEvidence(
            sessionId: String,
            logLevel: String,
            message: String,
            component: String,
            timestamp: Long = System.currentTimeMillis()
        ): SafetyEvidence {
            return SafetyEvidence(
                sessionId = sessionId,
                type = EvidenceType.SYSTEM_LOG,
                timestamp = timestamp,
                metadata = mapOf(
                    "logLevel" to logLevel,
                    "message" to message,
                    "component" to component,
                    "deviceInfo" to getDeviceInfo()
                ),
                priority = if (logLevel == "ERROR") EvidencePriority.HIGH else EvidencePriority.LOW
            )
        }

        fun createUserInputEvidence(
            sessionId: String,
            inputType: String,
            inputData: String,
            timestamp: Long = System.currentTimeMillis()
        ): SafetyEvidence {
            return SafetyEvidence(
                sessionId = sessionId,
                type = EvidenceType.USER_INPUT,
                timestamp = timestamp,
                metadata = mapOf(
                    "inputType" to inputType,
                    "inputData" to inputData
                ),
                priority = EvidencePriority.HIGH
            )
        }

        private fun getDeviceInfo(): Map<String, String> {
            return mapOf(
                "manufacturer" to android.os.Build.MANUFACTURER,
                "model" to android.os.Build.MODEL,
                "osVersion" to android.os.Build.VERSION.RELEASE,
                "apiLevel" to android.os.Build.VERSION.SDK_INT.toString()
            )
        }
    }
}

/**
 * Safety session data model
 */
data class SafetySession(
    val id: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long,
    val evidenceCount: Int,
    val locationCount: Int,
    val photoCount: Int,
    val audioCount: Int,
    val transcriptionCount: Int,
    val status: String, // active, completed, interrupted, error
    val evidenceIds: List<String> = emptyList(),
    val triggerMethod: String? = null, // manual, gesture, voice, automatic
    val emergencyContacted: Boolean = false,
    val summary: String? = null
) {

    fun getDurationMs(): Long = endTime - startTime
    fun getDurationMinutes(): Long = getDurationMs() / (60 * 1000)
    fun getDurationSeconds(): Long = getDurationMs() / 1000

    fun getFormattedDuration(): String {
        val minutes = getDurationMinutes()
        val seconds = (getDurationSeconds() % 60)
        return if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }

    fun getFormattedStartTime(): String {
        return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(startTime))
    }

    fun getFormattedEndTime(): String {
        return SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(endTime))
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }

    fun toFirestoreMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "startTime" to startTime,
            "endTime" to endTime,
            "evidenceCount" to evidenceCount,
            "locationCount" to locationCount,
            "photoCount" to photoCount,
            "audioCount" to audioCount,
            "transcriptionCount" to transcriptionCount,
            "status" to status,
            "evidenceIds" to evidenceIds,
            "triggerMethod" to (triggerMethod ?: ""),
            "emergencyContacted" to emergencyContacted,
            "summary" to (summary ?: "")
        )
    }

    companion object {
        fun fromJson(json: String): SafetySession {
            return Gson().fromJson(json, SafetySession::class.java)
        }

        fun fromFirestoreMap(map: Map<String, Any>, documentId: String): SafetySession {
            return SafetySession(
                id = map["id"] as? String ?: documentId,
                userId = map["userId"] as? String ?: "",
                startTime = map["startTime"] as? Long ?: 0L,
                endTime = map["endTime"] as? Long ?: 0L,
                evidenceCount = (map["evidenceCount"] as? Long)?.toInt() ?: 0,
                locationCount = (map["locationCount"] as? Long)?.toInt() ?: 0,
                photoCount = (map["photoCount"] as? Long)?.toInt() ?: 0,
                audioCount = (map["audioCount"] as? Long)?.toInt() ?: 0,
                transcriptionCount = (map["transcriptionCount"] as? Long)?.toInt() ?: 0,
                status = map["status"] as? String ?: "unknown",
                evidenceIds = map["evidenceIds"] as? List<String> ?: emptyList(),
                triggerMethod = (map["triggerMethod"] as? String)?.takeIf { it.isNotBlank() },
                emergencyContacted = map["emergencyContacted"] as? Boolean ?: false,
                summary = (map["summary"] as? String)?.takeIf { it.isNotBlank() }
            )
        }
    }
}

/**
 * Evidence type enumeration
 */
enum class EvidenceType {
    LOCATION,      // GPS coordinates and location data
    PHOTO,         // Camera captured images
    AUDIO,         // Audio recordings
    TRANSCRIPTION, // Speech-to-text transcriptions
    SENSOR,        // Device sensor data (accelerometer, etc.)
    SYSTEM_LOG,    // System and app logs
    USER_INPUT     // Direct user input or actions
}

/**
 * Evidence priority levels
 */
enum class EvidencePriority {
    CRITICAL,  // Immediate attention required (emergency keywords, distress)
    HIGH,      // Important evidence (location, photos, key events)
    NORMAL,    // Regular monitoring data
    LOW        // Background information (system logs, sensor data)
}

/**
 * Evidence analysis result
 */
data class EvidenceAnalysisResult(
    val sessionId: String,
    val totalEvidence: Int,
    val criticalEvidence: Int,
    val locationPoints: Int,
    val photosCapture: Int,
    val audioMinutes: Int,
    val transcriptionWords: Int,
    val distressKeywordsDetected: Boolean,
    val emergencyTriggered: Boolean,
    val timelineSummary: List<TimelineEvent>,
    val riskAssessment: RiskLevel
)

/**
 * Timeline event for evidence analysis
 */
data class TimelineEvent(
    val timestamp: Long,
    val type: EvidenceType,
    val description: String,
    val priority: EvidencePriority,
    val location: Pair<Double, Double>? = null
) {
    fun getFormattedTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Risk assessment levels
 */
enum class RiskLevel {
    LOW,      // Normal safety monitoring, no alerts
    MEDIUM,   // Some concerning indicators
    HIGH,     // Multiple risk factors detected
    CRITICAL  // Immediate danger likely, emergency response needed
}

/**
 * Evidence validation result
 */
data class EvidenceValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    fun hasErrors(): Boolean = errors.isNotEmpty()
    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    fun getErrorMessage(): String = errors.joinToString("; ")
    fun getWarningMessage(): String = warnings.joinToString("; ")
}

/**
 * Evidence search criteria
 */
data class EvidenceSearchCriteria(
    val sessionId: String? = null,
    val type: EvidenceType? = null,
    val priority: EvidencePriority? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val hasLocation: Boolean? = null,
    val uploadStatus: String? = null,
    val verified: Boolean? = null,
    val limit: Int = 100
)