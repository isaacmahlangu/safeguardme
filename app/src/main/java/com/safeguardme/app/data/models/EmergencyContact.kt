// =============================================
// EmergencyContact.kt - Core Data Model
// =============================================

package com.safeguardme.app.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class EmergencyContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val contactType: ContactType = ContactType.TRUSTED_PERSON,
    val priority: Int = 1, // 1 = highest priority
    val isVerified: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastContactedAt: Long = 0L,
    val isActive: Boolean = true,

    // Safety-specific fields
    val canReceiveLocation: Boolean = true,
    val canReceiveEmergencyAlerts: Boolean = true,
    val notificationMethod: NotificationMethod = NotificationMethod.SMS_AND_CALL,
    val timeZone: String = "", // For international contacts
    val alternatePhoneNumber: String = "", // Backup contact method
    val address: String = "", // For emergency services

    // Security and privacy
    val isEncrypted: Boolean = false, // For sensitive contacts
    val accessLevel: AccessLevel = AccessLevel.STANDARD
) : Parcelable {

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MAX_PHONE_LENGTH = 20
        const val MAX_NOTES_LENGTH = 500
        const val MAX_CONTACTS_PER_USER = 10
        const val MIN_HIGH_PRIORITY_CONTACTS = 2

        // Default relationships for safety scenarios
        val COMMON_RELATIONSHIPS = listOf(
            "Family Member", "Close Friend", "Partner/Spouse", "Neighbor",
            "Colleague", "Counselor/Therapist", "Legal Advocate", "Medical Professional",
            "Community Leader", "Emergency Services", "Crisis Hotline", "Other"
        )
    }

    /**
     * Comprehensive validation for emergency contacts
     */
    fun isValid(): ValidationResult {
        val errors = mutableListOf<String>()

        // Name validation
        when {
            name.isBlank() -> errors.add("Contact name is required")
            name.length > MAX_NAME_LENGTH -> errors.add("Contact name is too long")
            name.trim() != name -> errors.add("Contact name has invalid formatting")
        }

        // Phone number validation
        when {
            phoneNumber.isBlank() -> errors.add("Phone number is required")
            !isValidPhoneNumber(phoneNumber) -> errors.add("Invalid phone number format")
            phoneNumber.length > MAX_PHONE_LENGTH -> errors.add("Phone number is too long")
        }

        // Relationship validation
        when {
            relationship.isBlank() -> errors.add("Relationship is required")
            relationship.length > 50 -> errors.add("Relationship description is too long")
        }

        // Notes validation
        if (notes.length > MAX_NOTES_LENGTH) {
            errors.add("Notes are too long (max $MAX_NOTES_LENGTH characters)")
        }

        // Priority validation
        if (priority < 1 || priority > 10) {
            errors.add("Priority must be between 1 and 10")
        }

        // Alternate phone validation (if provided)
        if (alternatePhoneNumber.isNotBlank() && !isValidPhoneNumber(alternatePhoneNumber)) {
            errors.add("Invalid alternate phone number format")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors)
        }
    }

    /**
     * Sanitize contact data for security and consistency
     */
    fun sanitized(): EmergencyContact {
        return copy(
            name = name.trim().take(MAX_NAME_LENGTH),
            phoneNumber = sanitizePhoneNumber(phoneNumber),
            relationship = relationship.trim().take(50),
            notes = notes.trim().take(MAX_NOTES_LENGTH),
            alternatePhoneNumber = if (alternatePhoneNumber.isNotBlank()) {
                sanitizePhoneNumber(alternatePhoneNumber)
            } else {
                ""
            },
            address = address.trim().take(200),
            timeZone = timeZone.trim().take(50)
        )
    }

    /**
     * Check if contact is considered high priority for emergency situations
     */
    fun isHighPriority(): Boolean = priority <= 3

    /**
     * Check if contact can be reached for emergency alerts
     */
    fun canReceiveEmergencyNotifications(): Boolean {
        return isActive &&
                isValid() == ValidationResult.Success &&
                canReceiveEmergencyAlerts &&
                notificationMethod != NotificationMethod.NONE
    }

    /**
     * Get display name for UI (truncated if necessary)
     */
    fun getDisplayName(maxLength: Int = 30): String {
        return if (name.length <= maxLength) {
            name
        } else {
            "${name.take(maxLength - 3)}..."
        }
    }

    /**
     * Get formatted phone number for display
     */
    fun getFormattedPhoneNumber(): String {
        return formatPhoneNumberForDisplay(phoneNumber)
    }

    /**
     * Create a safe copy for logging (removes sensitive info)
     */
    fun toLogSafeString(): String {
        return "EmergencyContact(id=$id, name=${name.take(3)}***, " +
                "relationship=$relationship, priority=$priority, isActive=$isActive)"
    }

    // Private validation helpers
    private fun isValidPhoneNumber(phone: String): Boolean {
        // Remove all non-digit characters except + for international numbers
        val cleanPhone = phone.replace(Regex("[^+\\d]"), "")

        return when {
            // International format (+1234567890)
            cleanPhone.startsWith("+") -> {
                val digits = cleanPhone.substring(1)
                digits.length in 7..15 && digits.all { it.isDigit() }
            }
            // Domestic format (various lengths depending on country)
            else -> {
                cleanPhone.length in 7..15 && cleanPhone.all { it.isDigit() }
            }
        }
    }

    private fun sanitizePhoneNumber(phone: String): String {
        // Keep only digits, +, -, (, ), and spaces
        return phone.replace(Regex("[^+\\d\\s\\-\\(\\)]"), "").trim()
    }

    private fun formatPhoneNumberForDisplay(phone: String): String {
        val digits = phone.replace(Regex("[^+\\d]"), "")

        return when {
            // International number
            digits.startsWith("+") -> digits
            // US/Canada number (10 digits)
            digits.length == 10 -> {
                "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
            }
            // US/Canada with country code (11 digits starting with 1)
            digits.length == 11 && digits.startsWith("1") -> {
                "+1 ${digits.substring(1, 4)}-${digits.substring(4, 7)}-${digits.substring(7)}"
            }
            // Other formats - return as-is with basic formatting
            else -> digits
        }
    }
}

// =============================================
// Supporting Enums and Classes
// =============================================

@Parcelize
enum class ContactType : Parcelable {
    TRUSTED_PERSON,      // Family, friends, trusted individuals
    PROFESSIONAL,        // Counselors, advocates, medical professionals
    EMERGENCY_SERVICE,   // Police, ambulance, fire department
    CRISIS_HOTLINE,      // Domestic violence hotlines, suicide prevention
    LEGAL_ADVOCATE,      // Lawyers, legal aid organizations
    MEDICAL_PROVIDER,    // Doctors, therapists, hospitals
    COMMUNITY_RESOURCE,  // Shelters, community organizations


    OTHER;

    fun getDisplayName(): String = when (this) {
        TRUSTED_PERSON -> "Trusted Person"
        PROFESSIONAL -> "Professional Support"
        EMERGENCY_SERVICE -> "Emergency Service"
        CRISIS_HOTLINE -> "Crisis Hotline"
        LEGAL_ADVOCATE -> "Legal Advocate"
        MEDICAL_PROVIDER -> "Medical Provider"
        COMMUNITY_RESOURCE -> "Community Resource"
        OTHER -> "Other"
    }

    fun getDescription(): String = when (this) {
        TRUSTED_PERSON -> "Family members, friends, or other trusted individuals"
        PROFESSIONAL -> "Counselors, therapists, social workers, or advocates"
        EMERGENCY_SERVICE -> "Police, ambulance, fire department, or emergency dispatch"
        CRISIS_HOTLINE -> "24/7 crisis support lines and helplines"
        LEGAL_ADVOCATE -> "Lawyers, legal aid, or court advocates"
        MEDICAL_PROVIDER -> "Doctors, nurses, hospitals, or medical facilities"
        COMMUNITY_RESOURCE -> "Shelters, support groups, or community organizations"
        OTHER -> "Other types of emergency contacts"
    }
}

@Parcelize
enum class NotificationMethod : Parcelable {
    SMS_ONLY,
    CALL_ONLY,
    SMS_AND_CALL,
    EMAIL_ONLY,
    EMAIL_AND_SMS,
    ALL_METHODS,
    NONE;

    fun getDisplayName(): String = when (this) {
        SMS_ONLY -> "Text Message Only"
        CALL_ONLY -> "Phone Call Only"
        SMS_AND_CALL -> "Text & Call"
        EMAIL_ONLY -> "Email Only"
        EMAIL_AND_SMS -> "Email & Text"
        ALL_METHODS -> "All Methods"
        NONE -> "No Notifications"
    }
}

@Parcelize
enum class AccessLevel : Parcelable {
    STANDARD,    // Normal contact access
    HIGH,        // High priority access (immediate alerts)
    RESTRICTED,  // Limited access (certain conditions only)
    EMERGENCY;   // Emergency-only access (life-threatening situations)

    fun getDisplayName(): String = when (this) {
        STANDARD -> "Standard Access"
        HIGH -> "High Priority"
        RESTRICTED -> "Restricted Access"
        EMERGENCY -> "Emergency Only"
    }
}

// =============================================
// Validation Result Classes
// =============================================

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val errors: List<String>) : ValidationResult()

    fun isValid(): Boolean = this is Success

    fun getErrorMessages(): List<String> = when (this) {
        is Success -> emptyList()
        is Error -> errors
    }
}

// =============================================
// Emergency Contact Extensions and Utilities
// =============================================

/**
 * Extension functions for emergency contact collections
 */
fun List<EmergencyContact>.getHighPriorityContacts(): List<EmergencyContact> {
    return filter { it.isHighPriority() && it.isActive }
        .sortedBy { it.priority }
}

fun List<EmergencyContact>.getContactsByType(type: ContactType): List<EmergencyContact> {
    return filter { it.contactType == type && it.isActive }
        .sortedBy { it.priority }
}

fun List<EmergencyContact>.canReceiveEmergencyAlerts(): List<EmergencyContact> {
    return filter { it.canReceiveEmergencyNotifications() }
        .sortedBy { it.priority }
}

fun List<EmergencyContact>.validateContactList(): ValidationResult {
    val errors = mutableListOf<String>()

    // Check maximum contacts limit
    if (size > EmergencyContact.MAX_CONTACTS_PER_USER) {
        errors.add("Too many contacts (max ${EmergencyContact.MAX_CONTACTS_PER_USER})")
    }

    // Check for minimum high priority contacts
    val highPriorityCount = count { it.isHighPriority() && it.isActive }
    if (highPriorityCount < EmergencyContact.MIN_HIGH_PRIORITY_CONTACTS) {
        errors.add("Need at least ${EmergencyContact.MIN_HIGH_PRIORITY_CONTACTS} high priority contacts")
    }

    // Check for duplicate phone numbers
    val phoneNumbers = mapNotNull { contact ->
        if (contact.phoneNumber.isNotBlank()) contact.phoneNumber else null
    }
    val duplicateNumbers = phoneNumbers.groupingBy { it }.eachCount().filterValues { it > 1 }
    if (duplicateNumbers.isNotEmpty()) {
        errors.add("Duplicate phone numbers found: ${duplicateNumbers.keys.joinToString(", ")}")
    }

    // Validate individual contacts
    forEach { contact ->
        val contactValidation = contact.isValid()
        if (!contactValidation.isValid()) {
            errors.addAll(contactValidation.getErrorMessages().map { "${contact.name}: $it" })
        }
    }

    return if (errors.isEmpty()) {
        ValidationResult.Success
    } else {
        ValidationResult.Error(errors)
    }
}

// =============================================
// Emergency Contact Builder Pattern
// =============================================

class EmergencyContactBuilder {
    private var name: String = ""
    private var phoneNumber: String = ""
    private var relationship: String = ""
    private var contactType: ContactType = ContactType.TRUSTED_PERSON
    private var priority: Int = 1
    private var notes: String = ""
    private var notificationMethod: NotificationMethod = NotificationMethod.SMS_AND_CALL

    fun name(name: String) = apply { this.name = name }
    fun phoneNumber(phoneNumber: String) = apply { this.phoneNumber = phoneNumber }
    fun relationship(relationship: String) = apply { this.relationship = relationship }
    fun contactType(contactType: ContactType) = apply { this.contactType = contactType }
    fun priority(priority: Int) = apply { this.priority = priority }
    fun notes(notes: String) = apply { this.notes = notes }
    fun notificationMethod(method: NotificationMethod) = apply { this.notificationMethod = method }

    fun build(): EmergencyContact {
        return EmergencyContact(
            name = name,
            phoneNumber = phoneNumber,
            relationship = relationship,
            contactType = contactType,
            priority = priority,
            notes = notes,
            notificationMethod = notificationMethod
        ).sanitized()
    }
}

// =============================================
// Usage Examples and Factory Methods
// =============================================

object EmergencyContactFactory {

    /**
     * Create a quick emergency contact with minimal required fields
     */
    fun createQuick(
        name: String,
        phoneNumber: String,
        relationship: String
    ): EmergencyContact {
        return EmergencyContactBuilder()
            .name(name)
            .phoneNumber(phoneNumber)
            .relationship(relationship)
            .build()
    }

    /**
     * Create default crisis hotlines for domestic violence support
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
                phoneNumber = "741741",
                relationship = "Crisis Support",
                contactType = ContactType.CRISIS_HOTLINE,
                priority = 2,
                notes = "Text HOME to 741741 for crisis support",
                canReceiveLocation = false,
                notificationMethod = NotificationMethod.SMS_ONLY
            )
        )
    }

    /**
     * Create emergency services contacts
     */
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