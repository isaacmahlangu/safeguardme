package com.safeguardme.app.auth

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    var email = mutableStateOf("")
    var password = mutableStateOf("")
    var isLoading = mutableStateOf(false)
    var error = mutableStateOf<String?>(null)
    var isAuthenticated = mutableStateOf(false)

    // Validation states
    var emailError = mutableStateOf<String?>(null)
    var passwordError = mutableStateOf<String?>(null)

    // Security features
    private var attemptCount = 0
    private var lastAttemptTime = 0L
    private val maxAttempts = 5
    private val lockoutDurationMs = 300000L // 5 minutes

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
    }

    fun onLoginClicked() {
        if (!validateInputs()) return
        if (isRateLimited()) return

        isLoading.value = true
        error.value = null
        clearValidationErrors()

        viewModelScope.launch {
            try {
                val result = repository.signIn(email.value.trim(), password.value)

                result.onSuccess {
                    isAuthenticated.value = true
                    attemptCount = 0 // Reset on success
                }.onFailure { exception ->
                    handleLoginFailure(exception)
                }
            } catch (e: Exception) {
                handleLoginFailure(e)
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Email validation
        when {
            email.value.isBlank() -> {
                emailError.value = "Email is required"
                isValid = false
            }
            !EMAIL_PATTERN.matcher(email.value.trim()).matches() -> {
                emailError.value = "Please enter a valid email address"
                isValid = false
            }
            else -> emailError.value = null
        }

        // Password validation
        when {
            password.value.isBlank() -> {
                passwordError.value = "Password is required"
                isValid = false
            }
            password.value.length < 6 -> {
                passwordError.value = "Password must be at least 6 characters"
                isValid = false
            }
            else -> passwordError.value = null
        }

        return isValid
    }

    private fun isRateLimited(): Boolean {
        val currentTime = System.currentTimeMillis()

        if (attemptCount >= maxAttempts) {
            val timeSinceLastAttempt = currentTime - lastAttemptTime
            if (timeSinceLastAttempt < lockoutDurationMs) {
                val remainingMinutes = (lockoutDurationMs - timeSinceLastAttempt) / 60000L
                error.value = "Too many login attempts. Please wait ${remainingMinutes + 1} minutes before trying again."
                return true
            } else {
                // Reset attempts after lockout period
                attemptCount = 0
            }
        }

        return false
    }

    private fun handleLoginFailure(exception: Throwable) {
        attemptCount++
        lastAttemptTime = System.currentTimeMillis()

        // Generic error message to prevent account enumeration
        error.value = when {
            attemptCount >= maxAttempts -> "Account temporarily locked due to multiple failed attempts"
            else -> "Invalid email or password. Please try again."
        }

        // Clear password for security
        password.value = ""
    }

    private fun clearValidationErrors() {
        emailError.value = null
        passwordError.value = null
    }

    fun clearError() {
        error.value = null
    }

    fun updateEmail(newEmail: String) {
        email.value = newEmail.trim()
        // Clear error when user starts typing
        emailError.value = null
    }

    fun updatePassword(newPassword: String) {
        password.value = newPassword
        // Clear error when user starts typing
        passwordError.value = null
    }

    // Security: Clear sensitive data when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        password.value = ""
        email.value = ""
    }
}