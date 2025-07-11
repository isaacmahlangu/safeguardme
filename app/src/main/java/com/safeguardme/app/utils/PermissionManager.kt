// utils/PermissionManager.kt
package com.safeguardme.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor() {

    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var pendingPermissionsCallback: ((PermissionResult) -> Unit)? = null

    // Essential vs Optional permissions
    private val essentialPermissions = listOf(
        AppPermission.LOCATION,
        AppPermission.CAMERA
    )

    private val optionalPermissions = listOf(
        AppPermission.AUDIO_RECORDING,
        AppPermission.PHONE_CALLS
    )

    fun initialize(activity: ComponentActivity) {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val result = processPermissionResults(permissions)
            updatePermissionsState(permissions)
            pendingPermissionsCallback?.invoke(result)
            pendingPermissionsCallback = null
        }

        // Initial permission check
        checkAllPermissions(activity)
    }

    fun checkAllPermissions(context: Context) {
        val currentPermissions = AppPermission.values().associate { permission ->
            permission.manifestPermission to isPermissionGranted(context, permission)
        }
        updatePermissionsState(currentPermissions)
    }

    fun requestPermissions(
        permissions: List<AppPermission>,
        callback: (PermissionResult) -> Unit
    ) {
        pendingPermissionsCallback = callback
        val permissionStrings = permissions.map { it.manifestPermission }.toTypedArray()
        permissionLauncher?.launch(permissionStrings)
    }

    fun requestEssentialPermissions(callback: (PermissionResult) -> Unit) {
        requestPermissions(essentialPermissions, callback)
    }

    fun requestOptionalPermissions(callback: (PermissionResult) -> Unit) {
        requestPermissions(optionalPermissions, callback)
    }

    fun isPermissionGranted(context: Context, permission: AppPermission): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission.manifestPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun areEssentialPermissionsGranted(context: Context): Boolean {
        return essentialPermissions.all { isPermissionGranted(context, it) }
    }

    fun getPermissionStatus(context: Context, permission: AppPermission): PermissionStatus {
        return when {
            isPermissionGranted(context, permission) -> PermissionStatus.GRANTED
            else -> PermissionStatus.DENIED
        }
    }

    private fun processPermissionResults(results: Map<String, Boolean>): PermissionResult {
        val granted = results.filter { it.value }.keys
        val denied = results.filter { !it.value }.keys

        return PermissionResult(
            granted = granted.toList(),
            denied = denied.toList(),
            allGranted = denied.isEmpty()
        )
    }

    private fun updatePermissionsState(permissions: Map<String, Boolean>) {
        _permissionsState.value = _permissionsState.value.copy(
            locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true,
            cameraGranted = permissions[Manifest.permission.CAMERA] == true,
            audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true,
            phoneGranted = permissions[Manifest.permission.CALL_PHONE] == true,
            lastChecked = System.currentTimeMillis()
        )
    }
}

// Data classes
data class PermissionsState(
    val locationGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val audioGranted: Boolean = false,
    val phoneGranted: Boolean = false,
    val lastChecked: Long = 0L
) {
    val essentialPermissionsGranted: Boolean
        get() = locationGranted && cameraGranted

    val allPermissionsGranted: Boolean
        get() = locationGranted && cameraGranted && audioGranted && phoneGranted

    val hasAnyPermissions: Boolean
        get() = locationGranted || cameraGranted || audioGranted || phoneGranted
}

data class PermissionResult(
    val granted: List<String>,
    val denied: List<String>,
    val allGranted: Boolean
)

enum class PermissionStatus {
    GRANTED,
    DENIED,
    NOT_REQUESTED
}

enum class AppPermission(
    val manifestPermission: String,
    val title: String,
    val description: String,
    val rationale: String,
    val criticalityLevel: CriticalityLevel
) {
    LOCATION(
        manifestPermission = Manifest.permission.ACCESS_FINE_LOCATION,
        title = "Location Access",
        description = "Allows automatic location tagging for incident reports",
        rationale = "This helps emergency responders locate you quickly during an emergency. Location data is encrypted and only shared when you trigger an emergency.",
        criticalityLevel = CriticalityLevel.ESSENTIAL
    ),

    CAMERA(
        manifestPermission = Manifest.permission.CAMERA,
        title = "Camera Access",
        description = "Enables photo and video evidence capture",
        rationale = "Visual evidence can be crucial for incident documentation. All media is encrypted and stored securely on your device.",
        criticalityLevel = CriticalityLevel.ESSENTIAL
    ),

    AUDIO_RECORDING(
        manifestPermission = Manifest.permission.RECORD_AUDIO,
        title = "Microphone Access",
        description = "Enables voice trigger and audio evidence recording",
        rationale = "Voice triggers allow hands-free emergency activation when you can't touch your phone. Audio evidence can provide crucial context for incidents.",
        criticalityLevel = CriticalityLevel.ENHANCED
    ),

    PHONE_CALLS(
        manifestPermission = Manifest.permission.CALL_PHONE,
        title = "Phone Access",
        description = "Enables direct emergency calling",
        rationale = "Allows the app to automatically call emergency services or your emergency contacts when needed, potentially saving critical seconds.",
        criticalityLevel = CriticalityLevel.ENHANCED
    )
}

enum class CriticalityLevel {
    ESSENTIAL,    // Required for core functionality
    ENHANCED,     // Improves user experience significantly
    OPTIONAL      // Nice to have features
}