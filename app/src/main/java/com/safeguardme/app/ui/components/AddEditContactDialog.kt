// =============================================
// AddEditContactDialog.kt - Comprehensive Implementation
// =============================================

package com.safeguardme.app.ui.components

import NotificationMethodPicker
import RelationshipSuggestions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.safeguardme.app.data.models.ContactType
import com.safeguardme.app.data.models.EmergencyContact
import com.safeguardme.app.data.models.NotificationMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactDialog(
    contact: EmergencyContact?,
    onSave: (EmergencyContact) -> Unit,
    onDismiss: () -> Unit,
    validationErrors: List<String> = emptyList(),
    isLoading: Boolean = false
) {
    val isEditing = contact != null

    // Form state
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phoneNumber by remember { mutableStateOf(contact?.phoneNumber ?: "") }
    var alternatePhone by remember { mutableStateOf(contact?.alternatePhoneNumber ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "") }
    var contactType by remember { mutableStateOf(contact?.contactType ?: ContactType.TRUSTED_PERSON) }
    var priority by remember { mutableStateOf(contact?.priority ?: 1) }
    var notes by remember { mutableStateOf(contact?.notes ?: "") }
    var notificationMethod by remember { mutableStateOf(contact?.notificationMethod ?: NotificationMethod.SMS_AND_CALL) }
    var canReceiveLocation by remember { mutableStateOf(contact?.canReceiveLocation ?: true) }
    var canReceiveEmergencyAlerts by remember { mutableStateOf(contact?.canReceiveEmergencyAlerts ?: true) }

    // UI state
    var showContactTypePicker by remember { mutableStateOf(false) }
    var showNotificationMethodPicker by remember { mutableStateOf(false) }
    var showRelationshipSuggestions by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(0) }

    // Focus management
    val nameFocusRequester = remember { FocusRequester() }
    val phoneFocusRequester = remember { FocusRequester() }
    val relationshipFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Validation state
    val nameError = remember(name) {
        when {
            name.isBlank() -> "Name is required"
            name.length > EmergencyContact.MAX_NAME_LENGTH -> "Name is too long"
            else -> null
        }
    }

    val phoneError = remember(phoneNumber) {
        when {
            phoneNumber.isBlank() -> "Phone number is required"
            !isValidPhoneNumber(phoneNumber) -> "Invalid phone number format"
            else -> null
        }
    }

    val relationshipError = remember(relationship) {
        when {
            relationship.isBlank() -> "Relationship is required"
            relationship.length > 50 -> "Relationship is too long"
            else -> null
        }
    }

    // Form validation
    val isFormValid = nameError == null && phoneError == null && relationshipError == null
    val canSave = isFormValid && !isLoading && name.isNotBlank() && phoneNumber.isNotBlank()

    // Auto-focus on first field
    LaunchedEffect(Unit) {
        if (!isEditing) {
            nameFocusRequester.requestFocus()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                ContactDialogHeader(
                    isEditing = isEditing,
                    contactName = name.ifBlank { "New Contact" },
                    onDismiss = onDismiss,
                    currentStep = currentStep,
                    totalSteps = 3
                )

                Divider()

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Step indicator
                    if (!isEditing) {
                        StepIndicator(
                            currentStep = currentStep,
                            totalSteps = 3,
                            stepTitles = listOf("Basic Info", "Contact Details", "Preferences")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Error display
                    if (validationErrors.isNotEmpty()) {
                        ErrorCard(errors = validationErrors)
                    }

                    when (currentStep) {
                        0 -> {
                            // Step 1: Basic Information
                            BasicInfoStep(
                                name = name,
                                onNameChange = { name = it.take(EmergencyContact.MAX_NAME_LENGTH) },
                                nameError = nameError,
                                nameFocusRequester = nameFocusRequester,
                                phoneNumber = phoneNumber,
                                onPhoneChange = { phoneNumber = formatPhoneInput(it) },
                                phoneError = phoneError,
                                phoneFocusRequester = phoneFocusRequester,
                                onNext = {
                                    if (name.isNotBlank() && phoneNumber.isNotBlank()) {
                                        currentStep = 1
                                        keyboardController?.hide()
                                    }
                                }
                            )
                        }

                        1 -> {
                            // Step 2: Contact Details
                            ContactDetailsStep(
                                relationship = relationship,
                                onRelationshipChange = { relationship = it.take(50) },
                                relationshipError = relationshipError,
                                relationshipFocusRequester = relationshipFocusRequester,
                                showRelationshipSuggestions = showRelationshipSuggestions,
                                onShowSuggestions = { showRelationshipSuggestions = it },
                                contactType = contactType,
                                onContactTypeClick = { showContactTypePicker = true },
                                priority = priority,
                                onPriorityChange = { priority = it },
                                alternatePhone = alternatePhone,
                                onAlternatePhoneChange = { alternatePhone = formatPhoneInput(it) },
                                onNext = {
                                    if (relationship.isNotBlank()) {
                                        currentStep = 2
                                        keyboardController?.hide()
                                    }
                                },
                                onBack = { currentStep = 0 }
                            )
                        }

                        2 -> {
                            // Step 3: Preferences
                            PreferencesStep(
                                notificationMethod = notificationMethod,
                                onNotificationMethodClick = { showNotificationMethodPicker = true },
                                canReceiveLocation = canReceiveLocation,
                                onCanReceiveLocationChange = { canReceiveLocation = it },
                                canReceiveEmergencyAlerts = canReceiveEmergencyAlerts,
                                onCanReceiveEmergencyAlertsChange = { canReceiveEmergencyAlerts = it },
                                notes = notes,
                                onNotesChange = { notes = it.take(EmergencyContact.MAX_NOTES_LENGTH) },
                                onBack = { currentStep = 1 }
                            )
                        }
                    }
                }

                Divider()

                // Footer
                ContactDialogFooter(
                    isEditing = isEditing,
                    canSave = canSave,
                    isLoading = isLoading,
                    currentStep = currentStep,
                    totalSteps = if (isEditing) 1 else 3,
                    onSave = {
                        val newContact = if (isEditing) {
                            contact!!.copy(
                                name = name.trim(),
                                phoneNumber = phoneNumber.trim(),
                                relationship = relationship.trim(),
                                contactType = contactType,
                                priority = priority,
                                notes = notes.trim(),
                                alternatePhoneNumber = alternatePhone.trim(),
                                notificationMethod = notificationMethod,
                                canReceiveLocation = canReceiveLocation,
                                canReceiveEmergencyAlerts = canReceiveEmergencyAlerts
                            )
                        } else {
                            EmergencyContact(
                                name = name.trim(),
                                phoneNumber = phoneNumber.trim(),
                                relationship = relationship.trim(),
                                contactType = contactType,
                                priority = priority,
                                notes = notes.trim(),
                                alternatePhoneNumber = alternatePhone.trim(),
                                notificationMethod = notificationMethod,
                                canReceiveLocation = canReceiveLocation,
                                canReceiveEmergencyAlerts = canReceiveEmergencyAlerts
                            )
                        }
                        onSave(newContact)
                    },
                    onCancel = onDismiss,
                    onNext = {
                        when (currentStep) {
                            0 -> if (name.isNotBlank() && phoneNumber.isNotBlank()) currentStep = 1
                            1 -> if (relationship.isNotBlank()) currentStep = 2
                        }
                    },
                    onBack = {
                        when (currentStep) {
                            1 -> currentStep = 0
                            2 -> currentStep = 1
                        }
                    }
                )
            }
        }

        // Contact Type Picker
        if (showContactTypePicker) {
            ContactTypePicker(
                selectedType = contactType,
                onTypeSelected = {
                    contactType = it
                    showContactTypePicker = false
                },
                onDismiss = { showContactTypePicker = false }
            )
        }

        // Notification Method Picker
        if (showNotificationMethodPicker) {
            NotificationMethodPicker(
                selectedMethod = notificationMethod,
                onMethodSelected = {
                    notificationMethod = it
                    showNotificationMethodPicker = false
                },
                onDismiss = { showNotificationMethodPicker = false }
            )
        }

        // Relationship Suggestions
        if (showRelationshipSuggestions) {
            RelationshipSuggestions(
                onSuggestionSelected = {
                    relationship = it
                    showRelationshipSuggestions = false
                },
                onDismiss = { showRelationshipSuggestions = false }
            )
        }
    }
}

// =============================================
// Header Component
// =============================================

@Composable
private fun ContactDialogHeader(
    isEditing: Boolean,
    contactName: String,
    onDismiss: () -> Unit,
    currentStep: Int,
    totalSteps: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isEditing) "Edit Contact" else "Add Emergency Contact",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (!isEditing) {
                Text(
                    text = "Step ${currentStep + 1} of $totalSteps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (contactName != "New Contact") {
                Text(
                    text = contactName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isEditing) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// =============================================
// Step Indicator Component
// =============================================

@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    stepTitles: List<String>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stepTitles.forEachIndexed { index, title ->
            StepItem(
                stepNumber = index + 1,
                title = title,
                isActive = index == currentStep,
                isCompleted = index < currentStep,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: Int,
    title: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.CircleShape,
            color = when {
                isCompleted -> MaterialTheme.colorScheme.primary
                isActive -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = when {
                isActive -> MaterialTheme.colorScheme.primary
                isCompleted -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )
    }
}

// =============================================
// Error Card Component
// =============================================

@Composable
private fun ErrorCard(errors: List<String>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Please fix the following issues:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            errors.forEach { error ->
                Text(
                    text = "• $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

// =============================================
// Step 1: Basic Info
// =============================================

@Composable
private fun BasicInfoStep(
    name: String,
    onNameChange: (String) -> Unit,
    nameError: String?,
    nameFocusRequester: FocusRequester,
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    phoneError: String?,
    phoneFocusRequester: FocusRequester,
    onNext: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Basic Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Enter the essential contact information for this person.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name *") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester),
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { phoneFocusRequester.requestFocus() }
            ),
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            }
        )

        // Phone number field
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number *") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(phoneFocusRequester),
            isError = phoneError != null,
            supportingText = phoneError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onNext() }
            ),
            leadingIcon = {
                Icon(Icons.Default.Phone, contentDescription = null)
            },
            placeholder = { Text("+1 (555) 123-4567") }
        )

        // Helper text
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Use the full name and primary phone number for this contact.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// =============================================
// Step 2: Contact Details
// =============================================

@Composable
private fun ContactDetailsStep(
    relationship: String,
    onRelationshipChange: (String) -> Unit,
    relationshipError: String?,
    relationshipFocusRequester: FocusRequester,
    showRelationshipSuggestions: Boolean,
    onShowSuggestions: (Boolean) -> Unit,
    contactType: ContactType,
    onContactTypeClick: () -> Unit,
    priority: Int,
    onPriorityChange: (Int) -> Unit,
    alternatePhone: String,
    onAlternatePhoneChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Contact Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Relationship field with suggestions
        OutlinedTextField(
            value = relationship,
            onValueChange = onRelationshipChange,
            label = { Text("Relationship *") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(relationshipFocusRequester),
            isError = relationshipError != null,
            supportingText = relationshipError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onNext() }
            ),
            trailingIcon = {
                IconButton(onClick = { onShowSuggestions(true) }) {
                    Icon(Icons.Default.ExpandMore, contentDescription = "Show suggestions")
                }
            },
            placeholder = { Text("e.g., Sister, Best Friend, Counselor") }
        )

        // Contact type selector
        ContactTypeSelector(
            selectedType = contactType,
            onClick = onContactTypeClick
        )

        // Priority slider
        PrioritySelector(
            priority = priority,
            onPriorityChange = onPriorityChange
        )

        // Alternate phone (optional)
        OutlinedTextField(
            value = alternatePhone,
            onValueChange = onAlternatePhoneChange,
            label = { Text("Alternate Phone (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            leadingIcon = {
                Icon(Icons.Default.PhoneCallback, contentDescription = null)
            },
            placeholder = { Text("Backup contact number") }
        )
    }
}

// =============================================
// Step 3: Preferences
// =============================================

@Composable
private fun PreferencesStep(
    notificationMethod: NotificationMethod,
    onNotificationMethodClick: () -> Unit,
    canReceiveLocation: Boolean,
    onCanReceiveLocationChange: (Boolean) -> Unit,
    canReceiveEmergencyAlerts: Boolean,
    onCanReceiveEmergencyAlertsChange: (Boolean) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Preferences",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Notification method selector
        NotificationMethodSelector(
            selectedMethod = notificationMethod,
            onClick = onNotificationMethodClick
        )

        // Permission toggles
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Emergency Permissions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                PermissionToggle(
                    title = "Share Location",
                    description = "Allow this contact to receive your location during emergencies",
                    checked = canReceiveLocation,
                    onCheckedChange = onCanReceiveLocationChange,
                    icon = Icons.Default.LocationOn
                )

                PermissionToggle(
                    title = "Emergency Alerts",
                    description = "Send emergency notifications to this contact",
                    checked = canReceiveEmergencyAlerts,
                    onCheckedChange = onCanReceiveEmergencyAlertsChange,
                    icon = Icons.Default.NotificationImportant
                )
            }
        }

        // Notes field
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Notes (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
            supportingText = {
                Text("${notes.length}/${EmergencyContact.MAX_NOTES_LENGTH}")
            },
            placeholder = { Text("Special instructions, availability, etc.") }
        )
    }
}

// =============================================
// Helper Components
// =============================================

@Composable
private fun ContactTypeSelector(
    selectedType: ContactType,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getContactTypeColor(selectedType),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getContactTypeIcon(selectedType),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selectedType.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = selectedType.getDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrioritySelector(
    priority: Int,
    onPriorityChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Priority Level",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = when {
                    priority <= 3 -> Color(0xFFE57373)
                    priority <= 6 -> Color(0xFFFFB74D)
                    else -> Color(0xFFA5D6A7)
                }
            ) {
                Text(
                    text = when {
                        priority <= 3 -> "HIGH"
                        priority <= 6 -> "MEDIUM"
                        else -> "LOW"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Slider(
            value = priority.toFloat(),
            onValueChange = { onPriorityChange(it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1 (Highest)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Current: $priority",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "10 (Lowest)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotificationMethodSelector(
    selectedMethod: NotificationMethod,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getNotificationMethodIcon(selectedMethod),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notification Method",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = selectedMethod.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// =============================================
// Footer Component
// =============================================

@Composable
private fun ContactDialogFooter(
    isEditing: Boolean,
    canSave: Boolean,
    isLoading: Boolean,
    currentStep: Int,
    totalSteps: Int,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back/Cancel button
        if (isEditing || currentStep == 0) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
        } else {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Back")
            }
        }

        // Next/Save button
        Button(
            onClick = {
                if (isEditing || currentStep == totalSteps - 1) {
                    onSave()
                } else {
                    onNext()
                }
            },
            enabled = canSave && !isLoading,
            modifier = Modifier.weight(1f)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            val buttonText = when {
                isLoading -> if (isEditing) "Updating..." else "Saving..."
                isEditing -> "Update Contact"
                currentStep == totalSteps - 1 -> "Add Contact"
                else -> "Next"
            }

            Text(buttonText)

            if (!isEditing && currentStep < totalSteps - 1 && !isLoading) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// =============================================
// Utility Functions
// =============================================

private fun formatPhoneInput(input: String): String {
    // Remove all non-digit characters except +
    val digits = input.filter { it.isDigit() || it == '+' }

    // Basic US phone number formatting
    return when {
        digits.startsWith("+") -> digits.take(15) // International format
        digits.length <= 3 -> digits
        digits.length <= 6 -> "${digits.take(3)}-${digits.drop(3)}"
        digits.length <= 10 -> "${digits.take(3)}-${digits.drop(3).take(3)}-${digits.drop(6)}"
        else -> "+1-${digits.take(3)}-${digits.drop(3).take(3)}-${digits.drop(6).take(4)}"
    }
}

private fun isValidPhoneNumber(phone: String): Boolean {
    val cleanPhone = phone.replace(Regex("[^+\\d]"), "")
    return when {
        cleanPhone.startsWith("+") -> {
            val digits = cleanPhone.substring(1)
            digits.length in 7..15 && digits.all { it.isDigit() }
        }
        else -> cleanPhone.length in 7..15 && cleanPhone.all { it.isDigit() }
    }
}

// Helper functions for styling (you'll need to implement these)
fun getContactTypeIcon(type: ContactType): ImageVector = when (type) {
    ContactType.TRUSTED_PERSON -> Icons.Default.Person
    ContactType.PROFESSIONAL -> Icons.Default.Work
    ContactType.EMERGENCY_SERVICE -> Icons.Default.Emergency
    ContactType.CRISIS_HOTLINE -> Icons.Default.Phone
    ContactType.LEGAL_ADVOCATE -> Icons.Default.Gavel
    ContactType.MEDICAL_PROVIDER -> Icons.Default.LocalHospital
    ContactType.COMMUNITY_RESOURCE -> Icons.Default.Group
    ContactType.OTHER -> Icons.Default.ContactPhone
}

fun getContactTypeColor(type: ContactType): Color = when (type) {
    ContactType.TRUSTED_PERSON -> Color(0xFF4CAF50)
    ContactType.PROFESSIONAL -> Color(0xFF2196F3)
    ContactType.EMERGENCY_SERVICE -> Color(0xFFF44336)
    ContactType.CRISIS_HOTLINE -> Color(0xFF9C27B0)
    ContactType.LEGAL_ADVOCATE -> Color(0xFF607D8B)
    ContactType.MEDICAL_PROVIDER -> Color(0xFF00BCD4)
    ContactType.COMMUNITY_RESOURCE -> Color(0xFFFF9800)
    ContactType.OTHER -> Color(0xFF795548)
}

private fun getNotificationMethodIcon(method: NotificationMethod): ImageVector = when (method) {
    NotificationMethod.SMS_ONLY -> Icons.Default.Sms
    NotificationMethod.CALL_ONLY -> Icons.Default.Call
    NotificationMethod.SMS_AND_CALL -> Icons.Default.ContactPhone
    NotificationMethod.EMAIL_ONLY -> Icons.Default.Email
    NotificationMethod.EMAIL_AND_SMS -> Icons.Default.MailOutline
    NotificationMethod.ALL_METHODS -> Icons.Default.Notifications
    NotificationMethod.NONE -> Icons.Default.NotificationsOff
}