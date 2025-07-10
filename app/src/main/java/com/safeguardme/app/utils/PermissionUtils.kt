// utils/PermissionUtils.kt
package com.safeguardme.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionUtils {

    // Camera permissions
    const val CAMERA_PERMISSION = Manifest.permission.CAMERA

    // Storage permissions
    val STORAGE_PERMISSIONS = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // Location permissions
    val LOCATION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Audio recording permission
    const val RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO

    // Phone permissions (for emergency calling)
    const val CALL_PHONE_PERMISSION = Manifest.permission.CALL_PHONE

    // Vibration permission
    const val VIBRATE_PERMISSION = Manifest.permission.VIBRATE

    /**
     * Check if a single permission is granted
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if all permissions in array are granted
     */
    fun arePermissionsGranted(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { isPermissionGranted(context, it) }
    }

    /**
     * Check camera permission
     */
    fun isCameraPermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, CAMERA_PERMISSION)
    }

    /**
     * Check storage permissions
     */
    fun areStoragePermissionsGranted(context: Context): Boolean {
        return arePermissionsGranted(context, STORAGE_PERMISSIONS)
    }

    /**
     * Check location permissions
     */
    fun areLocationPermissionsGranted(context: Context): Boolean {
        return arePermissionsGranted(context, LOCATION_PERMISSIONS)
    }

    /**
     * Check if any location permission is granted
     */
    fun isAnyLocationPermissionGranted(context: Context): Boolean {
        return LOCATION_PERMISSIONS.any { isPermissionGranted(context, it) }
    }

    /**
     * Check audio recording permission
     */
    fun isAudioRecordingPermissionGranted(context: Context): Boolean {
        return isPermissionGranted(context, RECORD_AUDIO_PERMISSION)
    }

    /**
     * Get user-friendly permission rationale text
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            CAMERA_PERMISSION -> "SafeguardMe needs camera access to capture evidence photos. This helps document incidents securely."

            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES -> "SafeguardMe needs storage access to select photos as evidence. Your photos remain private and secure."

            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> "SafeguardMe needs location access to record incident locations and share your location with emergency contacts when needed."

            RECORD_AUDIO_PERMISSION -> "SafeguardMe needs microphone access for voice evidence recording and emergency keyword detection."

            CALL_PHONE_PERMISSION -> "SafeguardMe needs phone access to call emergency services when you're in danger."

            else -> "SafeguardMe needs this permission to provide safety features."
        }
    }

    /**
     * Get critical permission explanation for domestic violence context
     */
    fun getCriticalPermissionExplanation(permission: String): String {
        return when (permission) {
            CAMERA_PERMISSION -> "⚠️ Camera access is essential for documenting evidence that may be crucial for legal proceedings and your safety."

            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES -> "⚠️ Photo access allows you to add existing evidence to your secure incident reports."

            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> "⚠️ Location access enables emergency features that can alert your trusted contacts when you need help."

            RECORD_AUDIO_PERMISSION -> "⚠️ Microphone access enables voice evidence recording and emergency keyword detection for your protection."

            CALL_PHONE_PERMISSION -> "⚠️ Phone access allows the app to directly call emergency services in critical situations."

            else -> "⚠️ This permission is important for your safety and security features."
        }
    }
}