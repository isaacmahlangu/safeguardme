// ui/viewmodels/SafetyTriggerViewModel.kt - Enhanced with Permission Management
package com.safeguardme.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.data.models.User
import com.safeguardme.app.data.repositories.SafetyEvidenceRepository
import com.safeguardme.app.data.repositories.UserRepository
import com.safeguardme.app.managers.AppPermission
import com.safeguardme.app.managers.EmergencyContactNotificationManager
import com.safeguardme.app.managers.PermissionManager
import com.safeguardme.app.services.SafetyMonitoringService
import com.safeguardme.app.utils.FirebaseUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafetyTriggerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val safetyEvidenceRepository: SafetyEvidenceRepository,
    private val emergencyContactNotificationManager: EmergencyContactNotificationManager,
    private val permissionManager: PermissionManager // ✅ NEW: Inject PermissionManager
) : ViewModel() {

    // ✅ NEW: Permission state tracking
    private val _permissionStatus = MutableStateFlow(SafetyPermissionStatus())
    val permissionStatus: StateFlow<SafetyPermissionStatus> = _permissionStatus.asStateFlow()

    private val _showPermissionRequest = MutableStateFlow<AppPermission?>(null)
    val showPermissionRequest: StateFlow<AppPermission?> = _showPermissionRequest.asStateFlow()

    private val _permissionWarnings = MutableStateFlow<List<String>>(emptyList())
    val permissionWarnings: StateFlow<List<String>> = _permissionWarnings.asStateFlow()

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

    private val _voiceDetectionEnabled = MutableStateFlow(false)
    val voiceDetectionEnabled = _voiceDetectionEnabled.asStateFlow()

    private val _triggerKeyword = MutableStateFlow<String?>(null)
    val triggerKeyword = _triggerKeyword.asStateFlow()

    // Safety monitoring state
    private val _isMonitoringActive = MutableStateFlow(false)
    val isMonitoringActive: StateFlow<Boolean> = _isMonitoringActive.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _monitoringStats = MutableStateFlow(MonitoringStats())
    val monitoringStats: StateFlow<MonitoringStats> = _monitoringStats.asStateFlow()

    val canActivateSafety: StateFlow<Boolean> = _permissionStatus.map { status ->
        status.hasCriticalPermissions()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canCollectFullEvidence: StateFlow<Boolean> = _permissionStatus.map { status ->
        status.hasAllOptimalPermissions()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ✅ ENHANCED: Permission-aware status messages
    val statusMessage: StateFlow<String> = combine(
        safetyStatus,
        _permissionStatus
    ) { status, permissions ->
        when (status) {
            SafetyStatus.DISABLED -> {
                if (permissions.hasCriticalPermissions()) {
                    "Ready to protect you. Tap to activate comprehensive safety monitoring."
                } else {
                    "Grant essential permissions to enable full safety protection."
                }
            }
            SafetyStatus.ENABLED -> {
                val capabilities = permissions.getActiveCapabilities()
                "🟡 SAFETY ACTIVE - Monitoring: ${capabilities.joinToString(", ")}"
            }
            SafetyStatus.EMERGENCY -> {
                "🔴 EMERGENCY ACTIVE - All available evidence being collected and contacts notified."
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    val buttonText: StateFlow<String> = combine(
        safetyStatus,
        _permissionStatus
    ) { status, permissions ->
        when (status) {
            SafetyStatus.DISABLED -> {
                if (permissions.hasCriticalPermissions()) {
                    "Activate Safety"
                } else {
                    "Grant Permissions"
                }
            }
            SafetyStatus.ENABLED -> "Safety Active"
            SafetyStatus.EMERGENCY -> "Emergency Mode"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    // ✅ ENHANCED: Permission-aware mic status
    val micStatusText: StateFlow<String> = combine(
        safetyStatus,
        _permissionStatus
    ) { status, permissions ->
        when (status) {
            SafetyStatus.DISABLED -> {
                if (permissions.canRecordAudio) {
                    "Monitoring: READY (Tap to activate)"
                } else {
                    "Monitoring: NEEDS MICROPHONE ACCESS"
                }
            }
            SafetyStatus.ENABLED -> {
                val active = mutableListOf<String>()
                if (permissions.canRecordAudio) active.add("Audio")
                if (permissions.canAccessLocation) active.add("Location")
                if (permissions.canTakePhotos) active.add("Photos")
                "🔴 RECORDING: ${active.joinToString(", ")}"
            }
            SafetyStatus.EMERGENCY -> "🚨 EMERGENCY: All systems active"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Monitoring: Checking...")

    // Gesture trigger states
    private val _gestureTriggersEnabled = MutableStateFlow(true)
    val gestureTriggersEnabled: StateFlow<Boolean> = _gestureTriggersEnabled.asStateFlow()

    private val _volumeButtonTriggerEnabled = MutableStateFlow(true)
    val volumeButtonTriggerEnabled: StateFlow<Boolean> = _volumeButtonTriggerEnabled.asStateFlow()

    private val _shakeTriggerEnabled = MutableStateFlow(true)
    val shakeTriggerEnabled: StateFlow<Boolean> = _shakeTriggerEnabled.asStateFlow()

    private val _powerButtonTriggerEnabled = MutableStateFlow(false)
    val powerButtonTriggerEnabled: StateFlow<Boolean> = _powerButtonTriggerEnabled.asStateFlow()

    init {
        initializePermissionMonitoring()
        loadUserData()
        observeMonitoringStats()
        checkAllPermissions()
    }

    /**
     * ✅ NEW: Initialize permission state monitoring
     */
    private fun initializePermissionMonitoring() {
        viewModelScope.launch {
            // Monitor permission changes from PermissionManager
            combine(
                permissionManager.audioGranted,
                permissionManager.locationGranted,
                permissionManager.cameraGranted,
                permissionManager.storageGranted
            ) { audio, location, camera, storage ->
                SafetyPermissionStatus(
                    canRecordAudio = audio,
                    canAccessLocation = location,
                    canTakePhotos = camera,
                    canSaveEvidence = storage,
                    lastChecked = System.currentTimeMillis()
                )
            }.collect { status ->
                _permissionStatus.value = status
                updatePermissionWarnings(status)

                // Log permission changes for debugging
                android.util.Log.d("SafetyTriggerVM", "🔍 Permissions updated: ${status.getSummary()}")
            }
        }
    }

    /**
     * ✅ NEW: Check all required permissions
     */
    fun checkAllPermissions() {
        viewModelScope.launch {
            try {
                val audioGranted = permissionManager.isPermissionGranted(AppPermission.AUDIO_RECORDING)
                val locationGranted = permissionManager.isPermissionGranted(AppPermission.LOCATION)
                val cameraGranted = permissionManager.isPermissionGranted(AppPermission.CAMERA)
                val storageGranted = permissionManager.isPermissionGranted(AppPermission.STORAGE)

                val status = SafetyPermissionStatus(
                    canRecordAudio = audioGranted,
                    canAccessLocation = locationGranted,
                    canTakePhotos = cameraGranted,
                    canSaveEvidence = storageGranted,
                    lastChecked = System.currentTimeMillis()
                )

                _permissionStatus.value = status
                updatePermissionWarnings(status)

                android.util.Log.d("SafetyTriggerVM", "🔍 Permission check complete:")
                android.util.Log.d("SafetyTriggerVM", "   🎤 Audio: $audioGranted")
                android.util.Log.d("SafetyTriggerVM", "   📍 Location: $locationGranted")
                android.util.Log.d("SafetyTriggerVM", "   📷 Camera: $cameraGranted")
                android.util.Log.d("SafetyTriggerVM", "   💾 Storage: $storageGranted")

            } catch (e: Exception) {
                android.util.Log.e("SafetyTriggerVM", "❌ Error checking permissions", e)
                _error.value = "Error checking permissions: ${e.message}"
            }
        }
    }

    /**
     * ✅ NEW: Update permission warnings
     */
    private fun updatePermissionWarnings(status: SafetyPermissionStatus) {
        val warnings = mutableListOf<String>()

        if (!status.canRecordAudio) {
            warnings.add("Voice evidence collection disabled - grant microphone access")
        }
        if (!status.canAccessLocation) {
            warnings.add("Location tracking disabled - grant location access")
        }
        if (!status.canTakePhotos) {
            warnings.add("Photo evidence disabled - grant camera access")
        }
        if (!status.canSaveEvidence) {
            warnings.add("Evidence storage limited - grant storage access")
        }

        _permissionWarnings.value = warnings
    }

    /**
     * ✅ NEW: Request specific permission
     */
    fun requestPermission(permission: AppPermission) {
        viewModelScope.launch {
            try {
                android.util.Log.d("SafetyTriggerVM", "🎯 Requesting permission: ${permission.name}")

                permissionManager.requestPermission(permission) { granted ->
                    android.util.Log.d("SafetyTriggerVM", "🎯 Permission ${permission.name} result: $granted")

                    if (granted) {
                        _error.value = null // Clear any previous errors
                        when (permission) {
                            AppPermission.AUDIO_RECORDING -> android.util.Log.d("SafetyTriggerVM", "✅ Audio evidence collection enabled")
                            AppPermission.LOCATION -> android.util.Log.d("SafetyTriggerVM", "✅ Location tracking enabled")
                            AppPermission.CAMERA -> android.util.Log.d("SafetyTriggerVM", "✅ Photo evidence enabled")
                            AppPermission.STORAGE -> android.util.Log.d("SafetyTriggerVM", "✅ Evidence storage enabled")
                            else -> {}
                        }
                    } else {
                        _error.value = when (permission) {
                            AppPermission.AUDIO_RECORDING -> "Voice evidence collection requires microphone access. Safety monitoring will work with limited capabilities."
                            AppPermission.LOCATION -> "Location tracking requires location access. Emergency contacts won't receive your location."
                            AppPermission.CAMERA -> "Photo evidence requires camera access. Visual documentation will be unavailable."
                            AppPermission.STORAGE -> "Evidence storage requires storage access. Evidence may not be saved permanently."
                            else -> "Permission denied. Some safety features may not work properly."
                        }
                    }

                    _showPermissionRequest.value = null
                    checkAllPermissions()
                }
            } catch (e: Exception) {
                android.util.Log.e("SafetyTriggerVM", "❌ Error requesting permission", e)
                _error.value = "Error requesting permission: ${e.message}"
                _showPermissionRequest.value = null
            }
        }
    }

    /**
     * ✅ NEW: Show permission request dialog
     */
    fun showPermissionDialog(permission: AppPermission) {
        _showPermissionRequest.value = permission
    }

    /**
     * ✅ NEW: Dismiss permission request dialog
     */
    fun dismissPermissionDialog() {
        _showPermissionRequest.value = null
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                userRepository.observeCurrentUserProfile()
                    .catch { e -> handleError(e) }
                    .collect { user ->
                        _user.value = user
                        user?.let {
                            _safetyStatus.value = it.safetyStatus
                            _isMonitoringActive.value = it.safetyStatus != SafetyStatus.DISABLED
                        }
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun observeMonitoringStats() {
        viewModelScope.launch {
            try {
                while (true) {
                    if (_isMonitoringActive.value && _currentSessionId.value != null) {
                        updateMonitoringStats()
                    }
                    kotlinx.coroutines.delay(5000)
                }
            } catch (e: Exception) {
                // Monitoring stats update failed, continue silently
            }
        }
    }

    /**
     * ✅ ENHANCED: Permission-aware safety button press
     */
    fun onSafetyButtonPressed() {
        // ✅ CRITICAL: Check permissions before proceeding
        val currentPermissions = _permissionStatus.value

        if (_safetyStatus.value == SafetyStatus.DISABLED) {
            // Check if we have minimum permissions to activate safety
            if (!currentPermissions.hasCriticalPermissions()) {
                android.util.Log.w("SafetyTriggerVM", "⚠️ Cannot activate safety - insufficient permissions")

                // Show the most critical missing permission
                val missingCritical = currentPermissions.getMostCriticalMissingPermission()
                if (missingCritical != null) {
                    _showPermissionRequest.value = missingCritical
                    return
                }
            }

            // Warn about missing optional permissions
            if (!currentPermissions.hasAllOptimalPermissions()) {
                val missingFeatures = currentPermissions.getMissingFeatures()
                android.util.Log.w("SafetyTriggerVM", "⚠️ Starting safety with limited features: missing ${missingFeatures.joinToString(", ")}")
            }
        }

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val newStatus = when (_safetyStatus.value) {
                    SafetyStatus.DISABLED -> {
                        // Check emergency contact readiness
                        /*val contactStatus = emergencyContactNotificationManager.getEmergencyContactStatus()
                        if (!contactStatus.isReady) {
                            _error.value = "Emergency contacts not ready: ${contactStatus.getReadinessMessage()}"
                            return@launch
                        }*/
                        SafetyStatus.ENABLED
                    }
                    SafetyStatus.ENABLED -> SafetyStatus.DISABLED
                    SafetyStatus.EMERGENCY -> SafetyStatus.DISABLED
                }

                _safetyStatus.value = newStatus

                userRepository.updateSafetyStatus(newStatus)
                    .onSuccess {
                        when (newStatus) {
                            SafetyStatus.ENABLED -> startSafetyMonitoring()
                            SafetyStatus.DISABLED -> stopSafetyMonitoring()
                            SafetyStatus.EMERGENCY -> escalateToEmergency()
                        }
                    }
                    .onFailure { error ->
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

    /**
     * ✅ ENHANCED: Permission-aware safety monitoring start
     */
    private fun startSafetyMonitoring() {
        try {
            val permissions = _permissionStatus.value
            val sessionId = generateSessionId()
            _currentSessionId.value = sessionId
            _isMonitoringActive.value = true

            // Start the safety monitoring service with permission context
            SafetyMonitoringService.startMonitoring(
                context = context
                //capabilities = permissions.getServiceCapabilities()
            )

            // Initialize monitoring stats with available capabilities
            _monitoringStats.value = MonitoringStats(
                sessionId = sessionId,
                startTime = System.currentTimeMillis(),
                availableCapabilities = permissions.getActiveCapabilities(),
                missingCapabilities = permissions.getMissingFeatures()
            )

            android.util.Log.i("SafetyTriggerVM", "🛡️ Started safety monitoring with capabilities: ${permissions.getActiveCapabilities()}")

            if (permissions.getMissingFeatures().isNotEmpty()) {
                android.util.Log.w("SafetyTriggerVM", "⚠️ Limited capabilities due to missing permissions: ${permissions.getMissingFeatures()}")
            }

        } catch (e: Exception) {
            android.util.Log.e("SafetyTriggerVM", "❌ Failed to start safety monitoring", e)
            handleError(e)
        }
    }

    /**
     * ✅ ENHANCED: Enhanced stop monitoring with permission context
     */
    private fun stopSafetyMonitoring() {
        try {
            val sessionId = _currentSessionId.value
            val permissions = _permissionStatus.value

            SafetyMonitoringService.stopMonitoring(context)

            _isMonitoringActive.value = false
            _currentSessionId.value = null

            android.util.Log.i("SafetyTriggerVM", "🛑 Stopped safety monitoring - Session: $sessionId")
            android.util.Log.i("SafetyTriggerVM", "   Final capabilities used: ${permissions.getActiveCapabilities()}")

            sessionId?.let {
                viewModelScope.launch {
                    createFinalSessionSummary(it)
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("SafetyTriggerVM", "❌ Failed to stop safety monitoring", e)
            handleError(e)
        }
    }

    /**
     * ✅ ENHANCED: Permission-aware emergency escalation
     */
    fun escalateToEmergency() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.w("SafetyTriggerVM", "🚨 ESCALATING TO EMERGENCY MODE")

                val permissions = _permissionStatus.value
                if (!permissions.hasCriticalPermissions()) {
                    android.util.Log.w("SafetyTriggerVM", "⚠️ Emergency escalation with limited permissions: ${permissions.getMissingFeatures()}")
                }

                _safetyStatus.value = SafetyStatus.EMERGENCY
                //availableCapabilities = permissions.getServiceCapabilities()
                userRepository.updateSafetyStatus(SafetyStatus.EMERGENCY)
                    .onSuccess {
                        SafetyMonitoringService.escalateToEmergency(
                            context = context

                        )

                        _monitoringStats.value = _monitoringStats.value.copy(
                            emergencyTriggered = true,
                            emergencyTime = System.currentTimeMillis()
                        )
                    }
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

    private suspend fun updateMonitoringStats() {
        try {
            val sessionId = _currentSessionId.value ?: return
            val currentStats = _monitoringStats.value

            val evidenceResult = safetyEvidenceRepository.getEvidenceForSession(sessionId)
            evidenceResult.onSuccess { evidenceList ->
                val newStats = currentStats.copy(
                    evidenceCount = evidenceList.size,
                    locationCount = evidenceList.count { it.type == com.safeguardme.app.data.models.EvidenceType.LOCATION },
                    photoCount = evidenceList.count { it.type == com.safeguardme.app.data.models.EvidenceType.PHOTO },
                    audioSegments = evidenceList.count { it.type == com.safeguardme.app.data.models.EvidenceType.AUDIO },
                    transcriptionCount = evidenceList.count { it.type == com.safeguardme.app.data.models.EvidenceType.TRANSCRIPTION },
                    lastUpdate = System.currentTimeMillis()
                )
                _monitoringStats.value = newStats
            }
        } catch (e: Exception) {
            android.util.Log.e("SafetyTriggerVM", "❌ Error updating monitoring stats", e)
        }
    }

    private suspend fun createFinalSessionSummary(sessionId: String) {
        try {
            safetyEvidenceRepository.createSessionSummary(sessionId)
                .onSuccess { session ->
                    android.util.Log.i("SafetyTriggerVM", "✅ Created session summary: ${session.evidenceCount} evidence items")
                }
                .onFailure { error ->
                    android.util.Log.e("SafetyTriggerVM", "❌ Failed to create session summary", error)
                }
        } catch (e: Exception) {
            android.util.Log.e("SafetyTriggerVM", "❌ Error creating session summary", e)
        }
    }

    // ✅ ENHANCED: Permission-aware gesture triggers
    fun onVolumeButtonsPressed(consecutiveCount: Int) {
        if (!_volumeButtonTriggerEnabled.value) return

        if (consecutiveCount >= 3 && _safetyStatus.value == SafetyStatus.DISABLED) {
            val permissions = _permissionStatus.value
            if (permissions.hasCriticalPermissions()) {
                triggerEmergencyGesture("Volume Buttons (3x)")
            } else {
                android.util.Log.w("SafetyTriggerVM", "⚠️ Volume gesture blocked - insufficient permissions")
                _error.value = "Grant essential permissions to enable gesture triggers"
            }
        }
    }

    fun onPhoneShakeDetected(intensity: Float) {
        if (!_shakeTriggerEnabled.value) return

        if (intensity > 15.0f && _safetyStatus.value == SafetyStatus.DISABLED) {
            val permissions = _permissionStatus.value
            if (permissions.hasCriticalPermissions()) {
                triggerEmergencyGesture("Phone Shake")
            } else {
                android.util.Log.w("SafetyTriggerVM", "⚠️ Shake gesture blocked - insufficient permissions")
                _error.value = "Grant essential permissions to enable gesture triggers"
            }
        }
    }

    fun onPowerButtonPressed(consecutiveCount: Int) {
        if (!_powerButtonTriggerEnabled.value) return

        if (consecutiveCount >= 5 && _safetyStatus.value == SafetyStatus.DISABLED) {
            val permissions = _permissionStatus.value
            if (permissions.hasCriticalPermissions()) {
                triggerEmergencyGesture("Power Button (5x)")
            } else {
                android.util.Log.w("SafetyTriggerVM", "⚠️ Power button gesture blocked - insufficient permissions")
                _error.value = "Grant essential permissions to enable gesture triggers"
            }
        }
    }

    fun onScreenTapPattern(pattern: List<Long>) {
        if (isSOSTapPattern(pattern) && _safetyStatus.value == SafetyStatus.DISABLED) {
            val permissions = _permissionStatus.value
            if (permissions.hasCriticalPermissions()) {
                triggerEmergencyGesture("SOS Tap Pattern")
            } else {
                android.util.Log.w("SafetyTriggerVM", "⚠️ SOS gesture blocked - insufficient permissions")
                _error.value = "Grant essential permissions to enable gesture triggers"
            }
        }
    }

    private fun isSOSTapPattern(taps: List<Long>): Boolean {
        return taps.size >= 9
    }

    private fun triggerEmergencyGesture(triggerType: String) {
        viewModelScope.launch {
            android.util.Log.w("SafetyTriggerVM", "🚨 Emergency gesture triggered: $triggerType")

            _safetyStatus.value = SafetyStatus.ENABLED

            userRepository.updateSafetyStatus(SafetyStatus.ENABLED)
                .onSuccess {
                    startSafetyMonitoring()
                    _monitoringStats.value = _monitoringStats.value.copy(
                        triggerMethod = triggerType
                    )
                }
                .onFailure { error ->
                    _safetyStatus.value = SafetyStatus.DISABLED
                    handleError(error)
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

    // Confirmation dialog methods
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

    /**
     * ✅ ENHANCED: Get monitoring summary with permission context
     */
    fun getCurrentMonitoringSummary(): String {
        val stats = _monitoringStats.value
        val permissions = _permissionStatus.value
        val duration = if (stats.startTime > 0) {
            (System.currentTimeMillis() - stats.startTime) / 1000 / 60
        } else 0

        return """
            Session: ${stats.sessionId ?: "None"}
            Duration: ${duration}m
            Evidence: ${stats.evidenceCount} items
            📍 Locations: ${stats.locationCount}
            📷 Photos: ${stats.photoCount}
            🎤 Audio: ${stats.audioSegments} segments
            📝 Transcriptions: ${stats.transcriptionCount}
            
            Capabilities: ${permissions.getActiveCapabilities().joinToString(", ")}
            ${if (permissions.getMissingFeatures().isNotEmpty()) "⚠️ Missing: ${permissions.getMissingFeatures().joinToString(", ")}" else ""}
            ${if (stats.emergencyTriggered) "🚨 Emergency Mode Active" else ""}
        """.trimIndent()
    }

    fun forceStopMonitoring() {
        viewModelScope.launch {
            try {
                _safetyStatus.value = SafetyStatus.DISABLED
                SafetyMonitoringService.stopMonitoring(context)
                _isMonitoringActive.value = false
                _currentSessionId.value = null

                userRepository.updateSafetyStatus(SafetyStatus.DISABLED)

                android.util.Log.w("SafetyTriggerVM", "🛑 Force stopped safety monitoring")

            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun generateSessionId(): String {
        return "safety_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    private fun handleError(throwable: Throwable) {
        _error.value = when (throwable) {
            is SecurityException -> "Permission denied: ${throwable.message}"
            is Exception -> FirebaseUtils.getErrorMessage(throwable)
            else -> "An unexpected error occurred"
        }
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        if (_isMonitoringActive.value) {
            SafetyMonitoringService.stopMonitoring(context)
        }
    }
}

/**
 * ✅ NEW: Safety-specific permission status
 */
data class SafetyPermissionStatus(
    val canRecordAudio: Boolean = false,
    val canAccessLocation: Boolean = false,
    val canTakePhotos: Boolean = false,
    val canSaveEvidence: Boolean = false,
    val canSendSMS: Boolean = false,
    val lastChecked: Long = 0L
) {
    fun hasCriticalPermissions(): Boolean {
        // At minimum we need audio OR location for meaningful safety monitoring
        return canRecordAudio || canAccessLocation
    }

    fun hasAllOptimalPermissions(): Boolean {
        return canRecordAudio && canAccessLocation && canTakePhotos && canSaveEvidence && canSendSMS
    }

    fun getMostCriticalMissingPermission(): AppPermission? {
        return when {
            !canRecordAudio -> AppPermission.AUDIO_RECORDING
            !canAccessLocation -> AppPermission.LOCATION
            !canTakePhotos -> AppPermission.CAMERA
            !canSaveEvidence -> AppPermission.STORAGE
            !canSendSMS -> AppPermission.SMS_MESSAGING
            else -> null
        }
    }

    fun getMissingPermissions(): List<AppPermission> {
        val missing = mutableListOf<AppPermission>()
        if (!canRecordAudio) missing.add(AppPermission.AUDIO_RECORDING)
        if (!canAccessLocation) missing.add(AppPermission.LOCATION)
        if (!canTakePhotos) missing.add(AppPermission.CAMERA)
        if (!canSaveEvidence) missing.add(AppPermission.STORAGE)
        if (!canSendSMS) missing.add(AppPermission.SMS_MESSAGING)
        return missing
    }

    fun getActiveCapabilities(): List<String> {
        val capabilities = mutableListOf<String>()
        if (canRecordAudio) capabilities.add("Voice Evidence")
        if (canAccessLocation) capabilities.add("Location Tracking")
        if (canTakePhotos) capabilities.add("Photo Evidence")
        if (canSaveEvidence) capabilities.add("Evidence Storage")
        if (canSendSMS) capabilities.add("SMS Messaging")
        return capabilities
    }

    fun getMissingFeatures(): List<String> {
        val missing = mutableListOf<String>()
        if (!canRecordAudio) missing.add("Voice Evidence")
        if (!canAccessLocation) missing.add("Location Tracking")
        if (!canTakePhotos) missing.add("Photo Evidence")
        if (!canSaveEvidence) missing.add("Evidence Storage")
        if (!canSendSMS) missing.add("SMS Messaging")
        return missing
    }

    fun getServiceCapabilities(): Map<String, Boolean> {
        return mapOf(
            "audio" to canRecordAudio,
            "location" to canAccessLocation,
            "camera" to canTakePhotos,
            "storage" to canSaveEvidence,
            "sms" to canSendSMS
        )
    }
    // canCanSaveEvidence
    fun getSummary(): String {
        val total = 4
        val granted = listOf(canRecordAudio, canAccessLocation, canTakePhotos).count { it }
        return "$granted/$total permissions granted"
    }
}

/**
 * ✅ ENHANCED: Enhanced monitoring stats with permission context
 */
data class MonitoringStats(
    val sessionId: String? = null,
    val startTime: Long = 0L,
    val evidenceCount: Int = 0,
    val locationCount: Int = 0,
    val photoCount: Int = 0,
    val audioSegments: Int = 0,
    val transcriptionCount: Int = 0,
    val emergencyTriggered: Boolean = false,
    val emergencyTime: Long? = null,
    val triggerMethod: String? = null,
    val lastUpdate: Long = 0L,
    val availableCapabilities: List<String> = emptyList(), // ✅ NEW
    val missingCapabilities: List<String> = emptyList()    // ✅ NEW
) {
    fun getDurationMinutes(): Long {
        return if (startTime > 0) {
            (System.currentTimeMillis() - startTime) / 1000 / 60
        } else 0
    }

    fun isActive(): Boolean = sessionId != null && startTime > 0

    fun getCapabilitySummary(): String {
        return if (availableCapabilities.isNotEmpty()) {
            "Active: ${availableCapabilities.joinToString(", ")}" +
                    if (missingCapabilities.isNotEmpty()) " | Missing: ${missingCapabilities.joinToString(", ")}" else ""
        } else "No capabilities available"
    }
}