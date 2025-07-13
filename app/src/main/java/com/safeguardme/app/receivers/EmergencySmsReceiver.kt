// receivers/EmergencySmsReceiver.kt - Handle SMS Responses from Emergency Contacts
package com.safeguardme.app.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.safeguardme.app.data.repositories.EmergencyContactRepository
import com.safeguardme.app.data.repositories.SafetyEvidenceRepository
import com.safeguardme.app.data.models.SafetyEvidence
import com.safeguardme.app.data.models.EvidenceType
import com.safeguardme.app.services.SafetyMonitoringService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EmergencySmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var emergencyContactRepository: EmergencyContactRepository

    @Inject
    lateinit var safetyEvidenceRepository: SafetyEvidenceRepository

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "EmergencySmsReceiver"

        // Response keywords that emergency contacts might send
        private val SAFETY_CONFIRMATION_KEYWORDS = listOf(
            "safe", "ok", "okay", "fine", "good", "yes",
            "all good", "i'm safe", "im safe", "no problem"
        )

        private val EMERGENCY_KEYWORDS = listOf(
            "help", "emergency", "police", "911", "danger",
            "not safe", "unsafe", "trouble", "call police"
        )

        private val TEST_KEYWORDS = listOf(
            "test", "testing", "test message"
        )

        private val STOP_KEYWORDS = listOf(
            "stop", "unsubscribe", "opt out", "remove"
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "📱 SMS received - checking for emergency contact responses")

            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                return
            }

            val smsMessages = extractSmsMessages(intent)
            if (smsMessages.isEmpty()) {
                Log.d(TAG, "No valid SMS messages found")
                return
            }

            // Process each SMS message
            smsMessages.forEach { smsMessage ->
                processSmsMessage(context, smsMessage)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing SMS in receiver", e)
        }
    }

    private fun extractSmsMessages(intent: Intent): List<SmsMessage> {
        return try {
            val pdus = intent.extras?.get("pdus") as? Array<*>
            val format = intent.getStringExtra("format")

            pdus?.mapNotNull { pdu ->
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        SmsMessage.createFromPdu(pdu as ByteArray, format)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsMessage.createFromPdu(pdu as ByteArray)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to create SMS message from PDU", e)
                    null
                }
            } ?: emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extracting SMS messages", e)
            emptyList()
        }
    }

    private fun processSmsMessage(context: Context, smsMessage: SmsMessage) {
        receiverScope.launch {
            try {
                val senderNumber = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.displayMessageBody?.lowercase()?.trim() ?: ""
                val timestamp = smsMessage.timestampMillis

                Log.d(TAG, "📱 Processing SMS from $senderNumber: $messageBody")

                // Check if sender is an emergency contact
                val isEmergencyContact = isFromEmergencyContact(senderNumber)

                if (!isEmergencyContact) {
                    Log.d(TAG, "SMS not from emergency contact, ignoring")
                    return@launch
                }

                Log.i(TAG, "✅ SMS from verified emergency contact: $senderNumber")

                // Analyze message content and respond accordingly
                val responseType = analyzeMessageContent(messageBody)
                handleEmergencyContactResponse(context, senderNumber, messageBody, responseType, timestamp)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error processing SMS message", e)
            }
        }
    }

    private suspend fun isFromEmergencyContact(senderNumber: String): Boolean {
        return try {
            val contacts = emergencyContactRepository.getAllContacts().getOrElse { emptyList() }

            // Normalize phone numbers for comparison
            val normalizedSender = normalizePhoneNumber(senderNumber)

            contacts.any { contact ->
                val normalizedContact = normalizePhoneNumber(contact.phoneNumber)
                normalizedSender == normalizedContact
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking emergency contact", e)
            false
        }
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        // Remove all non-numeric characters except +
        val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")

        // Handle different formats
        return when {
            cleaned.startsWith("+1") -> cleaned.substring(2) // Remove +1 country code
            cleaned.startsWith("1") && cleaned.length == 11 -> cleaned.substring(1) // Remove leading 1
            else -> cleaned
        }
    }

    private fun analyzeMessageContent(messageBody: String): SmsResponseType {
        return when {
            SAFETY_CONFIRMATION_KEYWORDS.any { messageBody.contains(it) } -> SmsResponseType.SAFETY_CONFIRMATION
            EMERGENCY_KEYWORDS.any { messageBody.contains(it) } -> SmsResponseType.EMERGENCY_ESCALATION
            TEST_KEYWORDS.any { messageBody.contains(it) } -> SmsResponseType.TEST_RESPONSE
            STOP_KEYWORDS.any { messageBody.contains(it) } -> SmsResponseType.UNSUBSCRIBE_REQUEST
            else -> SmsResponseType.GENERAL_RESPONSE
        }
    }

    private suspend fun handleEmergencyContactResponse(
        context: Context,
        senderNumber: String,
        messageBody: String,
        responseType: SmsResponseType,
        timestamp: Long
    ) {
        try {
            Log.i(TAG, "🔄 Handling ${responseType.name} from $senderNumber")

            // Create evidence for the SMS response
            createSmsResponseEvidence(senderNumber, messageBody, responseType, timestamp)

            when (responseType) {
                SmsResponseType.SAFETY_CONFIRMATION -> {
                    handleSafetyConfirmation(context, senderNumber, messageBody)
                }

                SmsResponseType.EMERGENCY_ESCALATION -> {
                    handleEmergencyEscalation(context, senderNumber, messageBody)
                }

                SmsResponseType.TEST_RESPONSE -> {
                    handleTestResponse(context, senderNumber)
                }

                SmsResponseType.UNSUBSCRIBE_REQUEST -> {
                    handleUnsubscribeRequest(context, senderNumber)
                }

                SmsResponseType.GENERAL_RESPONSE -> {
                    handleGeneralResponse(context, senderNumber, messageBody)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling emergency contact response", e)
        }
    }

    private suspend fun createSmsResponseEvidence(
        senderNumber: String,
        messageBody: String,
        responseType: SmsResponseType,
        timestamp: Long
    ) {
        try {
            // Get current session ID if monitoring is active
            val sessionId = getCurrentSessionId() ?: "sms_response_${System.currentTimeMillis()}"

            val evidence = SafetyEvidence.createUserInputEvidence(
                sessionId = sessionId,
                inputType = "emergency_contact_sms_response",
                inputData = """
                    {
                        "sender": "$senderNumber",
                        "message": "$messageBody",
                        "response_type": "${responseType.name}",
                        "received_at": $timestamp,
                        "analysis": {
                            "is_safety_confirmation": ${responseType == SmsResponseType.SAFETY_CONFIRMATION},
                            "is_emergency": ${responseType == SmsResponseType.EMERGENCY_ESCALATION},
                            "is_test": ${responseType == SmsResponseType.TEST_RESPONSE}
                        }
                    }
                """.trimIndent(),
                timestamp = timestamp
            )

            safetyEvidenceRepository.saveEvidence(evidence)
            Log.d(TAG, "📝 SMS response evidence created")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating SMS response evidence", e)
        }
    }

    private fun handleSafetyConfirmation(context: Context, senderNumber: String, messageBody: String) {
        try {
            Log.i(TAG, "✅ Safety confirmation received from $senderNumber")

            // Store confirmation
            val preferences = context.getSharedPreferences("emergency_responses", Context.MODE_PRIVATE)
            preferences.edit()
                .putString("last_safety_confirmation", "$senderNumber|${System.currentTimeMillis()}|$messageBody")
                .putLong("last_safety_confirmation_time", System.currentTimeMillis())
                .apply()

            // If monitoring is active, this could indicate the user is now safe
            notifyMonitoringService(context, "SAFETY_CONFIRMATION", mapOf(
                "sender" to senderNumber,
                "message" to messageBody
            ))

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling safety confirmation", e)
        }
    }

    private fun handleEmergencyEscalation(context: Context, senderNumber: String, messageBody: String) {
        try {
            Log.w(TAG, "🚨 Emergency escalation requested by emergency contact: $senderNumber")

            // Store escalation request
            val preferences = context.getSharedPreferences("emergency_responses", Context.MODE_PRIVATE)
            preferences.edit()
                .putString("last_escalation_request", "$senderNumber|${System.currentTimeMillis()}|$messageBody")
                .putLong("last_escalation_time", System.currentTimeMillis())
                .apply()

            // Trigger emergency escalation
            notifyMonitoringService(context, "EMERGENCY_ESCALATION_REQUESTED", mapOf(
                "sender" to senderNumber,
                "message" to messageBody,
                "trigger_source" to "emergency_contact_sms"
            ))

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling emergency escalation", e)
        }
    }

    private fun handleTestResponse(context: Context, senderNumber: String) {
        try {
            Log.d(TAG, "🧪 Test response received from $senderNumber")

            // Store test confirmation
            val preferences = context.getSharedPreferences("emergency_responses", Context.MODE_PRIVATE)
            preferences.edit()
                .putString("last_test_response", "$senderNumber|${System.currentTimeMillis()}")
                .putLong("last_test_response_time", System.currentTimeMillis())
                .apply()

            // Could trigger a confirmation SMS back to the contact
            // "SafeguardMe: Test successful! Emergency contact system is working."

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling test response", e)
        }
    }

    private fun handleUnsubscribeRequest(context: Context, senderNumber: String) {
        try {
            Log.w(TAG, "⚠️ Unsubscribe request from $senderNumber")

            // Store unsubscribe request
            val preferences = context.getSharedPreferences("emergency_responses", Context.MODE_PRIVATE)
            preferences.edit()
                .putString("unsubscribe_request", "$senderNumber|${System.currentTimeMillis()}")
                .apply()

            // This might require user action to actually remove the contact
            // For now, just log and potentially notify the user

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling unsubscribe request", e)
        }
    }

    private fun handleGeneralResponse(context: Context, senderNumber: String, messageBody: String) {
        try {
            Log.d(TAG, "💬 General response from emergency contact $senderNumber")

            // Store general response
            val preferences = context.getSharedPreferences("emergency_responses", Context.MODE_PRIVATE)
            preferences.edit()
                .putString("last_general_response", "$senderNumber|${System.currentTimeMillis()}|$messageBody")
                .putLong("last_general_response_time", System.currentTimeMillis())
                .apply()

            // General responses might contain important information
            notifyMonitoringService(context, "GENERAL_CONTACT_RESPONSE", mapOf(
                "sender" to senderNumber,
                "message" to messageBody
            ))

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling general response", e)
        }
    }

    private fun getCurrentSessionId(): String? {
        return try {
            // This would typically come from a shared service or repository
            // For now, return null to indicate no active session
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting current session ID", e)
            null
        }
    }

    private fun notifyMonitoringService(context: Context, eventType: String, data: Map<String, String>) {
        try {
            val intent = Intent(context, SafetyMonitoringService::class.java)
            intent.action = "SMS_RESPONSE_EVENT"
            intent.putExtra("event_type", eventType)
            data.forEach { (key, value) ->
                intent.putExtra(key, value)
            }
            context.startService(intent)

            Log.d(TAG, "🔔 Notified monitoring service: $eventType")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error notifying monitoring service", e)
        }
    }

    /**
     * Utility methods for SMS response analysis
     */
    object SmsAnalysisUtils {

        /**
         * Extract session ID from SMS message if present
         */
        fun extractSessionId(messageBody: String): String? {
            val sessionPattern = Regex("""Session:\s*([a-zA-Z0-9_-]+)""")
            val match = sessionPattern.find(messageBody)
            return match?.groupValues?.get(1)
        }

        /**
         * Check if message indicates immediate danger
         */
        fun indicatesImmediateDanger(messageBody: String): Boolean {
            val dangerKeywords = listOf(
                "immediate danger", "urgent", "now", "happening now",
                "call 911", "police now", "ambulance", "fire"
            )

            return dangerKeywords.any { messageBody.lowercase().contains(it) }
        }

        /**
         * Extract any embedded commands from SMS
         */
        fun extractCommands(messageBody: String): List<String> {
            val commands = mutableListOf<String>()

            if (messageBody.contains("stop monitoring", ignoreCase = true)) {
                commands.add("STOP_MONITORING")
            }

            if (messageBody.contains("send location", ignoreCase = true)) {
                commands.add("REQUEST_LOCATION")
            }

            if (messageBody.contains("call me", ignoreCase = true)) {
                commands.add("REQUEST_CALL")
            }

            return commands
        }

        /**
         * Get sentiment analysis of the message
         */
        fun analyzeSentiment(messageBody: String): SentimentAnalysis {
            val positiveWords = listOf("safe", "ok", "good", "fine", "yes", "well")
            val negativeWords = listOf("help", "danger", "no", "bad", "hurt", "scared")
            val urgentWords = listOf("now", "urgent", "immediately", "asap", "emergency")

            val hasPositive = positiveWords.any { messageBody.lowercase().contains(it) }
            val hasNegative = negativeWords.any { messageBody.lowercase().contains(it) }
            val hasUrgent = urgentWords.any { messageBody.lowercase().contains(it) }

            return SentimentAnalysis(
                isPositive = hasPositive,
                isNegative = hasNegative,
                isUrgent = hasUrgent,
                overallSentiment = when {
                    hasUrgent && hasNegative -> "URGENT_NEGATIVE"
                    hasNegative -> "NEGATIVE"
                    hasPositive -> "POSITIVE"
                    else -> "NEUTRAL"
                }
            )
        }
    }
}

/**
 * Types of SMS responses from emergency contacts
 */
enum class SmsResponseType {
    SAFETY_CONFIRMATION,    // "I'm safe", "OK", "All good"
    EMERGENCY_ESCALATION,   // "Help", "Emergency", "Call police"
    TEST_RESPONSE,          // "Test", "Testing"
    UNSUBSCRIBE_REQUEST,    // "Stop", "Unsubscribe"
    GENERAL_RESPONSE        // Any other response
}

/**
 * Sentiment analysis result
 */
data class SentimentAnalysis(
    val isPositive: Boolean,
    val isNegative: Boolean,
    val isUrgent: Boolean,
    val overallSentiment: String
)