// data/models/Incident.kt
package com.safeguardme.app.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp

data class Incident(
    @DocumentId
    val incidentId: String = "",
    val date: Timestamp = Timestamp.now(),
    val location: String = "",
    val geoLocation: GeoPoint? = null,
    val description: String = "",
    val incidentType: IncidentType = IncidentType.OTHER,
    val severityLevel: SeverityLevel = SeverityLevel.LOW,
    val submittedToSAPS: Boolean = false,
    val submittedToNGO: Boolean = false,
    val imageUrls: List<String> = emptyList(),
    val audioUrl: String = "",
    val documentsUrls: List<String> = emptyList(),
    val witnessContacts: List<String> = emptyList(),
    val isEmergencyReport: Boolean = false,
    val requiresFollowUp: Boolean = false,
    val evidenceIntegrity: String = "", // Hash for tamper detection
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val lastModifiedAt: Timestamp? = null,
    val reportingMethod: ReportingMethod = ReportingMethod.MANUAL
) {
    // Security validation
    fun isValid(): Boolean {
        return date != null &&
                location.isNotBlank() &&
                description.isNotBlank() &&
                description.length >= 10
    }

    // Security: Sanitize for safe storage
    fun sanitized(): Incident {
        return copy(
            location = location.trim().take(200),
            description = description.trim().take(5000),
            imageUrls = imageUrls.take(10), // Limit evidence files
            documentsUrls = documentsUrls.take(5),
            witnessContacts = witnessContacts.take(5)
        )
    }

    // Generate evidence integrity hash
    fun generateEvidenceHash(): String {
        val content = "$incidentId$date$description${imageUrls.joinToString()}$audioUrl"
        return content.hashCode().toString()
    }
}

enum class IncidentType {
    PHYSICAL_VIOLENCE,
    EMOTIONAL_ABUSE,
    SEXUAL_VIOLENCE,
    ECONOMIC_ABUSE,
    STALKING,
    HARASSMENT,
    THREATS,
    OTHER
}

enum class SeverityLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class ReportingMethod {
    MANUAL,
    VOICE_TRIGGER,
    EMERGENCY_BUTTON,
    AUTOMATIC_DETECTION
}