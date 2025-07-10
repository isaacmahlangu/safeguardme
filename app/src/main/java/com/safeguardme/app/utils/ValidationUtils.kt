// utils/ValidationUtils.kt
package com.safeguardme.app.utils

import android.util.Patterns
import java.util.regex.Pattern

object ValidationUtils {

    private val PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$")
    private val STRONG_PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$"
    )

    /**
     * Validate email address
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                email.length <= 100 &&
                !email.contains("..") &&
                !email.startsWith(".") &&
                !email.endsWith(".")
    }

    /**
     * Validate phone number
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val cleaned = phone.replace(Regex("[^+0-9]"), "")
        return cleaned.isNotBlank() && PHONE_PATTERN.matcher(cleaned).matches()
    }

    /**
     * Validate password strength
     */
    fun isStrongPassword(password: String): Boolean {
        return password.length >= 8 &&
                password.length <= 128 &&
                STRONG_PASSWORD_PATTERN.matcher(password).matches()
    }

    /**
     * Get password strength score (0-4)
     */
    fun getPasswordStrength(password: String): Int {
        var score = 0

        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return score.coerceAtMost(4)
    }

    /**
     * Get password strength text
     */
    fun getPasswordStrengthText(score: Int): String {
        return when (score) {
            0, 1 -> "Very Weak"
            2 -> "Weak"
            3 -> "Medium"
            4 -> "Strong"
            else -> "Unknown"
        }
    }

    /**
     * Validate name (no numbers or special characters)
     */
    fun isValidName(name: String): Boolean {
        return name.isNotBlank() &&
                name.length >= 2 &&
                name.length <= 100 &&
                name.all { it.isLetter() || it.isWhitespace() || it == '\'' || it == '-' }
    }

    /**
     * Validate incident description
     */
    fun isValidIncidentDescription(description: String): Boolean {
        return description.trim().length >= 10 &&
                description.length <= 5000
    }

    /**
     * Validate location description
     */
    fun isValidLocation(location: String): Boolean {
        return location.trim().length >= 3 &&
                location.length <= 200
    }

    /**
     * Check if text contains potentially harmful content
     */
    fun containsHarmfulContent(text: String): Boolean {
        val harmfulPatterns = listOf(
            "<script",
            "javascript:",
            "vbscript:",
            "onload=",
            "onerror=",
            "<iframe",
            "<object",
            "<embed"
        )

        val lowerText = text.lowercase()
        return harmfulPatterns.any { pattern ->
            lowerText.contains(pattern)
        }
    }

    /**
     * Sanitize text input for safe storage
     */
    fun sanitizeText(text: String): String {
        return text
            .trim()
            .replace(Regex("[<>\"'&]"), "")
            .replace(Regex("\\s+"), " ")
    }
}