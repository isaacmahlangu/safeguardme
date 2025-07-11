import com.safeguardme.app.data.models.ContactType
import com.safeguardme.app.data.models.EmergencyContact
import com.safeguardme.app.data.models.NotificationMethod

object EmergencyContactFactory {
    /**
     * ✅ FIXED: Create default crisis hotlines with proper phone validation
     */
    fun createCrisisHotlines(): List<EmergencyContact> {
        return listOf(
            EmergencyContact(
                name = "National DV Hotline",
                phoneNumber = "1-800-799-7233",
                relationship = "Crisis Support",
                contactType = ContactType.CRISIS_HOTLINE,
                priority = 1,
                notes = "24/7 confidential support for domestic violence survivors",
                canReceiveLocation = false,
                notificationMethod = NotificationMethod.CALL_ONLY
            ),
            EmergencyContact(
                name = "Crisis Text Line",
                phoneNumber = "741741", // ✅ This will now validate correctly
                relationship = "Crisis Support",
                contactType = ContactType.CRISIS_HOTLINE,
                priority = 2,
                notes = "Text HOME to 741741 for crisis support",
                canReceiveLocation = false,
                notificationMethod = NotificationMethod.SMS_ONLY
            )
        )
    }

    fun createEmergencyServices(): List<EmergencyContact> {
        return listOf(
            EmergencyContact(
                name = "Emergency Services",
                phoneNumber = "911",
                relationship = "Emergency Response",
                contactType = ContactType.EMERGENCY_SERVICE,
                priority = 1,
                notes = "Police, Fire, Medical Emergency",
                notificationMethod = NotificationMethod.CALL_ONLY
            )
        )
    }
}