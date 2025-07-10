// utils/AppConstants.kt
package com.safeguardme.app.utils

object AppConstants {

    // App Information
    const val APP_VERSION = "1.0.0"
    const val APP_NAME = "SafeguardMe"
    const val LICENSE_TYPE = "Academic License"

    // Security Configuration
    const val MAX_LOGIN_ATTEMPTS = 5
    const val LOGIN_LOCKOUT_DURATION_MS = 300000L // 5 minutes
    const val MAX_REGISTRATION_ATTEMPTS = 3
    const val REGISTRATION_LOCKOUT_DURATION_MS = 600000L // 10 minutes

    // Data Limits
    const val MAX_CONTACTS = 10
    const val MAX_PRIMARY_CONTACTS = 3
    const val MAX_INCIDENT_DESCRIPTION_LENGTH = 5000
    const val MAX_LOCATION_LENGTH = 200
    const val MAX_IMAGES_PER_INCIDENT = 5
    const val MAX_IMAGE_SIZE_MB = 10
    const val MAX_AUDIO_SIZE_MB = 50
    const val MAX_DOCUMENT_SIZE_MB = 25

    // Network Configuration
    const val REQUEST_TIMEOUT_SECONDS = 30L
    const val RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 1000L

    // UI Configuration
    const val SPLASH_DELAY_MS = 2000L
    const val SNACKBAR_DURATION_MS = 4000L
    const val CONFIRMATION_TIMEOUT_SECONDS = 10

    // Emergency Configuration
    const val EMERGENCY_VIBRATION_DURATION_MS = 200L
    const val SAFETY_CONFIRMATION_TIMEOUT_SECONDS = 10
    const val PANIC_DOUBLE_TAP_TIMEOUT_MS = 500L

    // File Extensions
    val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    val ALLOWED_AUDIO_EXTENSIONS = setOf("mp3", "mp4", "wav", "3gp")
    val ALLOWED_DOCUMENT_EXTENSIONS = setOf("pdf", "txt", "doc", "docx")

    // Validation Patterns
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 128
    const val MIN_NAME_LENGTH = 2
    const val MAX_NAME_LENGTH = 100
    const val MIN_INCIDENT_DESCRIPTION_LENGTH = 10
    const val MIN_LOCATION_LENGTH = 3

    // Database Collections
    const val USERS_COLLECTION = "users"
    const val CONTACTS_COLLECTION = "contacts"
    const val INCIDENTS_COLLECTION = "incidents"
    const val EMERGENCY_ALERTS_COLLECTION = "emergency_alerts"
    const val AUDIT_LOGS_COLLECTION = "audit_logs"

    // Storage Paths
    const val EVIDENCE_IMAGES_PATH = "evidenceImages"
    const val AUDIO_EVIDENCE_PATH = "audioEvidence"
    const val DOCUMENT_EVIDENCE_PATH = "documentEvidence"
    const val PROFILE_IMAGES_PATH = "profileImages"

    // DataStore Keys
    const val SETTINGS_DATASTORE = "settings"
    const val DARK_MODE_KEY = "dark_mode"
    const val SOUNDS_ENABLED_KEY = "sounds_enabled"
    const val OFFLINE_MODE_KEY = "offline_mode"
    const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"

    // Error Messages
    const val GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please try again."
    const val NETWORK_ERROR_MESSAGE = "Network error. Please check your connection."
    const val AUTHENTICATION_ERROR_MESSAGE = "Authentication failed. Please try again."
    const val PERMISSION_DENIED_MESSAGE = "Access denied. Please check your permissions."
    const val FILE_TOO_LARGE_MESSAGE = "File size exceeds the maximum allowed limit."
    const val INVALID_FILE_TYPE_MESSAGE = "Invalid file type. Please select a supported file."

    // Success Messages
    const val PROFILE_UPDATED_MESSAGE = "Profile updated successfully"
    const val CONTACT_ADDED_MESSAGE = "Contact added successfully"
    const val INCIDENT_REPORTED_MESSAGE = "Incident reported successfully"
    const val SETTINGS_SAVED_MESSAGE = "Settings saved successfully"

    // Security Messages
    const val PASSWORD_VISIBLE_WARNING = "⚠️ Password visible - ensure you're in a safe location"
    const val SCREEN_RECORDING_BLOCKED = "Screen recording is blocked for your security"
    const val BIOMETRIC_REQUIRED = "Biometric authentication required"
    const val SESSION_EXPIRED = "Session expired. Please log in again."

    // Emergency Messages
    const val EMERGENCY_MODE_ACTIVATED = "Emergency mode activated. Trusted contacts will be notified."
    const val SAFETY_MODE_DISABLED = "Safety mode disabled. You are marked as safe."
    const val PANIC_BUTTON_PRESSED = "EMERGENCY: Panic button activated. Help is being dispatched."
    const val EMERGENCY_CONTACTS_NOTIFIED = "Emergency contacts have been notified of your situation."
}