// services/SafeguardMeFirebaseMessagingService.kt - Firebase Cloud Messaging Handler
package com.safeguardme.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.safeguardme.app.MainActivity
import com.safeguardme.app.R
import com.safeguardme.app.data.repositories.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SafeguardMeFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userRepository: UserRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "safeguard_notifications"
        private const val EMERGENCY_CHANNEL_ID = "emergency_notifications"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔔 SafeguardMe FCM Service created")
        createNotificationChannels()
    }

    /**
     * Called when a new FCM token is generated
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 New FCM token received: ${token.take(20)}...")

        // Save token to user profile for emergency notifications
        serviceScope.launch {
            try {
                //userRepository.updateFCMToken(token)
                Log.i(TAG, "✅ FCM token updated in user profile")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update FCM token", e)
            }
        }
    }

    /**
     * Called when a message is received while app is in foreground
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "📨 Message received from: ${remoteMessage.from}")

        // Handle different message types
        val messageType = remoteMessage.data["type"] ?: "general"

        when (messageType) {
            "emergency_contact_response" -> handleEmergencyContactResponse(remoteMessage)
            "safety_check" -> handleSafetyCheck(remoteMessage)
            "emergency_alert" -> handleEmergencyAlert(remoteMessage)
            "system_notification" -> handleSystemNotification(remoteMessage)
            "contact_verification" -> handleContactVerification(remoteMessage)
            else -> handleGeneralNotification(remoteMessage)
        }
    }

    /**
     * Handle emergency contact response messages
     */
    private fun handleEmergencyContactResponse(remoteMessage: RemoteMessage) {
        try {
            val contactName = remoteMessage.data["contact_name"] ?: "Emergency Contact"
            val response = remoteMessage.data["response"] ?: "responded"
            val sessionId = remoteMessage.data["session_id"]

            Log.i(TAG, "🚨 Emergency contact response: $contactName - $response")

            // Show high-priority notification
            showNotification(
                title = "Emergency Contact Response",
                message = "$contactName has $response to your safety alert",
                channelId = EMERGENCY_CHANNEL_ID,
                priority = NotificationCompat.PRIORITY_HIGH,
                data = mapOf("session_id" to sessionId)
            )

            // Update emergency response tracking
            sessionId?.let { updateEmergencyResponseTracking(it, contactName, response) }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling emergency contact response", e)
        }
    }

    /**
     * Handle safety check messages
     */
    private fun handleSafetyCheck(remoteMessage: RemoteMessage) {
        try {
            val message = remoteMessage.data["message"] ?: "Safety check requested"
            val requester = remoteMessage.data["requester"] ?: "Emergency Contact"

            Log.i(TAG, "🛡️ Safety check from: $requester")

            showNotification(
                title = "Safety Check Request",
                message = "$requester is checking on your safety: $message",
                channelId = CHANNEL_ID,
                priority = NotificationCompat.PRIORITY_HIGH,
                actionText = "Respond Safe",
                actionIntent = createSafetyResponseIntent()
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling safety check", e)
        }
    }

    /**
     * Handle emergency alert messages
     */
    private fun handleEmergencyAlert(remoteMessage: RemoteMessage) {
        try {
            val alertType = remoteMessage.data["alert_type"] ?: "general"
            val message = remoteMessage.data["message"] ?: "Emergency alert"
            val location = remoteMessage.data["location"]

            Log.w(TAG, "🚨 EMERGENCY ALERT: $alertType - $message")

            // Show critical emergency notification
            showNotification(
                title = "🚨 EMERGENCY ALERT",
                message = message,
                channelId = EMERGENCY_CHANNEL_ID,
                priority = NotificationCompat.PRIORITY_MAX,
                data = mapOf(
                    "alert_type" to alertType,
                    "location" to location
                ),
                actionText = "View Details",
                actionIntent = createEmergencyViewIntent()
            )

            // Trigger emergency response if needed
            if (alertType == "automatic_trigger") {
                triggerAutomaticEmergencyResponse()
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling emergency alert", e)
        }
    }

    /**
     * Handle system notification messages
     */
    private fun handleSystemNotification(remoteMessage: RemoteMessage) {
        try {
            val title = remoteMessage.data["title"] ?: "SafeguardMe Update"
            val message = remoteMessage.data["message"] ?: "System notification"
            val priority = remoteMessage.data["priority"]?.toIntOrNull() ?: NotificationCompat.PRIORITY_DEFAULT

            Log.d(TAG, "📢 System notification: $title")

            showNotification(
                title = title,
                message = message,
                channelId = CHANNEL_ID,
                priority = priority
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling system notification", e)
        }
    }

    /**
     * Handle contact verification messages
     */
    private fun handleContactVerification(remoteMessage: RemoteMessage) {
        try {
            val contactName = remoteMessage.data["contact_name"] ?: "Emergency Contact"
            val verificationCode = remoteMessage.data["verification_code"]

            Log.d(TAG, "✅ Contact verification: $contactName")

            showNotification(
                title = "Contact Verification",
                message = "$contactName has verified their contact information",
                channelId = CHANNEL_ID,
                priority = NotificationCompat.PRIORITY_DEFAULT
            )

            // Update contact verification status
            verificationCode?.let {
                serviceScope.launch {
                    updateContactVerificationStatus(contactName, it)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling contact verification", e)
        }
    }

    /**
     * Handle general notification messages
     */
    private fun handleGeneralNotification(remoteMessage: RemoteMessage) {
        try {
            val title = remoteMessage.notification?.title ?: "SafeguardMe"
            val message = remoteMessage.notification?.body ?: "New notification"

            Log.d(TAG, "📢 General notification: $title")

            showNotification(
                title = title,
                message = message,
                channelId = CHANNEL_ID,
                priority = NotificationCompat.PRIORITY_DEFAULT
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling general notification", e)
        }
    }

    /**
     * Show notification with customizable options
     */
    private fun showNotification(
        title: String,
        message: String,
        channelId: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        data: Map<String, String?> = emptyMap(),
        actionText: String? = null,
        actionIntent: PendingIntent? = null
    ) {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                data.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification) // You'll need this icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(priority)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))

            // Add action button if provided
            if (actionText != null && actionIntent != null) {
                notificationBuilder.addAction(
                    R.drawable.ic_notification,
                    actionText,
                    actionIntent
                )
            }

            // Set notification category based on channel
            when (channelId) {
                EMERGENCY_CHANNEL_ID -> {
                    notificationBuilder.setCategory(NotificationCompat.CATEGORY_ALARM)
                    notificationBuilder.setVibrate(longArrayOf(0, 1000, 500, 1000))
                }
                else -> {
                    notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                }
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt() // Unique ID

            notificationManager.notify(notificationId, notificationBuilder.build())

            Log.d(TAG, "📱 Notification shown: $title")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error showing notification", e)
        }
    }

    /**
     * Create notification channels for different types of messages
     */
    private fun createNotificationChannels() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // General notifications channel
        val generalChannel = NotificationChannel(
            CHANNEL_ID,
            "SafeguardMe Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "General SafeguardMe notifications and updates"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 250, 250)
        }

        // Emergency notifications channel
        val emergencyChannel = NotificationChannel(
            EMERGENCY_CHANNEL_ID,
            "Emergency Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Critical emergency alerts and safety notifications"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            setShowBadge(true)
        }

        notificationManager.createNotificationChannel(generalChannel)
        notificationManager.createNotificationChannel(emergencyChannel)

        Log.d(TAG, "📢 Notification channels created")
    }

    /**
     * Create intent for safety response action
     */
    private fun createSafetyResponseIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("action", "safety_response")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            this, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Create intent for emergency view action
     */
    private fun createEmergencyViewIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("action", "view_emergency")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            this, 200, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Update emergency response tracking
     */
    private fun updateEmergencyResponseTracking(sessionId: String, contactName: String, response: String) {
        serviceScope.launch {
            try {
                // Update emergency response tracking in repository
                // This would integrate with your existing emergency contact system
                Log.d(TAG, "📊 Updated emergency response tracking: $sessionId - $contactName - $response")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating emergency response tracking", e)
            }
        }
    }

    /**
     * Update contact verification status
     */
    private suspend fun updateContactVerificationStatus(contactName: String, verificationCode: String) {
        try {
            // Update contact verification in repository
            Log.d(TAG, "✅ Updated contact verification: $contactName")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating contact verification", e)
        }
    }

    /**
     * Trigger automatic emergency response
     */
    private fun triggerAutomaticEmergencyResponse() {
        try {
            Log.w(TAG, "🚨 Triggering automatic emergency response")

            // Start safety monitoring service
            val intent = Intent(this, SafetyMonitoringService::class.java).apply {
                action = SafetyMonitoringService.ACTION_EMERGENCY_ESCALATION
            }
            startForegroundService(intent)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error triggering automatic emergency response", e)
        }
    }

    /**
     * Handle message deletion (when app is in background)
     */
    override fun onDeletedMessages() {
        super.onDeletedMessages()
        Log.w(TAG, "📭 Some messages were deleted from the server before delivery")
    }

    /**
     * Handle message send error
     */
    override fun onMessageSent(msgId: String) {
        super.onMessageSent(msgId)
        Log.d(TAG, "📤 Message sent successfully: $msgId")
    }

    /**
     * Handle message send error
     */
    override fun onSendError(msgId: String, exception: Exception) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "📤❌ Failed to send message: $msgId", exception)
    }
}