// data/models/EmergencyAlert.kt
package com.safeguardme.app.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp

data class EmergencyAlert(
    @DocumentId
    val alertId: String = "",
    val userId: String = "",
    val triggerMethod: TriggerMethod = TriggerMethod.MANUAL,
    val location: GeoPoint? = null,
    val locationAddress: String = "",
    val alertLevel: AlertLevel = AlertLevel.HIGH,
    val message: String = "",
    val contactsNotified: List<String> = emptyList(),
    val authoritiesNotified: Boolean = false,
    val isActive: Boolean = true,
    val resolvedAt: Timestamp? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val deviceInfo: String = ""
) {
    fun isValid(): Boolean {
        return userId.isNotBlank() &&
                (location != null || locationAddress.isNotBlank())
    }
}

enum class TriggerMethod {
    MANUAL,
    VOICE_KEYWORD,
    PANIC_BUTTON,
    SHAKE_DETECTION,
    BIOMETRIC_DURESS
}

enum class AlertLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}