// =============================================
// EmergencyContactsScreen.kt
// =============================================

package com.safeguardme.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.safeguardme.app.data.models.ContactType
import com.safeguardme.app.data.models.EmergencyContact
import com.safeguardme.app.ui.components.AddEditContactDialog
import com.safeguardme.app.ui.viewmodels.EmergencyContactViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    navController: NavController,
    viewModel: EmergencyContactViewModel = hiltViewModel()
) {
    val contacts by viewModel.contacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val showAddDialog by viewModel.showAddDialog.collectAsState()
    val editingContact by viewModel.editingContact.collectAsState()
    val emergencyReadyContacts by viewModel.emergencyReadyContacts.collectAsState()

    var showEmergencyReadinessCard by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Emergency Contacts",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startAddingContact() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Contact")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.startAddingContact() },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Emergency Contact")
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            // Emergency Readiness Card
            AnimatedVisibility(
                visible = showEmergencyReadinessCard,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                EmergencyReadinessCard(
                    emergencyReadyCount = emergencyReadyContacts.size,
                    totalContacts = contacts.size,
                    onDismiss = { showEmergencyReadinessCard = false }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Contacts list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (contacts.isEmpty() && !isLoading) {
                    item {
                        EmptyContactsState(
                            onAddContact = { viewModel.startAddingContact() }
                        )
                    }
                } else {
                    // Group contacts by priority/type
                    val highPriorityContacts = contacts.filter { it.isHighPriority() }
                    val otherContacts = contacts.filter { !it.isHighPriority() }

                    if (highPriorityContacts.isNotEmpty()) {
                        item {
                            Text(
                                text = "High Priority",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(highPriorityContacts) { contact ->
                            EmergencyContactCard(
                                contact = contact,
                                onEdit = { viewModel.startEditingContact(contact) },
                                onDelete = { viewModel.deleteContact(contact.id) },
                                onVerify = { viewModel.verifyContact(contact.id) }
                            )
                        }
                    }

                    if (otherContacts.isNotEmpty()) {
                        item {
                            Text( 
                                text = "Other Contacts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        items(otherContacts) { contact ->
                            EmergencyContactCard(
                                contact = contact,
                                onEdit = { viewModel.startEditingContact(contact) },
                                onDelete = { viewModel.deleteContact(contact.id) },
                                onVerify = { viewModel.verifyContact(contact.id) }
                            )
                        }
                    }
                }
            }
        }

        // Error snackbar
        error?.let { errorMessage ->
            LaunchedEffect(errorMessage) {
                // Show snackbar or handle error display
                viewModel.clearError()
            }
        }

        // Success message
        successMessage?.let { message ->
            LaunchedEffect(message) {
                // Show success snackbar
                viewModel.clearSuccessMessage()
            }
        }

        // Add/Edit Contact Dialog
        if (showAddDialog) {
            AddEditContactDialog(
                contact = editingContact,
                onSave = { contact ->
                    viewModel.saveContact(contact)
                },
                onDismiss = { viewModel.cancelEditing() }
            )
        }
    }
}

@Composable
private fun EmergencyReadinessCard(
    emergencyReadyCount: Int,
    totalContacts: Int,
    onDismiss: () -> Unit
) {
    val isReady = emergencyReadyCount >= 2
    val cardColor = if (isReady) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isReady) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isReady) "Emergency Ready" else "Setup Needed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isReady) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                Text(
                    text = "$emergencyReadyCount of $totalContacts contacts ready for emergencies",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isReady) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
                if (!isReady) {
                    Text(
                        text = "Add at least 2 verified emergency contacts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = if (isReady) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        }
    }
}

@Composable
private fun EmergencyContactCard(
    contact: EmergencyContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onVerify: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showActions = !showActions }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                // Contact type icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getContactTypeColor(contact.contactType),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getContactTypeIcon(contact.contactType),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = contact.getDisplayName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (contact.isHighPriority()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "HIGH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (contact.isVerified) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Verified",
                                tint = Color.Green,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = contact.relationship,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = contact.getFormattedPhoneNumber(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (contact.notes.isNotBlank()) {
                        Text(
                            text = contact.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Priority indicator
                Text(
                    text = "#${contact.priority}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action buttons
            AnimatedVisibility(visible = showActions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!contact.isVerified) {
                        OutlinedButton(
                            onClick = onVerify,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Verify")
                        }
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContactsState(
    onAddContact: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ContactPhone,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Emergency Contacts",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add trusted contacts who can help you in emergency situations",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onAddContact) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add First Contact")
        }
    }
}

// Helper functions for contact type styling
private fun getContactTypeIcon(type: ContactType) = when (type) {
    ContactType.TRUSTED_PERSON -> Icons.Default.Person
    ContactType.PROFESSIONAL -> Icons.Default.Work
    ContactType.EMERGENCY_SERVICE -> Icons.Default.Emergency
    ContactType.CRISIS_HOTLINE -> Icons.Default.Phone
    ContactType.LEGAL_ADVOCATE -> Icons.Default.Gavel
    ContactType.MEDICAL_PROVIDER -> Icons.Default.LocalHospital
    ContactType.COMMUNITY_RESOURCE -> Icons.Default.Group
    ContactType.OTHER -> Icons.Default.ContactPhone
}

private fun getContactTypeColor(type: ContactType) = when (type) {
    ContactType.TRUSTED_PERSON -> Color(0xFF4CAF50)
    ContactType.PROFESSIONAL -> Color(0xFF2196F3)
    ContactType.EMERGENCY_SERVICE -> Color(0xFFF44336)
    ContactType.CRISIS_HOTLINE -> Color(0xFF9C27B0)
    ContactType.LEGAL_ADVOCATE -> Color(0xFF607D8B)
    ContactType.MEDICAL_PROVIDER -> Color(0xFF00BCD4)
    ContactType.COMMUNITY_RESOURCE -> Color(0xFFFF9800)
    ContactType.OTHER -> Color(0xFF795548)
}