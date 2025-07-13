// managers/EmergencyContactNotificationManager.kt - Emergency Contact Communication
package com.safeguardme.app.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.safeguardme.app.data.models.EmergencyContact
import com.safeguardme.app.data.repositories.EmergencyContactRepository
import com.safeguardme.app.data.repositories.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyContactNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val emergencyContactRepository: EmergencyContactRepository,
    private val userRepository: UserRepository,
    private val locationTrackingManager: LocationTrackingManager
) {
    companion object {
        private const val TAG = "EmergencyContactNotificationManager"
        private const val MAX_SMS_LENGTH = 160
        private const val LOCATION_UPDATE_INTERVAL_MS = 60000L // 1 minute
    }

    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isLocationSharingActive = false

    /**
     * Send safety mode activation notification to all emergency contacts
     */
    suspend fun sendSafetyModeActivationNotification(sessionId: String) {
        try {
            Log.i(TAG, "📱 Sending safety mode activation notifications")

            val contacts = getVerifiedEmergencyContacts()
            if (contacts.isEmpty()) {
                Log.w(TAG, "⚠️ No verified emergency contacts found")
                return
            }

            val user = userRepository.getCurrentUser().firstOrNull()
            val userName = user?.fullName ?: "SafeguardMe User"
            val location = locationTrackingManager.getCurrentLocation()

            val message = createSafetyActivationMessage(userName, sessionId, location)

            contacts.forEach { contact ->
                sendSMS(contact, message)
            }

            // Start periodic location sharing
            startPeriodicLocationSharing(sessionId, contacts)

            Log.i(TAG, "✅ Safety activation notifications sent to ${contacts.size} contacts")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending safety activation notifications", e)
        }
    }

    /**
     * Send safety mode deactivation notification
     */
    suspend fun sendSafetyModeDeactivationNotification(sessionId: String) {
        try {
            Log.i(TAG, "📱 Sending safety mode deactivation notifications")

            // Stop location sharing
            stopPeriodicLocationSharing()

            val contacts = getVerifiedEmergencyContacts()
            if (contacts.isEmpty()) {
                Log.w(TAG, "⚠️ No verified emergency contacts found for deactivation notification")
                return
            }

            val user = userRepository.getCurrentUser().firstOrNull()
            val userName = user?.fullName ?: "SafeguardMe User"
            val location = locationTrackingManager.getCurrentLocation()

            val message = createSafetyDeactivationMessage(userName, sessionId, location)

            contacts.forEach { contact ->
                sendSMS(contact, message)
            }

            Log.i(TAG, "✅ Safety deactivation notifications sent to ${contacts.size} contacts")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending safety deactivation notifications", e)
        }
    }

    /**
     * Send emergency alert to all emergency contacts
     */
    suspend fun sendEmergencyAlert(sessionId: String) {
        try {
            Log.w(TAG, "🚨 SENDING EMERGENCY ALERT")

            val contacts = getVerifiedEmergencyContacts()
            if (contacts.isEmpty()) {
                Log.e(TAG, "❌ CRITICAL: No emergency contacts available for emergency alert!")
                return
            }

            val user = userRepository.getCurrentUser().firstOrNull()
            val userName = user?.fullName ?: "SafeguardMe User"
            val location = locationTrackingManager.getCurrentLocation()

            val emergencyMessage = createEmergencyAlertMessage(userName, sessionId, location)

            // Send to all contacts immediately
            contacts.forEach { contact ->
                sendSMS(contact, emergencyMessage, isEmergency = true)
            }

            // Also send follow-up location updates more frequently
            startEmergencyLocationSharing(sessionId, contacts)

            Log.w(TAG, "🚨 EMERGENCY ALERTS SENT TO ${contacts.size} CONTACTS")

        } catch (e: Exception) {
            Log.e(TAG, "❌ CRITICAL ERROR sending emergency alerts", e)
        }
    }

    /**
     * Send distress alert when distress keywords are detected
     */
    suspend fun sendDistressAlert(transcription: String, keywords: List<String>) {
        try {
            Log.w(TAG, "🚨 Sending distress alert for detected keywords: $keywords")

            val contacts = getVerifiedEmergencyContacts()
            if (contacts.isEmpty()) {
                Log.w(TAG, "⚠️ No emergency contacts for distress alert")
                return
            }

            val user = userRepository.getCurrentUser().firstOrNull()
            val userName = user?.fullName ?: "SafeguardMe User"
            val location = locationTrackingManager.getCurrentLocation()

            val distressMessage = createDistressAlertMessage(userName, transcription, keywords, location)

            contacts.forEach { contact ->
                sendSMS(contact, distressMessage, isEmergency = true)
            }

            Log.w(TAG, "🚨 Distress alerts sent to ${contacts.size} contacts")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending distress alerts", e)
        }
    }

    /**
     * Send periodic location update
     */
    fun sendLocationUpdate(location: Location, sessionId: String) {
        if (!isLocationSharingActive) return

        notificationScope.launch {
            try {
                val contacts = getVerifiedEmergencyContacts()
                if (contacts.isEmpty()) return@launch

                val user = userRepository.getCurrentUser().firstOrNull()
                val userName = user?.fullName ?: "SafeguardMe User"

                val locationMessage = createLocationUpdateMessage(userName, location, sessionId)

                contacts.forEach { contact ->
                    sendSMS(contact, locationMessage)
                }

                Log.d(TAG, "📍 Location update sent to ${contacts.size} contacts")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending location update", e)
            }
        }
    }

    /**
     * Send system error alert
     */
    suspend fun sendSystemErrorAlert(errorMessage: String) {
        try {
            Log.e(TAG, "🚨 Sending system error alert")

            val contacts = getVerifiedEmergencyContacts()
            if (contacts.isEmpty()) return

            val user = userRepository.getCurrentUser().firstOrNull()
            val userName = user?.fullName ?: "SafeguardMe User"

            val message = createSystemErrorMessage(userName, errorMessage)

            contacts.forEach { contact ->
                sendSMS(contact, message, isEmergency = true)
            }

            Log.e(TAG, "🚨 System error alerts sent to ${contacts.size} contacts")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending system error alerts", e)
        }
    }

    // Private methods

    private suspend fun getVerifiedEmergencyContacts(): List<EmergencyContact> {
        return try {
            emergencyContactRepository.getEmergencyContacts()
                .getOrElse { emptyList() }
                .filter { it.isVerified && it.canReceiveEmergencyNotifications() }
                .sortedBy { it.priority }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting emergency contacts", e)
            emptyList()
        }
    }

    private fun sendSMS(contact: EmergencyContact, message: String, isEmergency: Boolean = false) {
        if (!hasSMSPermission()) {
            Log.e(TAG, "❌ SMS permission not granted")
            return
        }

        try {
            val smsManager = SmsManager.getDefault()
            val phoneNumber = contact.phoneNumber.replace(Regex("[^0-9+]"), "") // Clean phone number

            // Split long messages
            val parts = if (message.length > MAX_SMS_LENGTH) {
                smsManager.divideMessage(message)
            } else {
                arrayListOf(message)
            }

            if (parts.size == 1) {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            }

            val logLevel = if (isEmergency) "🚨" else "📱"
            Log.i(TAG, "$logLevel SMS sent to ${contact.name} (${phoneNumber}): ${message.take(50)}...")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send SMS to ${contact.name}", e)
        }
    }

    private fun startPeriodicLocationSharing(sessionId: String, contacts: List<EmergencyContact>) {
        if (isLocationSharingActive) {
            Log.w(TAG, "⚠️ Location sharing already active")
            return
        }

        Log.i(TAG, "📍 Starting periodic location sharing")
        isLocationSharingActive = true

        notificationScope.launch {
            try {
                while (isLocationSharingActive) {
                    delay(LOCATION_UPDATE_INTERVAL_MS)

                    if (isLocationSharingActive) {
                        val location = locationTrackingManager.getCurrentLocation()
                        location?.let { sendLocationUpdate(it, sessionId) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in periodic location sharing", e)
            }
        }
    }

    private fun startEmergencyLocationSharing(sessionId: String, contacts: List<EmergencyContact>) {
        Log.w(TAG, "🚨 Starting emergency location sharing (30s intervals)")
        isLocationSharingActive = true

        notificationScope.launch {
            try {
                repeat(10) { // Send 10 updates over 5 minutes
                    delay(30000) // 30 seconds

                    if (isLocationSharingActive) {
                        val location = locationTrackingManager.getCurrentLocation()
                        location?.let { sendLocationUpdate(it, sessionId) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in emergency location sharing", e)
            }
        }
    }

    private fun stopPeriodicLocationSharing() {
        if (!isLocationSharingActive) return

        Log.i(TAG, "🛑 Stopping periodic location sharing")
        isLocationSharingActive = false
    }

    private fun createSafetyActivationMessage(userName: String, sessionId: String, location: Location?): String {
        val timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        val locationText = location?.let {
            "Location: ${locationTrackingManager.getGoogleMapsUrl(it)}"
        } ?: "Location: Not available"

        return """
🛡️ SAFETY MODE ACTIVATED
$userName has activated SafeguardMe safety monitoring.

Time: $timestamp
$locationText
Session: $sessionId

You will receive location updates. Reply SAFE if they contact you.
        """.trimIndent()
    }

    private fun createSafetyDeactivationMessage(userName: String, sessionId: String, location: Location?): String {
        val timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        val locationText = location?.let {
            "Final location: ${locationTrackingManager.getGoogleMapsUrl(it)}"
        } ?: "Location: Not available"

        return """
✅ SAFETY MODE DEACTIVATED
$userName has safely deactivated SafeguardMe monitoring.

Time: $timestamp
$locationText
Session: $sessionId

They have indicated they are safe.
        """.trimIndent()
    }

    private fun createEmergencyAlertMessage(userName: String, sessionId: String, location: Location?): String {
        val timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        val locationText = location?.let {
            "LOCATION: ${locationTrackingManager.getGoogleMapsUrl(it)}"
        } ?: "LOCATION: Not available"

        return """
🚨 EMERGENCY ALERT 🚨
$userName needs immediate help!

Time: $timestamp
$locationText
Session: $sessionId

PLEASE CHECK ON THEM OR CONTACT AUTHORITIES IF NEEDED!
        """.trimIndent()
    }

    private fun createDistressAlertMessage(userName: String, transcription: String, keywords: List<String>, location: Location?): String {
        val timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
        val locationText = location?.let {
            "Location: ${locationTrackingManager.getGoogleMapsUrl(it)}"
        } ?: "Location: Not available"

        return """
⚠️ DISTRESS DETECTED
$userName's phone detected distress keywords: ${keywords.joinToString(", ")}

Audio: "${transcription.take(100)}${if (transcription.length > 100) "..." else ""}"

Time: $timestamp
$locationText

Please check on them immediately!
        """.trimIndent()
    }

    private fun createLocationUpdateMessage(userName: String, location: Location, sessionId: String): String {
        val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val accuracy = "±${location.accuracy.toInt()}m"

        return """
📍 $userName Location Update
Time: $timestamp ($accuracy)
${locationTrackingManager.getGoogleMapsUrl(location)}
Session: $sessionId
        """.trimIndent()
    }

    private fun createSystemErrorMessage(userName: String, errorMessage: String): String {
        val timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())

        return """
⚠️ SYSTEM ERROR ALERT
$userName's SafeguardMe app encountered an error during safety monitoring.

Error: $errorMessage
Time: $timestamp

Please check on them to ensure they are safe.
        """.trimIndent()
    }

    private fun hasSMSPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Test emergency contact system
     */
    suspend fun testEmergencyContacts(): TestResult {
        return try {
            val contacts = getVerifiedEmergencyContacts()

            when {
                contacts.isEmpty() -> TestResult.NO_CONTACTS
                !hasSMSPermission() -> TestResult.NO_SMS_PERMISSION
                else -> {
                    val testMessage = "SafeguardMe test message - your emergency contact is working. Reply TEST to confirm."
                    contacts.take(1).forEach { contact -> // Test with first contact only
                        sendSMS(contact, testMessage)
                    }
                    TestResult.SUCCESS
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error testing emergency contacts", e)
            TestResult.ERROR
        }
    }

    /**
     * Get emergency contact status summary
     */
    suspend fun getEmergencyContactStatus(): EmergencyContactStatus {
        val contacts = getVerifiedEmergencyContacts()
        val hasPermission = hasSMSPermission()

        return EmergencyContactStatus(
            totalContacts = contacts.size,
            verifiedContacts = contacts.count { it.isVerified },
            hasRequiredPermissions = hasPermission,
            isReady = contacts.isNotEmpty() && hasPermission
        )
    }
}

/**
 * Test result for emergency contact system
 */
enum class TestResult {
    SUCCESS,
    NO_CONTACTS,
    NO_SMS_PERMISSION,
    ERROR
}

/**
 * Emergency contact system status
 */
data class EmergencyContactStatus(
    val totalContacts: Int,
    val verifiedContacts: Int,
    val hasRequiredPermissions: Boolean,
    val isReady: Boolean
) {
    fun getReadinessMessage(): String {
        return when {
            !hasRequiredPermissions -> "SMS permission required"
            totalContacts == 0 -> "No emergency contacts added"
            verifiedContacts == 0 -> "No verified emergency contacts"
            isReady -> "Emergency contacts ready (${verifiedContacts})"
            else -> "Emergency contacts not ready"
        }
    }
}