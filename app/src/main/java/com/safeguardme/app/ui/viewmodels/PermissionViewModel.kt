// ui/viewmodels/PermissionViewModel.kt - CRASH FIXED with Safe Initialization
package com.safeguardme.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.managers.AppPermission
import com.safeguardme.app.managers.PermissionManager
import com.safeguardme.app.ui.screens.PermissionStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {

    // ✅ UPDATED: Keep step management for future expansion
    private val _currentStep = MutableStateFlow(PermissionStep.INTRODUCTION)
    val currentStep: StateFlow<PermissionStep> = _currentStep.asStateFlow()

    private val _processingPermissions = MutableStateFlow<Set<AppPermission>>(emptySet())
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // ✅ CRITICAL FIX: Make permissionManager nullable and track initialization
    private var permissionManager: PermissionManager? = null
    private var context: Context? = null
    private var _isInitialized = false

    /**
     * ✅ NEW: Check if ViewModel is properly initialized
     */
    fun isInitialized(): Boolean = _isInitialized

    /**
     * ✅ NEW: Define essential permissions (all current permissions)
     */
    private val essentialPermissions = setOf(
        AppPermission.LOCATION,        // Critical for safety location sharing
        AppPermission.CAMERA,          // Critical for evidence capture
        AppPermission.AUDIO_RECORDING, // Critical for audio evidence
        AppPermission.PHONE_CALLS,     // Critical for emergency calling
        AppPermission.STORAGE          // Critical for saving evidence/data
    )

    /**
     * ✅ NEW: Define enhanced permissions (currently empty, ready for future expansion)
     */
    private val enhancedPermissions = setOf<AppPermission>(
        // Future permissions will go here, such as:
        // AppPermission.CONTACTS,        // Optional for easier emergency contact setup
        // AppPermission.CALENDAR,        // Optional for scheduling safety check-ins
        // AppPermission.NOTIFICATIONS,   // Optional for notification customization
        // AppPermission.BLUETOOTH,       // Optional for wearable device integration
    )

    /**
     * ✅ CRITICAL FIX: Safe initialization with proper null handling
     */
    fun initialize(permissionManager: PermissionManager, context: Context) {
        try {
            this.permissionManager = permissionManager
            this.context = context
            permissionManager.initialize(context)
            _isInitialized = true
            android.util.Log.d("PermissionViewModel", "✅ ViewModel initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("PermissionViewModel", "❌ ViewModel initialization failed", e)
            _isInitialized = false
        }
    }

    /**
     * ✅ UPDATED: Step progression (skip enhanced if empty)
     */
    fun nextStep() {
        _currentStep.value = when (_currentStep.value) {
            PermissionStep.INTRODUCTION -> PermissionStep.ESSENTIAL_PERMISSIONS
            PermissionStep.ESSENTIAL_PERMISSIONS -> {
                // ✅ NEW: Skip enhanced step if no enhanced permissions defined
                if (enhancedPermissions.isEmpty()) {
                    PermissionStep.ESSENTIAL_PERMISSIONS // Stay on essential
                } else {
                    PermissionStep.ENHANCED_PERMISSIONS
                }
            }
            PermissionStep.ENHANCED_PERMISSIONS -> PermissionStep.ENHANCED_PERMISSIONS
        }
    }

    /**
     * ✅ CRITICAL FIX: Safe permission request with initialization check
     */
    fun requestPermission(permission: AppPermission) {
        android.util.Log.d("PermissionViewModel", "🎯 Permission request received: ${permission.name}")

        if (!_isInitialized || permissionManager == null) {
            android.util.Log.e("PermissionViewModel", "❌ PermissionManager not initialized!")
            return
        }

        if (_processingPermissions.value.contains(permission)) {
            android.util.Log.w("PermissionViewModel", "⚠️ Permission ${permission.name} already being processed")
            return
        }

        viewModelScope.launch {
            android.util.Log.d("PermissionViewModel", "🚀 Starting permission request for ${permission.name}")

            _processingPermissions.value = _processingPermissions.value + permission
            _isProcessing.value = _processingPermissions.value.isNotEmpty()

            try {
                android.util.Log.d("PermissionViewModel", "📞 Calling permissionManager.requestPermission")

                permissionManager?.requestPermission(permission) { granted ->
                    android.util.Log.d("PermissionViewModel", "✅ Permission result for ${permission.name}: $granted")

                    _processingPermissions.value = _processingPermissions.value - permission
                    _isProcessing.value = _processingPermissions.value.isNotEmpty()

                    permissionManager?.refreshPermissions()
                    android.util.Log.d("PermissionViewModel", "🔄 Permission states refreshed")
                }
            } catch (e: Exception) {
                android.util.Log.e("PermissionViewModel", "❌ Error requesting permission ${permission.name}", e)
                _processingPermissions.value = _processingPermissions.value - permission
                _isProcessing.value = _processingPermissions.value.isNotEmpty()
            }
        }
    }

    /**
     * ✅ NEW: Get essential permissions list
     */
    fun getEssentialPermissions(): Set<AppPermission> = essentialPermissions

    /**
     * ✅ NEW: Get enhanced permissions list
     */
    fun getEnhancedPermissions(): Set<AppPermission> = enhancedPermissions

    /**
     * ✅ NEW: Check if permission is essential
     */
    fun isEssentialPermission(permission: AppPermission): Boolean {
        return essentialPermissions.contains(permission)
    }

    /**
     * ✅ NEW: Check if permission is enhanced
     */
    fun isEnhancedPermission(permission: AppPermission): Boolean {
        return enhancedPermissions.contains(permission)
    }

    /**
     * ✅ CRITICAL FIX: Safe essential permissions status with null check
     */
    fun getEssentialPermissionsStatus(): PermissionGroupStatus {
        if (!_isInitialized || permissionManager == null) {
            android.util.Log.w("PermissionViewModel", "⚠️ Getting essential permissions status before initialization")
            return PermissionGroupStatus(
                grantedCount = 0,
                totalCount = essentialPermissions.size,
                missingPermissions = essentialPermissions
            )
        }

        try {
            val grantedCount = essentialPermissions.count {
                permissionManager?.isPermissionGranted(it) ?: false
            }
            val totalCount = essentialPermissions.size

            return PermissionGroupStatus(
                grantedCount = grantedCount,
                totalCount = totalCount,
                missingPermissions = essentialPermissions.filter {
                    !(permissionManager?.isPermissionGranted(it) ?: false)
                }.toSet()
            )
        } catch (e: Exception) {
            android.util.Log.e("PermissionViewModel", "❌ Error getting essential permissions status", e)
            return PermissionGroupStatus(
                grantedCount = 0,
                totalCount = essentialPermissions.size,
                missingPermissions = essentialPermissions
            )
        }
    }

    /**
     * ✅ CRITICAL FIX: Safe enhanced permissions status with null check
     */
    fun getEnhancedPermissionsStatus(): PermissionGroupStatus {
        if (!_isInitialized || permissionManager == null) {
            android.util.Log.w("PermissionViewModel", "⚠️ Getting enhanced permissions status before initialization")
            return PermissionGroupStatus(
                grantedCount = 0,
                totalCount = enhancedPermissions.size,
                missingPermissions = enhancedPermissions
            )
        }

        try {
            val grantedCount = enhancedPermissions.count {
                permissionManager?.isPermissionGranted(it) ?: false
            }
            val totalCount = enhancedPermissions.size

            return PermissionGroupStatus(
                grantedCount = grantedCount,
                totalCount = totalCount,
                missingPermissions = enhancedPermissions.filter {
                    !(permissionManager?.isPermissionGranted(it) ?: false)
                }.toSet()
            )
        } catch (e: Exception) {
            android.util.Log.e("PermissionViewModel", "❌ Error getting enhanced permissions status", e)
            return PermissionGroupStatus(
                grantedCount = 0,
                totalCount = enhancedPermissions.size,
                missingPermissions = enhancedPermissions
            )
        }
    }

    /**
     * ✅ CRITICAL FIX: Safe permission processing check
     */
    fun isPermissionProcessing(permission: AppPermission): Boolean {
        return _processingPermissions.value.contains(permission)
    }

    /**
     * ✅ UNCHANGED: Reset to introduction step
     */
    fun resetToIntroduction() {
        _currentStep.value = PermissionStep.INTRODUCTION
        _processingPermissions.value = emptySet()
        _isProcessing.value = false
    }

    /**
     * ✅ UNCHANGED: Skip to specific step
     */
    fun skipToStep(step: PermissionStep) {
        _currentStep.value = step
    }

    /**
     * ✅ UPDATED: Get current step progress
     */
    fun getStepProgress(): Float {
        return when (_currentStep.value) {
            PermissionStep.INTRODUCTION -> 0.33f
            PermissionStep.ESSENTIAL_PERMISSIONS -> {
                if (enhancedPermissions.isEmpty()) 1.0f else 0.66f
            }
            PermissionStep.ENHANCED_PERMISSIONS -> 1.0f
        }
    }

    /**
     * ✅ CRITICAL FIX: Safe can proceed check
     */
    fun canProceedToNextStep(): Boolean {
        if (!_isInitialized) {
            android.util.Log.w("PermissionViewModel", "⚠️ Checking canProceedToNextStep before initialization")
            return false
        }

        return when (_currentStep.value) {
            PermissionStep.INTRODUCTION -> true

            PermissionStep.ESSENTIAL_PERMISSIONS -> {
                // ✅ CHANGED: Always allow proceeding, implement graceful degradation
                try {
                    val essentialStatus = getEssentialPermissionsStatus()

                    // Log what we have for debugging
                    android.util.Log.d("PermissionViewModel",
                        "✅ Graceful proceed: ${essentialStatus.grantedCount}/${essentialStatus.totalCount} permissions granted")

                    // Always return true - let users proceed with any permission state
                    true
                } catch (e: Exception) {
                    android.util.Log.e("PermissionViewModel", "❌ Error checking proceed status", e)
                    // Even on error, allow proceeding
                    true
                }
            }

            PermissionStep.ENHANCED_PERMISSIONS -> true
        }
    }

    fun getAppReadinessMessage(): String {
        if (!_isInitialized) {
            return "Initializing SafeguardMe..."
        }

        return try {
            val readinessLevel = getAppReadinessLevel()
            val availableFeatures = getAvailableFeatures()

            when (readinessLevel) {
                AppReadinessLevel.FULLY_READY ->
                    "SafeguardMe is fully ready with all ${availableFeatures.size} safety features enabled!"

                AppReadinessLevel.PARTIALLY_READY ->
                    "SafeguardMe is ready with ${availableFeatures.size} safety features. Grant more permissions to unlock additional features."

                AppReadinessLevel.BASIC_READY ->
                    "SafeguardMe is ready with basic safety features. Grant permissions to unlock advanced protection."

                AppReadinessLevel.INITIALIZING ->
                    "Setting up SafeguardMe..."
            }
        } catch (e: Exception) {
            android.util.Log.e("PermissionViewModel", "❌ Error getting readiness message", e)
            "SafeguardMe is ready to help keep you safe."
        }
    }

    fun getAppReadinessLevel(): AppReadinessLevel {
        if (!_isInitialized) {
            return AppReadinessLevel.INITIALIZING
        }

        return try {
            val essentialStatus = getEssentialPermissionsStatus()
            when {
                essentialStatus.isComplete() -> AppReadinessLevel.FULLY_READY
                essentialStatus.isPartial() -> AppReadinessLevel.PARTIALLY_READY
                else -> AppReadinessLevel.BASIC_READY
            }
        } catch (e: Exception) {
            android.util.Log.e("PermissionViewModel", "❌ Error getting readiness level", e)
            AppReadinessLevel.BASIC_READY
        }
    }

    fun getAvailableFeatures(): List<AppFeature> {
        if (!_isInitialized || permissionManager == null) {
            return listOf(AppFeature.BASIC_SAFETY_INFO)
        }

        val features = mutableListOf<AppFeature>()

        // Basic features always available
        features.add(AppFeature.BASIC_SAFETY_INFO)
        features.add(AppFeature.EMERGENCY_CONTACTS)
        features.add(AppFeature.SAFETY_TIPS)

        // Permission-dependent features
        if (permissionManager?.isPermissionGranted(AppPermission.LOCATION) == true) {
            features.add(AppFeature.LOCATION_SHARING)
            features.add(AppFeature.LOCATION_ALERTS)
        }

        if (permissionManager?.isPermissionGranted(AppPermission.CAMERA) == true) {
            features.add(AppFeature.PHOTO_EVIDENCE)
            features.add(AppFeature.VIDEO_EVIDENCE)
        }

        if (permissionManager?.isPermissionGranted(AppPermission.PHONE_CALLS) == true) {
            features.add(AppFeature.EMERGENCY_CALLING)
        }

        if (permissionManager?.isPermissionGranted(AppPermission.AUDIO_RECORDING) == true) {
            features.add(AppFeature.AUDIO_EVIDENCE)
            features.add(AppFeature.VOICE_ACTIVATION)
        }

        if (permissionManager?.isPermissionGranted(AppPermission.STORAGE) == true) {
            features.add(AppFeature.EVIDENCE_STORAGE)
            features.add(AppFeature.PHOTO_IMPORT)
        }

        return features
    }

    /**
     * ✅ NEW: Get missing features due to denied permissions
     */
    fun getMissingFeatures(): List<AppFeature> {
        if (!_isInitialized || permissionManager == null) {
            return emptyList()
        }

        val missingFeatures = mutableListOf<AppFeature>()

        if (permissionManager?.isPermissionGranted(AppPermission.LOCATION) != true) {
            missingFeatures.addAll(listOf(
                AppFeature.LOCATION_SHARING,
                AppFeature.LOCATION_ALERTS
            ))
        }

        if (permissionManager?.isPermissionGranted(AppPermission.CAMERA) != true) {
            missingFeatures.addAll(listOf(
                AppFeature.PHOTO_EVIDENCE,
                AppFeature.VIDEO_EVIDENCE
            ))
        }

        if (permissionManager?.isPermissionGranted(AppPermission.PHONE_CALLS) != true) {
            missingFeatures.add(AppFeature.EMERGENCY_CALLING)
        }

        if (permissionManager?.isPermissionGranted(AppPermission.AUDIO_RECORDING) != true) {
            missingFeatures.addAll(listOf(
                AppFeature.AUDIO_EVIDENCE,
                AppFeature.VOICE_ACTIVATION
            ))
        }

        if (permissionManager?.isPermissionGranted(AppPermission.STORAGE) != true) {
            missingFeatures.addAll(listOf(
                AppFeature.EVIDENCE_STORAGE,
                AppFeature.PHOTO_IMPORT
            ))
        }

        return missingFeatures
    }

    /**
     * ✅ CRITICAL FIX: Safe step completion status
     */
    fun getStepCompletionStatus(): StepCompletionStatus {
        if (!_isInitialized) {
            return StepCompletionStatus.INCOMPLETE
        }

        return try {
            when (_currentStep.value) {
                PermissionStep.INTRODUCTION -> StepCompletionStatus.COMPLETE

                PermissionStep.ESSENTIAL_PERMISSIONS -> {
                    val status = getEssentialPermissionsStatus()
                    when {
                        status.isComplete() -> StepCompletionStatus.COMPLETE
                        status.isPartial() -> StepCompletionStatus.PARTIAL
                        else -> StepCompletionStatus.INCOMPLETE
                    }
                }

                PermissionStep.ENHANCED_PERMISSIONS -> {
                    if (enhancedPermissions.isEmpty()) {
                        StepCompletionStatus.COMPLETE // No enhanced permissions defined
                    } else {
                        val status = getEnhancedPermissionsStatus()
                        when {
                            status.isComplete() -> StepCompletionStatus.COMPLETE
                            status.isPartial() -> StepCompletionStatus.PARTIAL
                            else -> StepCompletionStatus.INCOMPLETE
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PermissionViewModel", "❌ Error getting step completion status", e)
            StepCompletionStatus.INCOMPLETE
        }
    }

    /**
     * ✅ CRITICAL FIX: Safe app ready check
     */
    fun isAppReadyToUse(): Boolean {
        if (!_isInitialized) {
            return false
        }

        return try {
            getEssentialPermissionsStatus().isComplete()
        } catch (e: Exception) {
            android.util.Log.e("PermissionViewModel", "❌ Error checking if app ready", e)
            false
        }
    }

    /**
     * ✅ CRITICAL FIX: Safe safety readiness score
     */
    fun getSafetyReadinessScore(): Float {
        if (!_isInitialized) {
            return 0.0f
        }

        return try {
            val essentialStatus = getEssentialPermissionsStatus()
            val enhancedStatus = getEnhancedPermissionsStatus()

            // Essential permissions are 80% of the score, enhanced are 20%
            val essentialScore = if (essentialPermissions.isNotEmpty()) {
                essentialStatus.grantedCount.toFloat() / essentialStatus.totalCount.toFloat()
            } else 1.0f

            val enhancedScore = if (enhancedPermissions.isNotEmpty()) {
                enhancedStatus.grantedCount.toFloat() / enhancedStatus.totalCount.toFloat()
            } else 1.0f

            (essentialScore * 0.8f) + (enhancedScore * 0.2f)
        } catch (e: Exception) {
            android.util.Log.e("PermissionViewModel", "❌ Error calculating safety readiness score", e)
            0.0f
        }
    }

    /**
     * ✅ CRITICAL FIX: Safe missing permissions message
     */
    fun getMissingEssentialPermissionsMessage(): String? {
        if (!_isInitialized) {
            return "Initializing permissions..."
        }

        return try {
            val status = getEssentialPermissionsStatus()
            if (!status.isComplete()) {
                val missing = status.missingPermissions.map { permission ->
                    when (permission) {
                        AppPermission.LOCATION -> "Location (for safety alerts)"
                        AppPermission.CAMERA -> "Camera (for evidence capture)"
                        AppPermission.AUDIO_RECORDING -> "Audio Recording (for voice evidence)"
                        AppPermission.PHONE_CALLS -> "Phone Access (for emergency calling)"
                        AppPermission.STORAGE -> "Storage (for saving safety data)"
                        else -> permission.name
                    }
                }
                "Missing essential permissions: ${missing.joinToString(", ")}"
            } else null
        } catch (e: Exception) {
            android.util.Log.e("PermissionViewModel", "❌ Error getting missing permissions message", e)
            "Error checking permissions"
        }
    }

    override fun onCleared() {
        super.onCleared()
        _processingPermissions.value = emptySet()
        _isProcessing.value = false
        _isInitialized = false
        permissionManager = null
        context = null
    }
}

/**
 * ✅ NEW: Permission group status data class
 */
data class PermissionGroupStatus(
    val grantedCount: Int,
    val totalCount: Int,
    val missingPermissions: Set<AppPermission>
) {
    fun isComplete(): Boolean = grantedCount == totalCount
    fun isPartial(): Boolean = grantedCount > 0 && grantedCount < totalCount
    fun isEmpty(): Boolean = grantedCount == 0
    fun getCompletionPercentage(): Float = if (totalCount > 0) grantedCount.toFloat() / totalCount.toFloat() else 1.0f
}

/**
 * ✅ UNCHANGED: Step completion status for UI feedback
 */
enum class StepCompletionStatus {
    COMPLETE,    // All permissions granted
    PARTIAL,     // Some permissions granted
    INCOMPLETE   // No permissions granted
}

enum class AppReadinessLevel {
    INITIALIZING,        // Still setting up
    BASIC_READY,        // Core features available, no permissions
    PARTIALLY_READY,    // Some permissions granted, some features available
    FULLY_READY         // All permissions granted, all features available
}

enum class AppFeature(val displayName: String, val description: String) {
    // Always available
    BASIC_SAFETY_INFO("Safety Information", "Access safety tips and resources"),
    EMERGENCY_CONTACTS("Emergency Contacts", "Manage your emergency contact list"),
    SAFETY_TIPS("Safety Tips", "Learn personal safety strategies"),

    // Permission-dependent
    LOCATION_SHARING("Location Sharing", "Share your location in emergencies"),
    LOCATION_ALERTS("Location Alerts", "Get location-based safety alerts"),
    PHOTO_EVIDENCE("Photo Evidence", "Capture photos for incident reports"),
    VIDEO_EVIDENCE("Video Evidence", "Record videos for documentation"),
    EMERGENCY_CALLING("Emergency Calling", "One-tap emergency service calls"),
    AUDIO_EVIDENCE("Audio Evidence", "Record audio for incident reports"),
    VOICE_ACTIVATION("Voice Activation", "Activate app with voice commands"),
    EVIDENCE_STORAGE("Evidence Storage", "Securely store incident evidence"),
    PHOTO_IMPORT("Photo Import", "Import existing photos as evidence")
}