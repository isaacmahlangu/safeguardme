// utils/SecurityUtils.kt
package com.safeguardme.app.utils

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

object SecurityUtils {

    /**
     * Enable screen security (prevent screenshots and screen recording)
     */
    fun enableScreenSecurity(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    /**
     * Disable screen security
     */
    fun disableScreenSecurity(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    /**
     * Check if device is rooted (basic check)
     */
    fun isDeviceRooted(): Boolean {
        return try {
            val buildTags = android.os.Build.TAGS
            buildTags != null && buildTags.contains("test-keys")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if app is in debug mode
     */
    fun isDebugMode(context: Context): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Generate secure random string for IDs
     */
    fun generateSecureRandomString(length: Int = 32): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    /**
     * Sanitize input for safe display and storage
     */
    fun sanitizeInput(input: String, maxLength: Int = 1000): String {
        return input
            .trim()
            .take(maxLength)
            .replace(Regex("[<>\"'&]"), "") // Remove potentially dangerous characters
            .replace(Regex("\\s+"), " ") // Normalize whitespace
    }

    /**
     * Mask phone number for display (show only last 4 digits)
     */
    fun maskPhoneNumber(phoneNumber: String): String {
        return if (phoneNumber.length > 4) {
            "*".repeat(phoneNumber.length - 4) + phoneNumber.takeLast(4)
        } else {
            phoneNumber
        }
    }

    /**
     * Mask email address for display
     */
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email

        val localPart = parts[0]
        val domain = parts[1]

        val maskedLocal = if (localPart.length > 2) {
            localPart.take(2) + "*".repeat(localPart.length - 2)
        } else {
            localPart
        }

        return "$maskedLocal@$domain"
    }
}

// Composable for screen security
@Composable
fun SecureScreen() {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(activity) {
        activity?.let { SecurityUtils.enableScreenSecurity(it) }

        onDispose {
            activity?.let { SecurityUtils.disableScreenSecurity(it) }
        }
    }
}
