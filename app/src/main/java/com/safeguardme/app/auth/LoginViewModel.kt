package com.safeguardme.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Validation states
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()

    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()

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

        _isLoading.value = true
        _error.value = null
        clearValidationErrors()

        viewModelScope.launch {
            try {
                val result = repository.signIn(_email.value.trim(), _password.value)

                result.onSuccess {
                    _isAuthenticated.value = true
                    attemptCount = 0 // Reset on success
                }.onFailure { exception ->
                    handleLoginFailure(exception)
                }
            } catch (e: Exception) {
                handleLoginFailure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    private fun validateInputs(): Boolean {
        var isValid = true

        // Email validation
        when {
            _email.value.isBlank() -> {
                _emailError.value = "Email is required"
                isValid = false
            }
            !EMAIL_PATTERN.matcher(_email.value.trim()).matches() -> {
                _emailError.value = "Please enter a valid email address"
                isValid = false
            }
            else -> _emailError.value = null
        }

        // Password validation
        when {
            _password.value.isBlank() -> {
                _passwordError.value = "Password is required"
                isValid = false
            }
            _password.value.length < 6 -> {
                _passwordError.value = "Password must be at least 6 characters"
                isValid = false
            }
            else -> _passwordError.value = null
        }

        return isValid
    }

    private fun isRateLimited(): Boolean {
        val currentTime = System.currentTimeMillis()

        if (attemptCount >= maxAttempts) {
            val timeSinceLastAttempt = currentTime - lastAttemptTime
            if (timeSinceLastAttempt < lockoutDurationMs) {
                val remainingMinutes = (lockoutDurationMs - timeSinceLastAttempt) / 60000L
                _error.value = "Too many login attempts. Please wait ${remainingMinutes + 1} minutes before trying again."
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
        _error.value = when {
            attemptCount >= maxAttempts -> "Account temporarily locked due to multiple failed attempts"
            else -> "Invalid email or password. Please try again."
        }

        // Clear password for security
        _password.value = ""
    }

    private fun clearValidationErrors() {
        _emailError.value = null
        _passwordError.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun updateEmail(newEmail: String) {
        _email.value = newEmail.trim()
        // Clear error when user starts typing
        _emailError.value = null
    }

    fun updatePassword(newPassword: String) {
        _password.value = newPassword
        // Clear error when user starts typing
        _passwordError.value = null
    }

    // Reset authentication state (useful for logout)
    fun resetAuthenticationState() {
        _isAuthenticated.value = false
    }

    // Security: Clear sensitive data when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        _password.value = ""
        _email.value = ""
    }
}