// receivers/EmergencyBroadcastReceiver.kt - System Events and Emergency Triggers
package com.safeguardme.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.data.repositories.UserRepository
import com.safeguardme.app.services.SafetyMonitoringService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EmergencyBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userRepository: UserRepository

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "EmergencyBroadcastReceiver"

        // Emergency gesture action names
        const val ACTION_VOLUME_BUTTON_EMERGENCY = "com.safeguardme.VOLUME_BUTTON_EMERGENCY"
        const val ACTION_POWER_BUTTON_EMERGENCY = "com.safeguardme.POWER_BUTTON_EMERGENCY"
        const val ACTION_SHAKE_EMERGENCY = "com.safeguardme.SHAKE_EMERGENCY"
        const val ACTION_PANIC_TRIGGER = "android.intent.action.PANIC_TRIGGER"

        // System actions
        const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
        const val ACTION_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED"

        // Custom SafeguardMe actions
        const val ACTION_EMERGENCY_TRIGGER = "com.safeguardme.EMERGENCY_TRIGGER"
        const val ACTION_SAFETY_CHECK = "com.safeguardme.SAFETY_CHECK"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action = intent.action ?: return
            Log.d(TAG, "📡 Broadcast received: $action")

            when (action) {
                // System events
                ACTION_BOOT_COMPLETED -> handleBootCompleted(context, intent)
                ACTION_PACKAGE_REPLACED -> handlePackageReplaced(context, intent)

                // Emergency gesture triggers
                ACTION_VOLUME_BUTTON_EMERGENCY -> handleVolumeButtonEmergency(context, intent)
                ACTION_POWER_BUTTON_EMERGENCY -> handlePowerButtonEmergency(context, intent)
                ACTION_SHAKE_EMERGENCY -> handleShakeEmergency(context, intent)
                ACTION_PANIC_TRIGGER -> handlePanicTrigger(context, intent)

                // Custom emergency actions
                ACTION_EMERGENCY_TRIGGER -> handleEmergencyTrigger(context, intent)
                ACTION_SAFETY_CHECK -> handleSafetyCheck(context, intent)

                else -> {
                    Log.d(TAG, "Unhandled broadcast action: $action")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling broadcast", e)
        }
    }

    private fun handleBootCompleted(context: Context, intent: Intent) {
        try {
            Log.i(TAG, "🚀 Device boot completed - initializing SafeguardMe")

            receiverScope.launch {
                try {
                    // Check if safety monitoring was active before reboot
                    val wasMonitoringActive = checkPreviousMonitoringState(context)

                    if (wasMonitoringActive) {
                        Log.w(TAG, "⚠️ Safety monitoring was active before reboot - considering restart")

                        // Get user preference for auto-restart after reboot
                        val shouldAutoRestart = getUserAutoRestartPreference(context)

                        if (shouldAutoRestart) {
                            Log.i(TAG, "🔄 Auto-restarting safety monitoring after reboot")
                            restartSafetyMonitoring(context)
                        } else {
                            Log.i(TAG, "📱 Sending notification about interrupted monitoring")
                            sendMonitoringInterruptedNotification(context)
                        }
                    }

                    // Initialize emergency gesture detection
                    initializeEmergencyGestures(context)

                    // Perform system health check
                    performSystemHealthCheck(context)

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in boot completed handler", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling boot completed", e)
        }
    }

    private fun handlePackageReplaced(context: Context, intent: Intent) {
        try {
            Log.i(TAG, "📦 SafeguardMe package updated - reinitializing")

            receiverScope.launch {
                try {
                    // Reinitialize after app update
                    initializeEmergencyGestures(context)

                    // Check if monitoring needs to be restored
                    val wasMonitoringActive = checkPreviousMonitoringState(context)
                    if (wasMonitoringActive) {
                        Log.i(TAG, "🔄 Restoring safety monitoring after update")
                        restartSafetyMonitoring(context)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in package replaced handler", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling package replaced", e)
        }
    }

    private fun handleVolumeButtonEmergency(context: Context, intent: Intent) {
        try {
            val consecutiveCount = intent.getIntExtra("consecutive_count", 0)
            Log.w(TAG, "🔊 Volume button emergency trigger - count: $consecutiveCount")

            if (consecutiveCount >= 3) {
                triggerEmergencyMode(context, "Volume Button Gesture (${consecutiveCount}x)", intent)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling volume button emergency", e)
        }
    }

    private fun handlePowerButtonEmergency(context: Context, intent: Intent) {
        try {
            val consecutiveCount = intent.getIntExtra("consecutive_count", 0)
            Log.w(TAG, "⚡ Power button emergency trigger - count: $consecutiveCount")

            if (consecutiveCount >= 5) {
                triggerEmergencyMode(context, "Power Button Gesture (${consecutiveCount}x)", intent)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling power button emergency", e)
        }
    }

    private fun handleShakeEmergency(context: Context, intent: Intent) {
        try {
            val intensity = intent.getFloatExtra("shake_intensity", 0f)
            val duration = intent.getLongExtra("shake_duration", 0L)
            Log.w(TAG, "📳 Shake emergency trigger - intensity: $intensity, duration: ${duration}ms")

            if (intensity > 15.0f && duration > 1500) { // Strong shake for >1.5 seconds
                triggerEmergencyMode(context, "Phone Shake Gesture (${intensity}g for ${duration}ms)", intent)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling shake emergency", e)
        }
    }

    private fun handlePanicTrigger(context: Context, intent: Intent) {
        try {
            Log.w(TAG, "🚨 Panic trigger received - activating emergency mode")

            // Panic trigger is a standard Android action for emergency situations
            triggerEmergencyMode(context, "System Panic Trigger", intent)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling panic trigger", e)
        }
    }

    private fun handleEmergencyTrigger(context: Context, intent: Intent) {
        try {
            val triggerSource = intent.getStringExtra("trigger_source") ?: "Unknown"
            val triggerData = intent.getStringExtra("trigger_data") ?: ""

            Log.w(TAG, "🚨 Custom emergency trigger - source: $triggerSource")

            triggerEmergencyMode(context, "Custom Trigger: $triggerSource", intent)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling emergency trigger", e)
        }
    }

    private fun handleSafetyCheck(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "🔍 Safety check requested")

            receiverScope.launch {
                try {
                    performSafetySystemCheck(context)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error performing safety check", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling safety check", e)
        }
    }

    // Private helper methods

    private suspend fun checkPreviousMonitoringState(context: Context): Boolean {
        return try {
            val preferences = context.getSharedPreferences("safety_monitoring_state", Context.MODE_PRIVATE)
            val wasActive = preferences.getBoolean("was_monitoring_active", false)
            val lastActiveTime = preferences.getLong("last_active_time", 0)

            // Consider monitoring was active if it was within the last hour
            val hourAgo = System.currentTimeMillis() - (60 * 60 * 1000)

            wasActive && lastActiveTime > hourAgo
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking previous monitoring state", e)
            false
        }
    }

    private suspend fun getUserAutoRestartPreference(context: Context): Boolean {
        return try {
            // Check user's preference for auto-restart after reboot
            val preferences = context.getSharedPreferences("safety_preferences", Context.MODE_PRIVATE)
            preferences.getBoolean("auto_restart_after_reboot", false)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting auto-restart preference", e)
            false
        }
    }

    private suspend fun restartSafetyMonitoring(context: Context) {
        try {
            // Update user safety status
            userRepository.updateSafetyStatus(SafetyStatus.ENABLED)

            // Start safety monitoring service
            SafetyMonitoringService.startMonitoring(context)

            Log.i(TAG, "✅ Safety monitoring restarted successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to restart safety monitoring", e)
        }
    }

    private fun sendMonitoringInterruptedNotification(context: Context) {
        try {
            // Create a notification to inform user that monitoring was interrupted
            val notificationIntent = Intent(context, SafetyMonitoringService::class.java)
            notificationIntent.action = "MONITORING_INTERRUPTED_NOTIFICATION"
            context.startService(notificationIntent)

            Log.d(TAG, "📱 Monitoring interrupted notification sent")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending interrupted notification", e)
        }
    }

    private fun initializeEmergencyGestures(context: Context) {
        try {
            // Initialize gesture detection systems
            val preferences = context.getSharedPreferences("gesture_settings", Context.MODE_PRIVATE)

            val volumeEnabled = preferences.getBoolean("volume_gesture_enabled", true)
            val shakeEnabled = preferences.getBoolean("shake_gesture_enabled", true)
            val powerEnabled = preferences.getBoolean("power_gesture_enabled", false)

            Log.d(TAG, "🎭 Emergency gestures initialized - Volume: $volumeEnabled, Shake: $shakeEnabled, Power: $powerEnabled")

            // Store initialization state
            preferences.edit()
                .putLong("last_gesture_init_time", System.currentTimeMillis())
                .apply()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing emergency gestures", e)
        }
    }

    private fun performSystemHealthCheck(context: Context) {
        try {
            val healthReport = SystemHealthChecker.performHealthCheck(context)

            Log.d(TAG, "🏥 System health check completed: ${healthReport.overallStatus}")

            // Store health check results
            val preferences = context.getSharedPreferences("system_health", Context.MODE_PRIVATE)
            preferences.edit()
                .putString("last_health_check", healthReport.toJson())
                .putLong("last_health_check_time", System.currentTimeMillis())
                .apply()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error performing system health check", e)
        }
    }

    private fun triggerEmergencyMode(context: Context, triggerMethod: String, intent: Intent) {
        try {
            Log.w(TAG, "🚨 TRIGGERING EMERGENCY MODE - Method: $triggerMethod")

            receiverScope.launch {
                try {
                    // Update user safety status to emergency
                    userRepository.updateSafetyStatus(SafetyStatus.EMERGENCY)

                    // Start or escalate safety monitoring
                    val monitoringIntent = Intent(context, SafetyMonitoringService::class.java)
                    monitoringIntent.action = SafetyMonitoringService.ACTION_EMERGENCY_ESCALATION
                    monitoringIntent.putExtra("trigger_method", triggerMethod)
                    monitoringIntent.putExtra("trigger_time", System.currentTimeMillis())

                    // Copy any additional data from the trigger intent
                    intent.extras?.let { extras ->
                        monitoringIntent.putExtras(extras)
                    }

                    context.startForegroundService(monitoringIntent)

                    // Log emergency trigger event
                    logEmergencyTriggerEvent(context, triggerMethod, intent)

                    Log.w(TAG, "🚨 Emergency mode activated successfully")

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to trigger emergency mode", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in emergency mode trigger", e)
        }
    }

    private suspend fun performSafetySystemCheck(context: Context) {
        try {
            val checkResults = mutableMapOf<String, Boolean>()

            // Check emergency contacts
            val hasEmergencyContacts = checkEmergencyContacts()
            checkResults["emergency_contacts"] = hasEmergencyContacts

            // Check permissions
            val hasRequiredPermissions = checkRequiredPermissions(context)
            checkResults["permissions"] = hasRequiredPermissions

            // Check device admin status
            val hasDeviceAdmin = SafetyDeviceAdminReceiver.isAdminEnabled(context)
            checkResults["device_admin"] = hasDeviceAdmin

            // Check system health
            val systemHealthy = checkSystemHealth(context)
            checkResults["system_health"] = systemHealthy

            val allChecksPass = checkResults.values.all { it }

            Log.i(TAG, "🔍 Safety system check completed - All checks pass: $allChecksPass")
            Log.d(TAG, "Check details: $checkResults")

            // Store check results
            val preferences = context.getSharedPreferences("safety_system_checks", Context.MODE_PRIVATE)
            preferences.edit()
                .putBoolean("last_check_all_pass", allChecksPass)
                .putLong("last_check_time", System.currentTimeMillis())
                .putString("last_check_details", checkResults.toString())
                .apply()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error performing safety system check", e)
        }
    }

    private suspend fun checkEmergencyContacts(): Boolean {
        return try {
            // This would typically check the emergency contact repository
            // For now, return true as a placeholder
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking emergency contacts", e)
            false
        }
    }

    private fun checkRequiredPermissions(context: Context): Boolean {
        return try {
            val requiredPermissions = listOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.CALL_PHONE
            )

            requiredPermissions.all { permission ->
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, permission
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking permissions", e)
            false
        }
    }

    private fun checkSystemHealth(context: Context): Boolean {
        return try {
            // Check available storage
            val availableSpace = context.filesDir.freeSpace
            val hasEnoughSpace = availableSpace > (100 * 1024 * 1024) // 100MB

            // Check network connectivity
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as android.net.ConnectivityManager
            val hasNetwork = connectivityManager.activeNetworkInfo?.isConnected == true

            hasEnoughSpace && hasNetwork
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking system health", e)
            false
        }
    }

    private fun logEmergencyTriggerEvent(context: Context, triggerMethod: String, intent: Intent) {
        try {
            val eventData = mapOf(
                "trigger_method" to triggerMethod,
                "timestamp" to System.currentTimeMillis(),
                "intent_action" to (intent.action ?: "unknown"),
                "intent_extras" to (intent.extras?.toString() ?: "none")
            )

            // Store emergency trigger event
            val preferences = context.getSharedPreferences("emergency_trigger_events", Context.MODE_PRIVATE)
            preferences.edit()
                .putString("last_trigger_event", eventData.toString())
                .putLong("last_trigger_time", System.currentTimeMillis())
                .apply()

            Log.w(TAG, "📝 Emergency trigger event logged: $triggerMethod")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error logging emergency trigger event", e)
        }
    }

    /**
     * System health checker utility
     */
    object SystemHealthChecker {

        fun performHealthCheck(context: Context): SystemHealthReport {
            val checks = mutableMapOf<String, Boolean>()

            try {
                // Storage check
                val availableSpace = context.filesDir.freeSpace
                checks["sufficient_storage"] = availableSpace > (100 * 1024 * 1024)

                // Network check
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                        as android.net.ConnectivityManager
                checks["network_available"] = connectivityManager.activeNetworkInfo?.isConnected == true

                // Battery check
                val batteryManager = context.getSystemService(Context.BATTERY_SERVICE)
                        as? android.os.BatteryManager
                val batteryLevel = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
                checks["sufficient_battery"] = batteryLevel > 20

                // Device admin check
                checks["device_admin_active"] = SafetyDeviceAdminReceiver.isAdminEnabled(context)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in health check", e)
            }

            val overallHealthy = checks.values.all { it }

            return SystemHealthReport(
                checks = checks,
                overallStatus = if (overallHealthy) "HEALTHY" else "NEEDS_ATTENTION",
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

/**
 * System health report data class
 */
data class SystemHealthReport(
    val checks: Map<String, Boolean>,
    val overallStatus: String,
    val timestamp: Long
) {
    fun toJson(): String {
        return """
            {
                "checks": ${checks.entries.joinToString(",", "{", "}") { "\"${it.key}\":${it.value}" }},
                "overall_status": "$overallStatus",
                "timestamp": $timestamp
            }
        """.trimIndent()
    }
}