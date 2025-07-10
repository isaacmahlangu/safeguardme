// data/models/Contact.kt
package com.safeguardme.app.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Contact(
    @DocumentId
    val contactId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val isPrimary: Boolean = false,
    val canReceiveEmergencyAlerts: Boolean = true,
    val trustLevel: TrustLevel = TrustLevel.TRUSTED,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val lastContactedAt: Timestamp? = null,
    val isVerified: Boolean = false
) {
    // Security validation
    fun isValid(): Boolean {
        return name.isNotBlank() &&
                phoneNumber.isNotBlank() &&
                relationship.isNotBlank() &&
                phoneNumber.matches(Regex("^\\+?[1-9]\\d{1,14}$"))
    }

    // Security: Sanitize for safe storage
    fun sanitized(): Contact {
        return copy(
            name = name.trim().take(100),
            phoneNumber = phoneNumber.replace(Regex("[^+0-9]"), "").take(20),
            relationship = relationship.trim().take(50)
        )
    }
}

enum class TrustLevel {
    TRUSTED,
    VERIFIED,
    EMERGENCY_ONLY
}