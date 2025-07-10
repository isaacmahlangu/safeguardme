// ui/screens/IncidentReportScreen.kt
package com.safeguardme.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.safeguardme.app.data.models.IncidentType
import com.safeguardme.app.data.models.SeverityLevel
import com.safeguardme.app.ui.viewmodels.IncidentReportViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentReportScreen(
    navController: NavController,
    viewModel: IncidentReportViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // Form states
    val location by viewModel.location.collectAsState()
    val description by viewModel.description.collectAsState()
    val incidentType by viewModel.incidentType.collectAsState()
    val severityLevel by viewModel.severityLevel.collectAsState()
    val incidentDate by viewModel.incidentDate.collectAsState()
    val submittedToSAPS by viewModel.submittedToSAPS.collectAsState()
    val submittedToNGO by viewModel.submittedToNGO.collectAsState()

    // Evidence states
    val selectedImages by viewModel.selectedImages.collectAsState()
    val imageUploadProgress by viewModel.imageUploadProgress.collectAsState()

    // UI states
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val submitProgress by viewModel.submitProgress.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    val descriptionCharCount by viewModel.descriptionCharCount.collectAsState()

    // Validation states
    val locationError by viewModel.locationError.collectAsState()
    val descriptionError by viewModel.descriptionError.collectAsState()

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addImage(it) }
    }

    // Date picker
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Report Incident",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Security Notice
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                text = "Secure Evidence Recording",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Your incident report and evidence are encrypted and stored securely. Only you can access this information.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Incident Type Selection
            item {
                Text(
                    text = "Incident Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(IncidentType.values()) { type ->
                        IncidentTypeChip(
                            type = type,
                            isSelected = incidentType == type,
                            onClick = { viewModel.updateIncidentType(type) }
                        )
                    }
                }
            }

            // Severity Level
            item {
                Text(
                    text = "Severity Level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SeverityLevel.values()) { level ->
                        SeverityLevelChip(
                            level = level,
                            isSelected = severityLevel == level,
                            onClick = { viewModel.updateSeverityLevel(level) }
                        )
                    }
                }
            }

            // Date Selection
            item {
                OutlinedTextField(
                    value = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(incidentDate),
                    onValueChange = { },
                    label = { Text("Date & Time") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Location Field
            item {
                OutlinedTextField(
                    value = location,
                    onValueChange = viewModel::updateLocation,
                    label = { Text("Location") },
                    placeholder = { Text("Where did this incident occur?") },
                    isError = locationError != null,
                    supportingText = locationError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Description Field
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Description") },
                    placeholder = { Text("Describe what happened. Include as much detail as possible.") },
                    isError = descriptionError != null,
                    supportingText = {
                        Column {
                            descriptionError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                            Text(
                                text = descriptionCharCount,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Evidence Section
            item {
                Text(
                    text = "Evidence",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Image Evidence
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Add Image Button
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { imagePickerLauncher.launch("image/*") }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Add Photo Evidence",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tap to select images from gallery or camera",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Selected Images
                    if (selectedImages.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedImages) { uri ->
                                EvidenceImageCard(
                                    uri = uri,
                                    uploadProgress = imageUploadProgress[uri] ?: 0f,
                                    onRemove = { viewModel.removeImage(uri) }
                                )
                            }
                        }
                    }
                }
            }

            // Legal Reporting Status
            item {
                Text(
                    text = "Legal Reporting Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Submitted to SAPS",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Has this been reported to police?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = submittedToSAPS,
                            onCheckedChange = viewModel::updateSubmittedToSAPS
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Submitted to NGO",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Has this been reported to an organization?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = submittedToNGO,
                            onCheckedChange = viewModel::updateSubmittedToNGO
                        )
                    }
                }
            }

            // Submit Button
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isSubmitting) {
                        LinearProgressIndicator(
                            progress = submitProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Submitting incident report... ${(submitProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { viewModel.submitIncident() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isFormValid && !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isSubmitting) "Submitting..." else "Submit Incident Report"
                        )
                    }
                }
            }

            // Bottom padding
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Success Message
        successMessage?.let { message ->
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearSuccessMessage()
                navController.navigateUp()
            }

            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(message)
            }
        }

        // Error Message
        error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(errorMessage)
            }
        }

        // Date Picker
        if (showDatePicker) {
            // TODO: Implement date picker dialog
            // This would require additional dependencies or custom implementation
        }
    }
}

@Composable
private fun IncidentTypeChip(
    type: IncidentType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val text = when (type) {
        IncidentType.PHYSICAL_VIOLENCE -> "Physical"
        IncidentType.EMOTIONAL_ABUSE -> "Emotional"
        IncidentType.SEXUAL_VIOLENCE -> "Sexual"
        IncidentType.ECONOMIC_ABUSE -> "Economic"
        IncidentType.STALKING -> "Stalking"
        IncidentType.HARASSMENT -> "Harassment"
        IncidentType.THREATS -> "Threats"
        IncidentType.OTHER -> "Other"
    }

    FilterChip(
        onClick = onClick,
        label = { Text(text) },
        selected = isSelected
    )
}

@Composable
private fun SeverityLevelChip(
    level: SeverityLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (text, color) = when (level) {
        SeverityLevel.LOW -> "Low" to Color(0xFF4CAF50)
        SeverityLevel.MEDIUM -> "Medium" to Color(0xFFFF9800)
        SeverityLevel.HIGH -> "High" to Color(0xFFF44336)
        SeverityLevel.CRITICAL -> "Critical" to Color(0xFF9C27B0)
    }

    FilterChip(
        onClick = onClick,
        label = {
            Text(
                text = text,
                color = if (isSelected) Color.White else color
            )
        },
        selected = isSelected,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color
        )
    )
}

@Composable
private fun EvidenceImageCard(
    uri: Uri,
    uploadProgress: Float,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.size(80.dp)
    ) {
        Box {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Upload progress overlay
            if (uploadProgress > 0f && uploadProgress < 1f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = uploadProgress,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                )
            }
        }
    }
}