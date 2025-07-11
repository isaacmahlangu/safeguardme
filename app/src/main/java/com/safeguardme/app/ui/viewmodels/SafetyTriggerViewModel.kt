// ui/viewmodels/SafetyTriggerViewModel.kt
package com.safeguardme.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.data.models.User
import com.safeguardme.app.data.repositories.UserRepository
import com.safeguardme.app.utils.FirebaseUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafetyTriggerViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

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

    private val _safetyStatus = MutableStateFlow(SafetyStatus.DISABLED)
    val safetyStatus: StateFlow<SafetyStatus> = _safetyStatus.asStateFlow()



    // UI text based on safety status
    val statusMessage: StateFlow<String> = safetyStatus
        .map { status ->
            when (status) {
                SafetyStatus.DISABLED -> "Ready to protect you. Tap to activate safety monitoring."
                SafetyStatus.ENABLED -> "🟡 SAFETY MODE ACTIVE - Monitoring enabled, contacts notified."
                SafetyStatus.EMERGENCY -> "🔴 EMERGENCY ALERT ACTIVE - Help is on the way."
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    // Button text based on safety status
    val buttonText: StateFlow<String> = safetyStatus
        .map { status ->
            when (status) {
                SafetyStatus.DISABLED -> "Activate Safety"
                SafetyStatus.ENABLED -> "Safety Active"
                SafetyStatus.EMERGENCY -> "Emergency Mode"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    // Gesture trigger states
    private val _gestureTriggersEnabled = MutableStateFlow(true)
    val gestureTriggersEnabled: StateFlow<Boolean> = _gestureTriggersEnabled.asStateFlow()

    // Volume button trigger
    private val _volumeButtonTriggerEnabled = MutableStateFlow(true)
    val volumeButtonTriggerEnabled: StateFlow<Boolean> = _volumeButtonTriggerEnabled.asStateFlow()

    // Shake trigger
    private val _shakeTriggerEnabled = MutableStateFlow(true)
    val shakeTriggerEnabled: StateFlow<Boolean> = _shakeTriggerEnabled.asStateFlow()

    // Power button trigger (press 5 times rapidly)
    private val _powerButtonTriggerEnabled = MutableStateFlow(false)
    val powerButtonTriggerEnabled: StateFlow<Boolean> = _powerButtonTriggerEnabled.asStateFlow()

    val micStatusText: StateFlow<String> = gestureTriggersEnabled
        .map { enabled ->
            if (enabled) "Gesture triggers: ACTIVE" else "Gesture triggers: OFF"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Gesture triggers: ACTIVE")

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
                        // Update local safety status from user profile
                        user?.let { _safetyStatus.value = it.safetyStatus }
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
                val newStatus = when (_safetyStatus.value) {
                    SafetyStatus.DISABLED -> SafetyStatus.ENABLED
                    SafetyStatus.ENABLED -> SafetyStatus.DISABLED
                    SafetyStatus.EMERGENCY -> SafetyStatus.DISABLED
                }

                // Update local state immediately for responsive UI
                _safetyStatus.value = newStatus

                // Update backend
                userRepository.updateSafetyStatus(newStatus)
                    .onSuccess {
                        // Success - state already updated
                        when (newStatus) {
                            SafetyStatus.ENABLED -> triggerSafetyActivation()
                            SafetyStatus.EMERGENCY -> triggerEmergencyMode()
                            SafetyStatus.DISABLED -> triggerSafetyDeactivation()
                        }
                    }
                    .onFailure { error ->
                        // Revert local state on failure
                        _safetyStatus.value = when (newStatus) {
                            SafetyStatus.DISABLED -> SafetyStatus.ENABLED
                            SafetyStatus.ENABLED -> SafetyStatus.DISABLED
                            SafetyStatus.EMERGENCY -> SafetyStatus.ENABLED
                        }
                        handleError(error)
                    }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ NEW: Gesture-based triggers
    fun onVolumeButtonsPressed(consecutiveCount: Int) {
        if (!_volumeButtonTriggerEnabled.value) return

        if (consecutiveCount >= 3 && _safetyStatus.value == SafetyStatus.DISABLED) {
            triggerEmergencyGesture("Volume Buttons (3x)")
        }
    }

    fun onPhoneShakeDetected(intensity: Float) {
        if (!_shakeTriggerEnabled.value) return

        if (intensity > 15.0f && _safetyStatus.value == SafetyStatus.DISABLED) {
            triggerEmergencyGesture("Phone Shake")
        }
    }

    fun onPowerButtonPressed(consecutiveCount: Int) {
        if (!_powerButtonTriggerEnabled.value) return

        if (consecutiveCount >= 5 && _safetyStatus.value == SafetyStatus.DISABLED) {
            triggerEmergencyGesture("Power Button (5x)")
        }
    }

    fun onScreenTapPattern(pattern: List<Long>) {
        // Detect specific tap patterns like SOS (3 short, 3 long, 3 short)
        if (isSOSTapPattern(pattern) && _safetyStatus.value == SafetyStatus.DISABLED) {
            triggerEmergencyGesture("SOS Tap Pattern")
        }
    }

    private fun isSOSTapPattern(taps: List<Long>): Boolean {
        // SOS pattern: short-short-short-long-long-long-short-short-short
        // This is a simplified detection - could be enhanced
        return taps.size >= 9
    }

    private fun triggerEmergencyGesture(triggerType: String) {
        viewModelScope.launch {
            _safetyStatus.value = SafetyStatus.ENABLED

            userRepository.updateSafetyStatus(SafetyStatus.ENABLED)
                .onSuccess {
                    // Log gesture trigger for analytics
                    triggerSafetyActivation(triggerType)
                }
                .onFailure { error ->
                    _safetyStatus.value = SafetyStatus.DISABLED
                    handleError(error)
                }
        }
    }

    private fun triggerSafetyActivation(triggerMethod: String = "Manual") {
        // TODO: Implement safety activation logic
        // - Notify emergency contacts
        // - Start location sharing
        // - Enable audio recording (if permissions allow)
        // - Log activation method for analytics
    }

    private fun triggerEmergencyMode() {
        // TODO: Implement emergency mode logic
        // - Contact emergency services
        // - Send emergency SMS to contacts
        // - Enable continuous location tracking
        // - Start emergency audio recording
    }

    private fun triggerSafetyDeactivation() {
        // TODO: Implement deactivation logic
        // - Stop location sharing
        // - Notify contacts of safety
        // - Stop audio recording
    }

    fun escalateToEmergency() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _safetyStatus.value = SafetyStatus.EMERGENCY

                userRepository.updateSafetyStatus(SafetyStatus.EMERGENCY)
                    .onSuccess { triggerEmergencyMode() }
                    .onFailure { error ->
                        _safetyStatus.value = SafetyStatus.ENABLED
                        handleError(error)
                    }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Gesture trigger toggles
    fun toggleVolumeButtonTrigger() {
        _volumeButtonTriggerEnabled.value = !_volumeButtonTriggerEnabled.value
    }

    fun toggleShakeTrigger() {
        _shakeTriggerEnabled.value = !_shakeTriggerEnabled.value
    }

    fun togglePowerButtonTrigger() {
        _powerButtonTriggerEnabled.value = !_powerButtonTriggerEnabled.value
    }

    fun toggleGestureTriggers() {
        _gestureTriggersEnabled.value = !_gestureTriggersEnabled.value
    }

    // Confirmation dialog methods (keep for advanced features)
    fun confirmSafetyAction() {
        _showConfirmation.value = false
        onSafetyButtonPressed()
    }

    fun cancelConfirmation() {
        _showConfirmation.value = false
    }

    fun clearError() {
        _error.value = null
    }

    private fun handleError(throwable: Throwable) {
        _error.value = when (throwable) {
            is Exception -> FirebaseUtils.getErrorMessage(throwable)
            else -> "An unexpected error occurred"
        }
        _isLoading.value = false
    }
}