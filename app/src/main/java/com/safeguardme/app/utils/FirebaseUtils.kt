// utils/FirebaseUtils.kt
package com.safeguardme.app.utils

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.StorageException

object FirebaseUtils {

    // Convert Firebase exceptions to user-friendly error messages
    fun getErrorMessage(exception: Exception): String = when (exception) {
        is FirebaseFirestoreException -> when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Access denied. Please check your account permissions."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Service temporarily unavailable. Please try again."
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Request timed out. Please check your connection."
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                "Service limit reached. Please try again later."
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Authentication required. Please log in again."
            else -> "Database error occurred. Please try again."
        }

        is StorageException -> when (exception.errorCode) {
            StorageException.ERROR_OBJECT_NOT_FOUND ->
                "File not found."
            StorageException.ERROR_NOT_AUTHENTICATED ->
                "Authentication required for file access."
            StorageException.ERROR_QUOTA_EXCEEDED ->
                "Storage quota exceeded."
            StorageException.ERROR_RETRY_LIMIT_EXCEEDED ->
                "Upload failed after multiple attempts."
            else -> "File operation failed. Please try again."
        }

        is SecurityException -> exception.message ?: "Security validation failed."

        else -> "An unexpected error occurred. Please try again."
    }

    // Validate user input for security
    fun sanitizeInput(input: String, maxLength: Int = 1000): String {
        return input.trim()
            .take(maxLength)
            .replace(Regex("[<>\"'&]"), "") // Remove potentially dangerous characters
    }

    // Generate secure random IDs
    fun generateSecureId(): String {
        return java.util.UUID.randomUUID().toString()
    }

    // Validate phone number format
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val cleaned = phoneNumber.replace(Regex("[^+0-9]"), "")
        return cleaned.matches(Regex("^\\+?[1-9]\\d{1,14}$"))
    }

    // Validate email format (additional to Firebase validation)
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                email.length <= 100 &&
                !email.contains("..") &&
                !email.startsWith(".") &&
                !email.endsWith(".")
    }
}

