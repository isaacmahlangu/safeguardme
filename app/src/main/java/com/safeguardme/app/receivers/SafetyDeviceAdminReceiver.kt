// receivers/SafetyDeviceAdminReceiver.kt - Enhanced Security Through Device Administration
package com.safeguardme.app.receivers

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.safeguardme.app.services.SafetyMonitoringService

/**
 * Device Admin Receiver for enhanced security features
 * Provides additional security controls and tamper detection
 */
class SafetyDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "SafetyDeviceAdmin"

        /**
         * Get ComponentName for this receiver
         */
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context, SafetyDeviceAdminReceiver::class.java)
        }

        /**
         * Check if device admin is enabled
         */
        fun isAdminEnabled(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            return dpm.isAdminActive(getComponentName(context))
        }

        /**
         * Request device admin privileges
         */
        fun requestAdminPrivileges(context: Context): Intent {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, getComponentName(context))
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "SafeguardMe needs device admin privileges to provide enhanced security features during emergencies, including screen lock protection and tamper detection."
            )
            return intent
        }
    }

    /**
     * Called when device admin is enabled
     */
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "🔒 SafeguardMe device admin enabled")

        try {
            // Show confirmation to user
            Toast.makeText(
                context,
                "🔒 SafeguardMe security features enabled",
                Toast.LENGTH_LONG
            ).show()

            // Initialize security settings
            initializeSecuritySettings(context)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error enabling device admin", e)
        }
    }

    /**
     * Called when device admin is disabled
     */
    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "⚠️ SafeguardMe device admin disabled")

        try {
            // Show warning to user
            Toast.makeText(
                context,
                "⚠️ SafeguardMe security features disabled",
                Toast.LENGTH_LONG
            ).show()

            // Clean up security settings
            cleanupSecuritySettings(context)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error disabling device admin", e)
        }
    }

    /**
     * Called when password policy changes
     */
    override fun onPasswordChanged(context: Context, intent: Intent, user: android.os.UserHandle) {
        super.onPasswordChanged(context, intent, user)
        Log.d(TAG, "🔑 Device password changed")

        try {
            // Log security event
            logSecurityEvent(context, "PASSWORD_CHANGED")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling password change", e)
        }
    }

    /**
     * Called when password attempt fails
     */
    override fun onPasswordFailed(context: Context, intent: Intent, user: android.os.UserHandle) {
        super.onPasswordFailed(context, intent, user)
        Log.w(TAG, "⚠️ Device password attempt failed")

        try {
            // Log security event
            logSecurityEvent(context, "PASSWORD_FAILED")

            // Check if this could indicate device tampering during emergency
            handlePotentialTampering(context)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling password failure", e)
        }
    }

    /**
     * Called when password attempt succeeds
     */
    override fun onPasswordSucceeded(context: Context, intent: Intent, user: android.os.UserHandle) {
        super.onPasswordSucceeded(context, intent, user)
        Log.d(TAG, "✅ Device password succeeded")

        try {
            // Log security event
            logSecurityEvent(context, "PASSWORD_SUCCESS")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling password success", e)
        }
    }

    /**
     * Called when device lock task is starting
     */
    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.d(TAG, "🔒 Lock task mode entering for package: $pkg")

        try {
            logSecurityEvent(context, "LOCK_TASK_ENTERING", pkg)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling lock task mode entering", e)
        }
    }

    /**
     * Called when device lock task is exiting
     */
    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.d(TAG, "🔓 Lock task mode exiting")

        try {
            logSecurityEvent(context, "LOCK_TASK_EXITING")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling lock task mode exiting", e)
        }
    }

    // Private helper methods

    private fun initializeSecuritySettings(context: Context) {
        try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = getComponentName(context)

            // Set minimum password requirements for enhanced security
            dpm.setPasswordMinimumLength(adminComponent, 6)
            dpm.setPasswordQuality(adminComponent, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)

            // Set screen lock timeout for security
            dpm.setMaximumTimeToLock(adminComponent, 5 * 60 * 1000) // 5 minutes

            Log.d(TAG, "✅ Security settings initialized")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize security settings", e)
        }
    }

    private fun cleanupSecuritySettings(context: Context) {
        try {
            // Reset any policies we set
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = getComponentName(context)

            // Note: Some policies might require user action to reset
            Log.d(TAG, "🧹 Security settings cleanup initiated")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cleanup security settings", e)
        }
    }

    private fun logSecurityEvent(context: Context, eventType: String, details: String = "") {
        try {
            val timestamp = System.currentTimeMillis()
            Log.i(TAG, "🔐 Security Event: $eventType at $timestamp - $details")

            // Store security event for potential evidence
            val preferences = context.getSharedPreferences("safety_security_events", Context.MODE_PRIVATE)
            preferences.edit()
                .putString("last_security_event", "$eventType|$timestamp|$details")
                .putLong("last_security_event_time", timestamp)
                .apply()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to log security event", e)
        }
    }

    private fun handlePotentialTampering(context: Context) {
        try {
            // Check if safety monitoring is active
            val preferences = context.getSharedPreferences("safety_monitoring", Context.MODE_PRIVATE)
            val isMonitoringActive = preferences.getBoolean("is_monitoring_active", false)

            if (isMonitoringActive) {
                Log.w(TAG, "🚨 Potential device tampering detected during active safety monitoring")

                // Increment failed attempt counter
                val failedAttempts = preferences.getInt("failed_unlock_attempts", 0) + 1
                preferences.edit()
                    .putInt("failed_unlock_attempts", failedAttempts)
                    .putLong("last_failed_attempt", System.currentTimeMillis())
                    .apply()

                // If multiple failed attempts during monitoring, consider escalating
                if (failedAttempts >= 3) {
                    Log.w(TAG, "🚨 Multiple unlock failures during monitoring - potential tampering")

                    // Trigger additional security measures
                    triggerTamperResponse(context)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling potential tampering", e)
        }
    }

    private fun triggerTamperResponse(context: Context) {
        try {
            Log.w(TAG, "🚨 Triggering tamper response protocols")

            // Create tamper detection evidence
            val tamperEvent = createTamperEvidence(context)

            // If safety monitoring service is running, notify it
            val intent = Intent(context, SafetyMonitoringService::class.java)
            intent.action = "TAMPER_DETECTED"
            intent.putExtra("tamper_details", tamperEvent)
            context.startService(intent)

            Log.w(TAG, "🚨 Tamper response triggered successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to trigger tamper response", e)
        }
    }

    private fun createTamperEvidence(context: Context): String {
        return try {
            val preferences = context.getSharedPreferences("safety_security_events", Context.MODE_PRIVATE)
            val failedAttempts = preferences.getInt("failed_unlock_attempts", 0)
            val lastAttempt = preferences.getLong("last_failed_attempt", 0)

            """
            {
                "event_type": "TAMPER_DETECTION",
                "timestamp": ${System.currentTimeMillis()},
                "failed_attempts": $failedAttempts,
                "last_failed_attempt": $lastAttempt,
                "device_admin_active": true,
                "monitoring_active": true
            }
            """.trimIndent()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating tamper evidence", e)
            "{\"error\": \"Failed to create tamper evidence\"}"
        }
    }

    /**
     * Security utility methods
     */
    object SecurityUtils {

        /**
         * Force device lock (requires device admin)
         */
        fun lockDevice(context: Context): Boolean {
            return try {
                if (!isAdminEnabled(context)) {
                    Log.w(TAG, "⚠️ Cannot lock device - admin not enabled")
                    return false
                }

                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                dpm.lockNow()
                Log.i(TAG, "🔒 Device locked via admin policy")
                true

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to lock device", e)
                false
            }
        }

        /**
         * Check if device has secure lock screen
         */
        fun hasSecureLockScreen(context: Context): Boolean {
            return try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val adminComponent = getComponentName(context)

                if (isAdminEnabled(context)) {
                    dpm.isActivePasswordSufficient
                } else {
                    // Fallback check without admin privileges
                    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
                    keyguardManager.isKeyguardSecure
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error checking secure lock screen", e)
                false
            }
        }

        /**
         * Get security event history
         */
        fun getSecurityEventHistory(context: Context): List<String> {
            return try {
                val preferences = context.getSharedPreferences("safety_security_events", Context.MODE_PRIVATE)
                val events = mutableListOf<String>()

                // Get recent security events (simplified - in production you'd store a proper history)
                val lastEvent = preferences.getString("last_security_event", null)
                if (lastEvent != null) {
                    events.add(lastEvent)
                }

                events
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting security event history", e)
                emptyList()
            }
        }

        /**
         * Clear security event history (for privacy)
         */
        fun clearSecurityEventHistory(context: Context) {
            try {
                val preferences = context.getSharedPreferences("safety_security_events", Context.MODE_PRIVATE)
                preferences.edit().clear().apply()
                Log.d(TAG, "🧹 Security event history cleared")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error clearing security event history", e)
            }
        }
    }
}