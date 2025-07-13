// utils/AppPermissionExtensions.kt
package com.safeguardme.app.utils

import com.safeguardme.app.managers.AppPermission

fun AppPermission.getDisplayName(): String = when (this) {
    AppPermission.LOCATION -> "Location Access"
    AppPermission.CAMERA -> "Camera Access"
    AppPermission.AUDIO_RECORDING -> "Audio Recording"
    AppPermission.PHONE_CALLS -> "Phone Access"
    AppPermission.STORAGE -> "Storage Access"
    AppPermission.SMS_MESSAGING -> "SMS Messaging"
}

fun AppPermission.getEssentialReason(): String = when (this) {
    AppPermission.LOCATION -> "Required for location-based safety alerts and emergency response"
    AppPermission.CAMERA -> "Required for capturing photo evidence and emergency documentation"
    AppPermission.AUDIO_RECORDING -> "Required for recording audio evidence and voice memos"
    AppPermission.PHONE_CALLS -> "Required for emergency calling and direct contact with help"
    AppPermission.STORAGE -> "Required for securely storing safety data and evidence"
    AppPermission.SMS_MESSAGING -> "Required for sending and receiving SMS messages"
}

fun AppPermission.getSafetyImportanceLevel(): SafetyImportanceLevel = when (this) {
    AppPermission.LOCATION -> SafetyImportanceLevel.CRITICAL
    AppPermission.PHONE_CALLS -> SafetyImportanceLevel.CRITICAL
    AppPermission.CAMERA -> SafetyImportanceLevel.HIGH
    AppPermission.AUDIO_RECORDING -> SafetyImportanceLevel.HIGH
    AppPermission.STORAGE -> SafetyImportanceLevel.MEDIUM
    AppPermission.SMS_MESSAGING -> SafetyImportanceLevel.LOW
}