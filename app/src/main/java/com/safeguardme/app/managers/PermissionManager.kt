// managers/PermissionManager.kt - Enhanced with SMS Support
package com.safeguardme.app.managers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.safeguardme.app.utils.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor() {

    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "PermissionManager"
    }

    // ✅ UNCHANGED: Individual permission states
    private val _locationGranted = MutableStateFlow(false)
    val locationGranted: StateFlow<Boolean> = _locationGranted.asStateFlow()

    private val _cameraGranted = MutableStateFlow(false)
    val cameraGranted: StateFlow<Boolean> = _cameraGranted.asStateFlow()

    private val _audioGranted = MutableStateFlow(false)
    val audioGranted: StateFlow<Boolean> = _audioGranted.asStateFlow()

    private val _phoneGranted = MutableStateFlow(false)
    val phoneGranted: StateFlow<Boolean> = _phoneGranted.asStateFlow()

    // ✅ NEW: SMS permission state
    private val _smsGranted = MutableStateFlow(false)
    val smsGranted: StateFlow<Boolean> = _smsGranted.asStateFlow()

    private val _storageGranted = MutableStateFlow(false)
    val storageGranted: StateFlow<Boolean> = _storageGranted.asStateFlow()

    // ✅ UPDATED: Permissions state with SMS
    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()

    private var currentActivity: Activity? = null
    private var permissionLauncher: ((Array<String>) -> Unit)? = null

    /**
     * ✅ UNCHANGED: Initialize method
     */
    fun initialize(context: Context) {
        Log.d(TAG, "🔧 Initializing PermissionManager")
        updatePermissionStates(context)
    }

    /**
     * ✅ UNCHANGED: Set activity method
     */
    fun setActivity(activity: Activity) {
        Log.d(TAG, "🏠 Setting activity: ${activity::class.simpleName}")
        currentActivity = activity
        updatePermissionStates(activity)
    }

    /**
     * ✅ UNCHANGED: Set permission launcher
     */
    fun setPermissionLauncher(launcher: (Array<String>) -> Unit) {
        Log.d(TAG, "🚀 Setting permission launcher")
        permissionLauncher = launcher
    }

    /**
     * ✅ ENHANCED: Update permission states with SMS support
     */
    private fun updatePermissionStates(context: Context) {
        Log.d(TAG, "🔄 Updating all permission states")

        val oldLocationGranted = _locationGranted.value
        val oldCameraGranted = _cameraGranted.value
        val oldAudioGranted = _audioGranted.value
        val oldPhoneGranted = _phoneGranted.value
        val oldSmsGranted = _smsGranted.value // ✅ NEW
        val oldStorageGranted = _storageGranted.value

        // ✅ ENHANCED: More detailed permission checking with fallbacks
        _locationGranted.value = try {
            PermissionUtils.areLocationPermissionsGranted(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking location permissions", e)
            false
        }

        _cameraGranted.value = try {
            PermissionUtils.isCameraPermissionGranted(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking camera permission", e)
            false
        }

        _audioGranted.value = try {
            PermissionUtils.isAudioRecordingPermissionGranted(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking audio permission", e)
            false
        }

        _phoneGranted.value = try {
            PermissionUtils.isPermissionGranted(context, PermissionUtils.CALL_PHONE_PERMISSION)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking phone permission", e)
            false
        }

        // ✅ NEW: SMS permission checking
        _smsGranted.value = try {
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking SMS permission", e)
            false
        }

        _storageGranted.value = try {
            PermissionUtils.areStoragePermissionsGranted(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking storage permissions", e)
            false
        }

        // Enhanced logging for changes
        if (oldLocationGranted != _locationGranted.value) {
            Log.d(TAG, "📍 Location permission changed: $oldLocationGranted → ${_locationGranted.value}")
        }
        if (oldCameraGranted != _cameraGranted.value) {
            Log.d(TAG, "📷 Camera permission changed: $oldCameraGranted → ${_cameraGranted.value}")
        }
        if (oldAudioGranted != _audioGranted.value) {
            Log.d(TAG, "🎤 Audio permission changed: $oldAudioGranted → ${_audioGranted.value}")
        }
        if (oldPhoneGranted != _phoneGranted.value) {
            Log.d(TAG, "📞 Phone permission changed: $oldPhoneGranted → ${_phoneGranted.value}")
        }
        // ✅ NEW: SMS logging
        if (oldSmsGranted != _smsGranted.value) {
            Log.d(TAG, "💬 SMS permission changed: $oldSmsGranted → ${_smsGranted.value}")
        }
        if (oldStorageGranted != _storageGranted.value) {
            Log.d(TAG, "💾 Storage permission changed: $oldStorageGranted → ${_storageGranted.value}")
        }

        // ✅ UPDATED: Update combined state with SMS
        _permissionsState.value = PermissionsState(
            locationGranted = _locationGranted.value,
            cameraGranted = _cameraGranted.value,
            audioGranted = _audioGranted.value,
            phoneGranted = _phoneGranted.value,
            smsGranted = _smsGranted.value, // ✅ NEW
            storageGranted = _storageGranted.value,
            essentialPermissionsGranted = _locationGranted.value && _cameraGranted.value && _audioGranted.value && _phoneGranted.value && _smsGranted.value, // ✅ UPDATED
            allPermissionsGranted = _locationGranted.value && _cameraGranted.value &&
                    _audioGranted.value && _phoneGranted.value && _smsGranted.value && _storageGranted.value // ✅ UPDATED
        )

        Log.d(TAG, "📊 Permission state summary - Essential: ${_permissionsState.value.essentialPermissionsGranted}, All: ${_permissionsState.value.allPermissionsGranted}")
    }

    /**
     * ✅ ENHANCED: Request permission with SMS support
     */
    fun requestPermission(permission: AppPermission, onResult: (Boolean) -> Unit = {}) {
        val activity = currentActivity
        val launcher = permissionLauncher

        Log.d(TAG, "🎯 Requesting permission: ${permission.name}")

        if (activity == null) {
            Log.e(TAG, "❌ Activity not set - cannot request permissions")
            onResult(false)
            return
        }

        if (launcher == null) {
            Log.e(TAG, "❌ Permission launcher not set - cannot request permissions")
            onResult(false)
            return
        }

        // ✅ CRITICAL FIX: Use the androidPermissions from the enum directly
        val permissionsToRequest = permission.androidPermissions

        Log.d(TAG, "🔍 Checking permissions for ${permission.name}: ${permissionsToRequest.contentToString()}")

        // Check if already granted
        val alreadyGranted = permissionsToRequest.all { perm ->
            val granted = ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "   $perm: ${if (granted) "✅ GRANTED" else "❌ DENIED"}")
            granted
        }

        if (alreadyGranted) {
            Log.d(TAG, "✅ All permissions already granted for ${permission.name}")
            updateSpecificPermission(permission, true)
            onResult(true)
            return
        }

        // Store callback for this permission request
        pendingCallbacks[permission] = onResult

        Log.d(TAG, "🚀 Launching permission request dialog for ${permission.name}")

        try {
            launcher(permissionsToRequest)
            Log.d(TAG, "✅ Permission request launched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to launch permission request", e)
            pendingCallbacks.remove(permission)
            onResult(false)
        }
    }

    private val pendingCallbacks = mutableMapOf<AppPermission, (Boolean) -> Unit>()

    /**
     * ✅ ENHANCED: Permission result mapping with SMS support
     */
    fun onPermissionResult(permissions: Map<String, Boolean>) {
        Log.d(TAG, "🔄 Processing permission results: $permissions")

        permissions.forEach { (androidPermission, granted) ->
            Log.d(TAG, "📋 Permission $androidPermission: ${if (granted) "✅ GRANTED" else "❌ DENIED"}")

            // ✅ FIXED: Find which AppPermission this corresponds to
            val appPermission = findAppPermissionByAndroidPermission(androidPermission)
            if (appPermission != null) {
                Log.d(TAG, "🎯 Mapped to app permission: ${appPermission.name}")

                // ✅ CRITICAL: Update the specific permission state
                Log.d(TAG, "🎯 Updating ${appPermission.name} to $granted")
                updateSpecificPermission(appPermission, granted)

                // Execute and remove the pending callback
                pendingCallbacks[appPermission]?.let { callback ->
                    Log.d(TAG, "📞 Executing callback for ${appPermission.name}")
                    callback(granted)
                }
                pendingCallbacks.remove(appPermission)

                Log.d(TAG, "✅ Processed callback for ${appPermission.name}")
            } else {
                Log.w(TAG, "⚠️ Could not map Android permission $androidPermission to AppPermission")
            }
        }

        // Force refresh all states to ensure consistency
        currentActivity?.let {
            Log.d(TAG, "🔄 Refreshing all permission states after results")
            updatePermissionStates(it)
        }

        // ✅ UPDATED: Log final state for debugging with SMS
        Log.d(TAG, "🎯 Final permission states after processing:")
        Log.d(TAG, "   📍 Location: ${_locationGranted.value}")
        Log.d(TAG, "   📷 Camera: ${_cameraGranted.value}")
        Log.d(TAG, "   🎤 Audio: ${_audioGranted.value}")
        Log.d(TAG, "   📞 Phone: ${_phoneGranted.value}")
        Log.d(TAG, "   💬 SMS: ${_smsGranted.value}") // ✅ NEW
        Log.d(TAG, "   💾 Storage: ${_storageGranted.value}")
    }

    /**
     * ✅ CRITICAL FIX: Completely rewritten permission mapping logic
     */
    private fun findAppPermissionByAndroidPermission(androidPermission: String): AppPermission? {
        Log.d(TAG, "🔍 Mapping Android permission: $androidPermission")

        return AppPermission.values().find { appPerm ->
            val matches = appPerm.androidPermissions.contains(androidPermission)
            Log.d(TAG, "   Checking ${appPerm.name}: ${appPerm.androidPermissions.contentToString()} → $matches")
            matches
        }.also { result ->
            if (result != null) {
                Log.d(TAG, "✅ Successfully mapped $androidPermission → ${result.name}")
            } else {
                Log.w(TAG, "❌ Failed to map Android permission: $androidPermission")
                Log.w(TAG, "Available mappings:")
                AppPermission.values().forEach { perm ->
                    Log.w(TAG, "   ${perm.name}: ${perm.androidPermissions.contentToString()}")
                }
            }
        }
    }

    /**
     * ✅ ENHANCED: Update specific permission with SMS support
     */
    private fun updateSpecificPermission(permission: AppPermission, granted: Boolean) {
        Log.d(TAG, "🎯 Updating ${permission.name} from ${isPermissionGranted(permission)} to $granted")

        when (permission) {
            AppPermission.LOCATION -> _locationGranted.value = granted
            AppPermission.CAMERA -> _cameraGranted.value = granted
            AppPermission.AUDIO_RECORDING -> _audioGranted.value = granted
            AppPermission.PHONE_CALLS -> _phoneGranted.value = granted
            AppPermission.SMS_MESSAGING -> _smsGranted.value = granted // ✅ NEW
            AppPermission.STORAGE -> _storageGranted.value = granted
        }

        // ✅ UPDATED: Update combined state with SMS
        _permissionsState.value = PermissionsState(
            locationGranted = _locationGranted.value,
            cameraGranted = _cameraGranted.value,
            audioGranted = _audioGranted.value,
            phoneGranted = _phoneGranted.value,
            smsGranted = _smsGranted.value, // ✅ NEW
            storageGranted = _storageGranted.value,
            essentialPermissionsGranted = _locationGranted.value && _cameraGranted.value && _audioGranted.value && _phoneGranted.value && _smsGranted.value, // ✅ UPDATED
            allPermissionsGranted = _locationGranted.value && _cameraGranted.value &&
                    _audioGranted.value && _phoneGranted.value && _smsGranted.value && _storageGranted.value // ✅ UPDATED
        )

        Log.d(TAG, "✅ ${permission.name} updated successfully. New state: $granted")
    }

    /**
     * ✅ ENHANCED: Permission state getters with SMS
     */
    fun isPermissionGranted(permission: AppPermission): Boolean {
        return when (permission) {
            AppPermission.LOCATION -> _locationGranted.value
            AppPermission.CAMERA -> _cameraGranted.value
            AppPermission.AUDIO_RECORDING -> _audioGranted.value
            AppPermission.PHONE_CALLS -> _phoneGranted.value
            AppPermission.SMS_MESSAGING -> _smsGranted.value // ✅ NEW
            AppPermission.STORAGE -> _storageGranted.value
        }
    }

    /**
     * ✅ ENHANCED: Better rationale checking with error handling
     */
    fun shouldShowRationale(permission: AppPermission): Boolean {
        val activity = currentActivity ?: return false

        return try {
            permission.androidPermissions.any { perm ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking rationale for ${permission.name}", e)
            false
        }
    }

    /**
     * ✅ ENHANCED: Force refresh with error handling
     */
    fun refreshPermissions() {
        Log.d(TAG, "🔄 Force refreshing all permissions")
        try {
            currentActivity?.let { updatePermissionStates(it) }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error refreshing permissions", e)
        }
    }

    /**
     * ✅ ENHANCED: Debug method to log current permission states with SMS
     */
    fun debugLogPermissionStates() {
        Log.d(TAG, "🐛 DEBUG: Current permission states:")
        Log.d(TAG, "   📍 Location: ${_locationGranted.value}")
        Log.d(TAG, "   📷 Camera: ${_cameraGranted.value}")
        Log.d(TAG, "   🎤 Audio: ${_audioGranted.value}")
        Log.d(TAG, "   📞 Phone: ${_phoneGranted.value}")
        Log.d(TAG, "   💬 SMS: ${_smsGranted.value}") // ✅ NEW
        Log.d(TAG, "   💾 Storage: ${_storageGranted.value}")
        Log.d(TAG, "   ✅ Essential: ${_permissionsState.value.essentialPermissionsGranted}")
        Log.d(TAG, "   ✅ All: ${_permissionsState.value.allPermissionsGranted}")

        // Also log what Android thinks
        currentActivity?.let { activity ->
            Log.d(TAG, "🐛 DEBUG: Android system permission states:")
            AppPermission.values().forEach { perm ->
                perm.androidPermissions.forEach { androidPerm ->
                    val granted = ContextCompat.checkSelfPermission(activity, androidPerm) == PackageManager.PERMISSION_GRANTED
                    Log.d(TAG, "   $androidPerm: ${if (granted) "✅ GRANTED" else "❌ DENIED"}")
                }
            }
        }
    }
}

// ✅ UPDATED: Data classes with SMS support
data class PermissionsState(
    val locationGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val audioGranted: Boolean = false,
    val phoneGranted: Boolean = false,
    val smsGranted: Boolean = false, // ✅ NEW
    val storageGranted: Boolean = false,
    val essentialPermissionsGranted: Boolean = false,
    val allPermissionsGranted: Boolean = false
)

enum class AppPermission(
    val title: String,
    val description: String,
    val rationale: String,
    val criticalityLevel: CriticalityLevel,
    val androidPermissions: Array<String>
) {
    LOCATION(
        title = "Location Access",
        description = "Share your location with emergency contacts",
        rationale = "Location access allows SafeguardMe to automatically include your location in incident reports and share it with trusted contacts during emergencies. This helps responders find you quickly when you need help most.",
        criticalityLevel = CriticalityLevel.ESSENTIAL,
        androidPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ),

    CAMERA(
        title = "Camera Access",
        description = "Capture evidence photos and videos",
        rationale = "Camera access enables you to quickly document incidents with photos and videos. This evidence is encrypted and stored securely, and can be crucial for legal proceedings and your safety.",
        criticalityLevel = CriticalityLevel.ESSENTIAL,
        androidPermissions = arrayOf(Manifest.permission.CAMERA)
    ),

    AUDIO_RECORDING(
        title = "Microphone Access",
        description = "Record voice evidence and enable keyword detection",
        rationale = "Microphone access allows you to record voice evidence and set up emergency keyword detection. You can speak your secret keyword to trigger emergency alerts even when the app is not visible.",
        criticalityLevel = CriticalityLevel.ENHANCED,
        androidPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    ),

    PHONE_CALLS(
        title = "Phone Access",
        description = "Make emergency calls directly from the app",
        rationale = "Phone access enables SafeguardMe to directly call emergency services (911) and your emergency contacts when you're in danger. This can save critical seconds in emergency situations.",
        criticalityLevel = CriticalityLevel.ENHANCED,
        androidPermissions = arrayOf(Manifest.permission.CALL_PHONE)
    ),

    SMS_MESSAGING(
        title = "SMS Messaging",
        description = "Send emergency text messages to your contacts",
        rationale = "SMS messaging allows SafeguardMe to send emergency alerts to your trusted contacts via text message when data connections are poor or unavailable. SMS often works when other communication methods fail, making it a critical backup for emergency situations.",
        criticalityLevel = CriticalityLevel.ESSENTIAL,
        androidPermissions = arrayOf(Manifest.permission.SEND_SMS)
    ),

    STORAGE(
        title = "Photo Access",
        description = "Select photos from your gallery as evidence",
        rationale = "Photo access allows you to add existing photos from your gallery to incident reports. This is useful for documenting evidence that you may have captured before installing SafeguardMe.",
        criticalityLevel = CriticalityLevel.ENHANCED,
        androidPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    )
}

enum class CriticalityLevel {
    ESSENTIAL,   // Required for core safety features
    ENHANCED,    // Optional but highly recommended
    CONVENIENCE  // Nice to have features
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    NOT_REQUESTED
}