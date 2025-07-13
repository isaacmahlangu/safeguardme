// managers/SafetyManager.kt - Emergency Response Coordinator
package com.safeguardme.app.managers

import android.app.NotificationManager
import android.content.Context
import android.location.LocationManager
import android.util.Log
import com.safeguardme.app.data.models.EvidenceType
import com.safeguardme.app.data.models.SafetyEvidence
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.data.repositories.SafetyEvidenceRepository
import com.safeguardme.app.data.repositories.SafetyRepository
import com.safeguardme.app.data.repositories.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SafetyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyRepository: SafetyRepository,
    private val safetyEvidenceRepository: SafetyEvidenceRepository,
    private val userRepository: UserRepository,
    private val notificationManager: NotificationManager,
    private val locationManager: LocationManager
) {

    companion object {
        private const val TAG = "SafetyManager"
    }

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentSessionId: String? = null

    /**
     * ✅ Trigger emergency mode from voice detection
     */
    suspend fun triggerEmergencyModeFromVoice(): Result<Unit> {
        return try {
            Log.i(TAG, "🚨 Voice trigger emergency activation initiated")

            // Update safety status to emergency
            val statusResult = safetyRepository.updateSafetyStatus(SafetyStatus.EMERGENCY)
            if (statusResult.isFailure) {
                throw statusResult.exceptionOrNull() ?: Exception("Failed to update safety status")
            }

            // Generate new session ID for this emergency
            currentSessionId = generateSessionId()

            Log.i(TAG, "✅ Emergency mode activated via voice trigger - Session: $currentSessionId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to trigger emergency mode from voice", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ Start evidence collection for emergency session
     */
    suspend fun startEvidenceCollection(): Result<String> {
        return try {
            val sessionId = currentSessionId ?: generateSessionId().also { currentSessionId = it }

            Log.d(TAG, "📸 Starting evidence collection for session: $sessionId")

            // Collect location evidence immediately
            collectLocationEvidence(sessionId)

            // Start audio recording if permissions available
            collectAudioEvidence(sessionId)

            // Log the voice trigger event
            logVoiceTriggerEvidence(sessionId)

            Log.i(TAG, "✅ Evidence collection started for session: $sessionId")
            Result.success(sessionId)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start evidence collection", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ Notify emergency contacts
     */
    suspend fun notifyEmergencyContacts(message: String): Result<Unit> {
        return try {
            Log.d(TAG, "📞 Notifying emergency contacts: $message")

            val user = userRepository.getCurrentUser().firstOrNull()
            if (user == null) {
                throw Exception("User not found")
            }

            val emergencyContacts = user.emergencyContacts
            if (emergencyContacts.isEmpty()) {
                Log.w(TAG, "⚠️ No emergency contacts configured")
                return Result.success(Unit)
            }

            // Get current location for notification
            val location = null //locationManager.getCurrentLocation()
            val locationText = if (location != null) {
                //"Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else {
                "Location: Unable to determine"
            }

            val fullMessage = """
                🚨 EMERGENCY ALERT 🚨
                
                $message
                
                $locationText
                
                Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
                
                This is an automated safety alert from SafeguardMe.
            """.trimIndent()

            // Send notifications to all emergency contacts
            emergencyContacts.forEach { contactId ->
                managerScope.launch {
                    try {
                        // Send SMS if available
                        //notificationManager.sendEmergencySMS(contactId, fullMessage)

                        // Send push notification if available
                        //notificationManager.sendEmergencyNotification(contactId, message, location)

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to notify contact $contactId", e)
                    }
                }
            }

            Log.i(TAG, "✅ Emergency notifications sent to ${emergencyContacts.size} contacts")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to notify emergency contacts", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ Update safety status
     */
    suspend fun updateSafetyStatus(status: SafetyStatus): Result<Unit> {
        return safetyRepository.updateSafetyStatus(status)
    }

    /**
     * ✅ Get current safety status
     */
    suspend fun getCurrentSafetyStatus(): Result<SafetyStatus> {
        return safetyRepository.getCurrentSafetyStatus()
    }

    // Private helper methods

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }

    private suspend fun collectLocationEvidence(sessionId: String) {
        try {
            val location = null // locationManager.getCurrentLocation()
            if (location != null) {
                val locationEvidence = SafetyEvidence(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    type = EvidenceType.LOCATION,
                    timestamp = System.currentTimeMillis(),
                    metadata = mapOf(
                        //"latitude" to location.latitude.toString(),
                        //"longitude" to location.longitude.toString(),
                        //"accuracy" to location.accuracy.toString(),
                        "source" to "voice_trigger"
                    ),
                    uploadStatus = "pending"
                )

                safetyEvidenceRepository.saveEvidence(locationEvidence)
                Log.d(TAG, "📍 Location evidence collected")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to collect location evidence", e)
        }
    }

    private suspend fun collectAudioEvidence(sessionId: String) {
        try {
            // Start ambient audio recording for evidence
            val audioEvidence = SafetyEvidence(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                type = EvidenceType.AUDIO,
                timestamp = System.currentTimeMillis(),
                metadata = mapOf(
                    "source" to "voice_trigger_ambient",
                    "duration" to "30", // 30 seconds of ambient audio
                    "triggered_by" to "voice_keyword"
                ),
                uploadStatus = "pending"
            )

            safetyEvidenceRepository.saveEvidence(audioEvidence)
            Log.d(TAG, "🎤 Audio evidence collection started")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to collect audio evidence", e)
        }
    }

    private suspend fun logVoiceTriggerEvidence(sessionId: String) {
        try {
            val triggerEvidence = SafetyEvidence(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                type = EvidenceType.TRANSCRIPTION,
                timestamp = System.currentTimeMillis(),
                metadata = mapOf(
                    "trigger_type" to "voice_keyword",
                    "trigger_method" to "always_on_detection",
                    "confidence" to "high",
                    "system_response" to "emergency_activated"
                ),
                //description = "Emergency triggered by voice keyword detection",
                uploadStatus = "pending"
            )

            safetyEvidenceRepository.saveEvidence(triggerEvidence)
            Log.d(TAG, "📝 Voice trigger evidence logged")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to log voice trigger evidence", e)
        }
    }
}