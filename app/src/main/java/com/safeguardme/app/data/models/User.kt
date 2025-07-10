// data/models/User.kt
package com.safeguardme.app.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 */
data class User(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val profilePhotoUrl: String? = null,
    val isEmailVerified: Boolean = false,

    // FIXED: Use Timestamp instead of Long for Firebase compatibility
    @ServerTimestamp
    val createdAt: Timestamp? = null,

    @ServerTimestamp
    val lastActiveAt: Timestamp? = null,

    // Safety-specific fields
    val safetyStatus: SafetyStatus = SafetyStatus.DISABLED,
    val emergencyContacts: List<String> = emptyList(), // Contact IDs
    val triggerKeyword: String = "help me",

    // Additional profile fields
    val isProfileComplete: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),

    // Emergency settings
    val emergencySettings: EmergencySettings = EmergencySettings()
) {
    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_PHONE_LENGTH = 20
        const val MAX_TRIGGER_KEYWORD_LENGTH = 50
        const val MIN_TRIGGER_KEYWORD_LENGTH = 3
    }

    /**
     * Validate user data for safety requirements
     */
    fun isValid(): Boolean {
        return uid.isNotBlank() &&
                email.isNotBlank() &&
                fullName.isNotBlank() &&
                fullName.length <= MAX_NAME_LENGTH &&
                triggerKeyword.length >= MIN_TRIGGER_KEYWORD_LENGTH &&
                triggerKeyword.length <= MAX_TRIGGER_KEYWORD_LENGTH
    }

    /**
     * Sanitize user data for security
     */
    fun sanitized(): User {
        return this.copy(
            email = email.trim().lowercase(),
            fullName = fullName.trim(),
            phoneNumber = phoneNumber.trim(),
            triggerKeyword = triggerKeyword.trim().lowercase()
        )
    }

    /**
     * Check if user profile is complete for emergency use
     */
    fun isEmergencyReady(): Boolean {
        return isValid() &&
                phoneNumber.isNotBlank() &&
                emergencyContacts.isNotEmpty() &&
                hasCompletedOnboarding
    }

    /**
     * Get user's first name for display
     */
    fun getFirstName(): String {
        return fullName.split(" ").firstOrNull()?.trim() ?: "User"
    }

    /**
     * Get display name with fallback
     */
    fun getDisplayName(): String {
        return when {
            fullName.isNotBlank() -> fullName
            email.isNotBlank() -> email.substringBefore("@")
            else -> "User"
        }
    }
}

/**
 * Safety status enum
 */
enum class SafetyStatus {
    DISABLED,
    ENABLED,
    EMERGENCY
}

/**
 * Notification preferences for emergency app
 */
data class NotificationPreferences(
    val enablePushNotifications: Boolean = true,
    val enableSMSAlerts: Boolean = true,
    val enableEmailAlerts: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val emergencyAlertsAlwaysEnabled: Boolean = true // Never silence emergency alerts
)

/**
 * Emergency-specific settings
 */
data class EmergencySettings(
    val autoLocationSharing: Boolean = true,
    val emergencyCallEnabled: Boolean = true,
    val panicButtonEnabled: Boolean = true,
    val voiceActivationEnabled: Boolean = false,
    val emergencyRecordingEnabled: Boolean = false,
    val automaticCheck: Boolean = true,
    val checkInFrequencyMinutes: Int = 60,
    val maxResponseTimeMinutes: Int = 5
)

/**
 * User creation helper for Firebase compatibility
 */
object UserFactory {

    /**
     * Create user from Firebase Auth with proper timestamp handling
     */
    fun fromFirebaseAuth(
        uid: String,
        email: String,
        displayName: String? = null,
        photoUrl: String? = null,
        isEmailVerified: Boolean = false
    ): User {
        return User(
            uid = uid,
            email = email,
            fullName = displayName ?: "",
            profilePhotoUrl = photoUrl,
            isEmailVerified = isEmailVerified,
            // Don't set timestamps here - let Firebase handle them
            createdAt = null,
            lastActiveAt = null
        )
    }

    /**
     * Create user for manual registration
     */
    fun createNew(
        uid: String,
        email: String,
        fullName: String,
        phoneNumber: String = ""
    ): User {
        return User(
            uid = uid,
            email = email,
            fullName = fullName,
            phoneNumber = phoneNumber,
            // Firebase will set these automatically
            createdAt = null,
            lastActiveAt = null
        ).sanitized()
    }

    /**
     * Create user with emergency defaults
     */
    fun createEmergencyUser(
        uid: String,
        email: String,
        fullName: String,
        phoneNumber: String,
        triggerKeyword: String = "help me"
    ): User {
        return User(
            uid = uid,
            email = email,
            fullName = fullName,
            phoneNumber = phoneNumber,
            triggerKeyword = triggerKeyword,
            emergencySettings = EmergencySettings(
                autoLocationSharing = true,
                emergencyCallEnabled = true,
                panicButtonEnabled = true
            ),
            createdAt = null,
            lastActiveAt = null
        ).sanitized()
    }
}

/**
 * Extension functions for User operations
 */

/**
 * Update last active timestamp helper
 */
fun User.withUpdatedActivity(): User {
    return this.copy(lastActiveAt = Timestamp.now())
}

/**
 * Toggle safety status helper
 */
fun User.toggleSafetyStatus(): User {
    val newStatus = when (safetyStatus) {
        SafetyStatus.DISABLED -> SafetyStatus.ENABLED
        SafetyStatus.ENABLED -> SafetyStatus.DISABLED
        SafetyStatus.EMERGENCY -> SafetyStatus.DISABLED
    }
    return this.copy(safetyStatus = newStatus)
}

/**
 * Add emergency contact helper
 */
fun User.addEmergencyContact(contactId: String): User {
    return if (!emergencyContacts.contains(contactId)) {
        this.copy(emergencyContacts = emergencyContacts + contactId)
    } else {
        this
    }
}

/**
 * Remove emergency contact helper
 */
fun User.removeEmergencyContact(contactId: String): User {
    return this.copy(emergencyContacts = emergencyContacts - contactId)
}

/**
 * Complete onboarding helper
 */
fun User.completeOnboarding(): User {
    return this.copy(
        hasCompletedOnboarding = true,
        isProfileComplete = isValid() && phoneNumber.isNotBlank()
    )
}

enum class SecurityLevel {
    STANDARD,
    HIGH_SECURITY,
    MAXIMUM_PROTECTION
}