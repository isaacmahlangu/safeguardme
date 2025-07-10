// ui/viewmodels/SafetyTriggerViewModel.kt
package com.safeguardme.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.data.models.User
import com.safeguardme.app.data.repositories.UserRepository
import com.safeguardme.app.utils.FirebaseUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafetyTriggerViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    init {
        loadUserWithProfileEnsurance()
    }

    private fun loadUserWithProfileEnsurance() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Ensure profile exists first
                userRepository.ensureUserProfile()
                    .onSuccess { user ->
                        _user.value = user
                        //safetyStatus.value = user.safetyStatus
                        //updateStatusMessages()
                    }
                    .onFailure { error ->
                        handleError(error)
                    }

                // Then start observing changes
                userRepository.observeCurrentUserProfile()
                    .catch { e -> handleError(e) }
                    .collect { user ->
                        if (user != null) {
                            _user.value = user
                            //safetyStatus.value = user.safetyStatus
                            //updateStatusMessages()
                        }
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }


    // User state
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // UI states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showConfirmation = MutableStateFlow(false)
    val showConfirmation: StateFlow<Boolean> = _showConfirmation.asStateFlow()

    private val _confirmationTimeout = MutableStateFlow(0)
    val confirmationTimeout: StateFlow<Int> = _confirmationTimeout.asStateFlow()

    // Safety status derived from user
    val safetyStatus: StateFlow<SafetyStatus> = _user
        .map { it?.safetyStatus ?: SafetyStatus.DISABLED }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SafetyStatus.DISABLED)

    // UI text based on safety status
    val statusMessage: StateFlow<String> = safetyStatus
        .map { status ->
            when (status) {
                SafetyStatus.DISABLED -> "Hi there, are you okay? Safety is our priority."
                SafetyStatus.ENABLED -> "Emergency mode is active. Your trusted contacts will be notified."
                SafetyStatus.EMERGENCY -> "EMERGENCY ALERT SENT. Help is on the way."
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    // Button text based on safety status
    val buttonText: StateFlow<String> = safetyStatus
        .map { status ->
            when (status) {
                SafetyStatus.DISABLED -> "Enable Safety"
                SafetyStatus.ENABLED -> "Safety Enabled"
                SafetyStatus.EMERGENCY -> "Emergency Active"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    // ASR status (Phase 1: always OFF)
    private val _asrEnabled = MutableStateFlow(false)
    val asrEnabled: StateFlow<Boolean> = _asrEnabled.asStateFlow()

    // Mic status text
    val micStatusText: StateFlow<String> = _asrEnabled
        .map { enabled ->
            if (enabled) "Keyword listener: ON" else "Keyword listener: OFF"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Keyword listener: OFF")

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                userRepository.observeCurrentUserProfile()
                    .catch { e -> handleError(e) }
                    .collect { user ->
                        _user.value = user
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun onSafetyButtonPressed() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Ensure profile exists before any updates
                userRepository.ensureUserProfile()
                    .onSuccess {
                        // Now safe to update safety status
                        val newStatus = when (safetyStatus.value) {
                            SafetyStatus.DISABLED -> SafetyStatus.ENABLED
                            SafetyStatus.ENABLED -> SafetyStatus.DISABLED
                            SafetyStatus.EMERGENCY -> SafetyStatus.DISABLED
                        }

                        updateSafetyStatus(newStatus)
                    }
                    .onFailure { error ->
                        handleError(error)
                    }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun updateSafetyStatus(newStatus: SafetyStatus) {
        userRepository.updateSafetyStatus(newStatus)
            .onSuccess {
                //safetyStatus.value = newStatus
                //updateStatusMessages()
                // Additional safety logic (notifications, etc.)
            }
            .onFailure { error ->
                handleError(error)
            }
    }

    fun confirmSafetyAction() {
        val currentStatus = safetyStatus.value

        when (currentStatus) {
            SafetyStatus.DISABLED -> enableSafety()
            SafetyStatus.ENABLED -> disableSafety()
            SafetyStatus.EMERGENCY -> disableSafety() // Emergency override
        }

        _showConfirmation.value = false
    }

    fun cancelConfirmation() {
        _showConfirmation.value = false
    }

    private fun enableSafety() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.updateSafetyStatus(SafetyStatus.ENABLED)
                    .onFailure { e -> handleError(e) }

                // TODO: Phase 2 - Trigger Cloud Function for contact notifications
                // triggerEmergencyNotifications()

            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun disableSafety() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.updateSafetyStatus(SafetyStatus.DISABLED)
                    .onFailure { e -> handleError(e) }

            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // TODO: Phase 2 - Emergency escalation
    fun escalateToEmergency() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.updateSafetyStatus(SafetyStatus.EMERGENCY)
                    .onFailure { e -> handleError(e) }

                // TODO: Trigger emergency services notification
                // triggerEmergencyServices()

            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // TODO: Phase 2 - ASR toggle
    fun toggleASR() {
        // Phase 1: No-op, always disabled
        // Phase 2: Implement ASR service toggle
    }

    private fun startConfirmationTimeout() {
        viewModelScope.launch {
            for (i in 10 downTo 1) {
                _confirmationTimeout.value = i
                delay(1000)
                if (!_showConfirmation.value) break
            }
            if (_showConfirmation.value) {
                _showConfirmation.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun handleError(throwable: Throwable) {
        _error.value = FirebaseUtils.getErrorMessage(throwable as Exception)
        _isLoading.value = false
    }
}