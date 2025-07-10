package com.safeguardme.app.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // Input fields
    var email = mutableStateOf("")
    var password = mutableStateOf("")
    var confirmPassword = mutableStateOf("")
    var termsAccepted = mutableStateOf(false)

    // Loading and result states
    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)
    var registrationSuccess = mutableStateOf(false)

    // Validation error states
    var emailError = mutableStateOf<String?>(null)
    var passwordError = mutableStateOf<String?>(null)
    var confirmPasswordError = mutableStateOf<String?>(null)
    var termsError = mutableStateOf<String?>(null)

    // Password strength indicators
    var hasMinLength = mutableStateOf(false)
    var hasUppercase = mutableStateOf(false)
    var hasLowercase = mutableStateOf(false)
    var hasDigit = mutableStateOf(false)
    var hasSpecialChar = mutableStateOf(false)

    // Security: Rate limiting for registration attempts
    private var attemptCount = 0
    private var lastAttemptTime = 0L
    private val maxAttempts = 3 // Stricter than login
    private val lockoutDurationMs = 600000L // 10 minutes

    companion object {
        private val EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )

        private val SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]")
    }

    init {
        // Monitor password changes for real-time validation
        observePasswordChanges()
    }

    private fun observePasswordChanges() {
        // This would ideally use collectAsState, but for simplicity using direct observation
        // In production, consider using Flow-based validation
    }

    fun onRegisterClicked() {
        if (!validateAllInputs()) return
        if (isRateLimited()) return

        isLoading.value = true
        error.value = null
        clearValidationErrors()

        viewModelScope.launch {
            try {
                val result = repository.signUp(email.value.trim(), password.value)

                result.onSuccess {
                    // Send email verification
                    repository.sendEmailVerification().onSuccess {
                        registrationSuccess.value = true
                        attemptCount = 0 // Reset on success
                    }.onFailure { verificationError ->
                        // Registration succeeded but verification email failed
                        error.value = "Account created but verification email failed. Please try logging in."
                        registrationSuccess.value = true
                    }
                }.onFailure { exception ->
                    handleRegistrationFailure(exception)
                }
            } catch (e: Exception) {
                handleRegistrationFailure(e)
            } finally {
                isLoading.value = false
                // Security: Clear passwords on completion
                if (registrationSuccess.value) {
                    clearSensitiveData()
                }
            }
        }
    }

    private fun validateAllInputs(): Boolean {
        var isValid = true

        // Validate email
        isValid = validateEmail() && isValid

        // Validate password
        isValid = validatePassword() && isValid

        // Validate confirm password
        isValid = validateConfirmPassword() && isValid

        // Validate terms acceptance
        isValid = validateTermsAcceptance() && isValid

        return isValid
    }

    private fun validateEmail(): Boolean {
        return when {
            email.value.isBlank() -> {
                emailError.value = "Email is required"
                false
            }
            !EMAIL_PATTERN.matcher(email.value.trim()).matches() -> {
                emailError.value = "Please enter a valid email address"
                false
            }
            email.value.trim().length > 100 -> {
                emailError.value = "Email address is too long"
                false
            }
            else -> {
                emailError.value = null
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val pwd = password.value

        // Update password strength indicators
        hasMinLength.value = pwd.length >= 8
        hasUppercase.value = pwd.any { it.isUpperCase() }
        hasLowercase.value = pwd.any { it.isLowerCase() }
        hasDigit.value = pwd.any { it.isDigit() }
        hasSpecialChar.value = SPECIAL_CHAR_PATTERN.matcher(pwd).find()

        return when {
            pwd.isBlank() -> {
                passwordError.value = "Password is required"
                false
            }
            !hasMinLength.value -> {
                passwordError.value = "Password must be at least 8 characters long"
                false
            }
            !hasUppercase.value -> {
                passwordError.value = "Password must contain at least one uppercase letter"
                false
            }
            !hasLowercase.value -> {
                passwordError.value = "Password must contain at least one lowercase letter"
                false
            }
            !hasDigit.value -> {
                passwordError.value = "Password must contain at least one number"
                false
            }
            !hasSpecialChar.value -> {
                passwordError.value = "Password must contain at least one special character"
                false
            }
            pwd.length > 128 -> {
                passwordError.value = "Password is too long (maximum 128 characters)"
                false
            }
            isCommonPassword(pwd) -> {
                passwordError.value = "Please choose a less common password"
                false
            }
            else -> {
                passwordError.value = null
                true
            }
        }
    }

    private fun validateConfirmPassword(): Boolean {
        return when {
            confirmPassword.value.isBlank() -> {
                confirmPasswordError.value = "Please confirm your password"
                false
            }
            confirmPassword.value != password.value -> {
                confirmPasswordError.value = "Passwords do not match"
                false
            }
            else -> {
                confirmPasswordError.value = null
                true
            }
        }
    }

    private fun validateTermsAcceptance(): Boolean {
        return if (!termsAccepted.value) {
            termsError.value = "You must accept the terms to create an account"
            false
        } else {
            termsError.value = null
            true
        }
    }

    private fun isCommonPassword(password: String): Boolean {
        // Check against common passwords
        val commonPasswords = setOf(
            "password", "123456", "password123", "admin", "qwerty",
            "letmein", "welcome", "monkey", "dragon", "password1",
            "123456789", "welcome123", "admin123", "qwerty123"
        )
        return commonPasswords.contains(password.lowercase())
    }

    private fun isRateLimited(): Boolean {
        val currentTime = System.currentTimeMillis()

        if (attemptCount >= maxAttempts) {
            val timeSinceLastAttempt = currentTime - lastAttemptTime
            if (timeSinceLastAttempt < lockoutDurationMs) {
                val remainingMinutes = (lockoutDurationMs - timeSinceLastAttempt) / 60000L
                error.value = "Too many registration attempts. Please wait ${remainingMinutes + 1} minutes before trying again."
                return true
            } else {
                // Reset attempts after lockout period
                attemptCount = 0
            }
        }

        return false
    }

    private fun handleRegistrationFailure(exception: Throwable) {
        attemptCount++
        lastAttemptTime = System.currentTimeMillis()

        // Generic error messages to prevent information disclosure
        error.value = when {
            exception.message?.contains("email", ignoreCase = true) == true -> {
                "Unable to create account with this email address"
            }
            exception.message?.contains("network", ignoreCase = true) == true -> {
                "Network error. Please check your connection and try again"
            }
            attemptCount >= maxAttempts -> {
                "Registration temporarily disabled due to multiple attempts"
            }
            else -> {
                "Unable to create account. Please try again"
            }
        }

        // Security: Clear passwords on failure
        password.value = ""
        confirmPassword.value = ""
    }

    private fun clearValidationErrors() {
        emailError.value = null
        passwordError.value = null
        confirmPasswordError.value = null
        termsError.value = null
    }

    private fun clearSensitiveData() {
        password.value = ""
        confirmPassword.value = ""
        // Keep email for potential use in verification flow
    }

    fun clearError() {
        error.value = null
    }

    fun updateEmail(newEmail: String) {
        email.value = newEmail.trim()
        emailError.value = null
    }

    fun updatePassword(newPassword: String) {
        password.value = newPassword
        passwordError.value = null
        // Update password requirements
        updatePasswordRequirements(newPassword)
    }

    fun updateConfirmPassword(newConfirmPassword: String) {
        confirmPassword.value = newConfirmPassword
        confirmPasswordError.value = null
    }

    fun updateTermsAccepted(accepted: Boolean) {
        termsAccepted.value = accepted
        termsError.value = null
    }

    private fun updatePasswordRequirements(password: String) {
        hasMinLength.value = password.length >= 8
        hasUppercase.value = password.any { it.isUpperCase() }
        hasLowercase.value = password.any { it.isLowerCase() }
        hasDigit.value = password.any { it.isDigit() }
        hasSpecialChar.value = password.any { !it.isLetterOrDigit() }
    }

    // Security: Clear all sensitive data when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        clearSensitiveData()
        email.value = ""
    }
}