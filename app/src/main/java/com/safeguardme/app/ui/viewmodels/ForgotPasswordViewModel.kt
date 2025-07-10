// ui/viewmodels/ForgotPasswordViewModel.kt
package com.safeguardme.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.auth.AuthRepository
import com.safeguardme.app.utils.FirebaseUtils
import com.safeguardme.app.utils.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Form state
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    // UI states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isResetEmailSent = MutableStateFlow(false)
    val isResetEmailSent: StateFlow<Boolean> = _isResetEmailSent.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Security: Rate limiting for password reset attempts
    private var attemptCount = 0
    private var lastAttemptTime = 0L
    private val maxAttempts = 3 // Maximum 3 attempts per hour
    private val lockoutDurationMs = 3600000L // 1 hour lockout

    // Form validation
    val isFormValid: StateFlow<Boolean> = _email
        .map { email ->
            email.trim().isNotEmpty() && ValidationUtils.isValidEmail(email.trim())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun updateEmail(newEmail: String) {
        _email.value = newEmail.trim().take(100) // Limit email length
        _emailError.value = null
        _error.value = null
    }

    fun sendPasswordResetEmail() {
        if (!validateForm()) return
        if (isRateLimited()) return

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                authRepository.sendPasswordResetEmail(_email.value.trim())
                    .onSuccess {
                        _isResetEmailSent.value = true
                        _successMessage.value = "Password reset link sent to ${_email.value.trim()}. Please check your email."
                        this@ForgotPasswordViewModel.attemptCount = 0 // Reset on success
                    }
                    .onFailure { exception ->
                        handleResetFailure(exception)
                    }

            } catch (e: Exception) {
                handleResetFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resendResetEmail() {
        if (isRateLimited()) return

        // Reset state and send again
        _isResetEmailSent.value = false
        _successMessage.value = null
        sendPasswordResetEmail()
    }

    private fun validateForm(): Boolean {
        val email = _email.value.trim()

        when {
            email.isEmpty() -> {
                _emailError.value = "Email address is required"
                return false
            }
            !ValidationUtils.isValidEmail(email) -> {
                _emailError.value = "Please enter a valid email address"
                return false
            }
            email.length > 100 -> {
                _emailError.value = "Email address is too long"
                return false
            }
            else -> {
                _emailError.value = null
                return true
            }
        }
    }

    private fun isRateLimited(): Boolean {
        val currentTime = System.currentTimeMillis()

        if (attemptCount >= maxAttempts) {
            val timeSinceLastAttempt = currentTime - lastAttemptTime
            if (timeSinceLastAttempt < lockoutDurationMs) {
                val remainingMinutes = (lockoutDurationMs - timeSinceLastAttempt) / 60000L
                _error.value = "Too many password reset attempts. Please wait ${remainingMinutes + 1} minutes before trying again."
                return true
            } else {
                // Reset attempts after lockout period
                attemptCount = 0
            }
        }

        return false
    }

    private fun handleResetFailure(exception: Throwable) {
        attemptCount++
        lastAttemptTime = System.currentTimeMillis()

        // Always show success message to prevent email enumeration
        // but provide helpful general guidance
        _isResetEmailSent.value = true
        _successMessage.value = "If an account with this email exists, a password reset link has been sent. Please check your email and spam folder."
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun resetForm() {
        _email.value = ""
        _emailError.value = null
        _error.value = null
        _successMessage.value = null
        _isResetEmailSent.value = false
    }
}