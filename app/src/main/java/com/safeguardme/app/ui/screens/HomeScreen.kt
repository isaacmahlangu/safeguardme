// ui/screens/HomeScreen.kt
package com.safeguardme.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VoiceChat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.safeguardme.app.data.models.Incident
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.data.models.SeverityLevel
import com.safeguardme.app.navigation.Screen
import com.safeguardme.app.ui.viewmodels.EmergencyContactViewModel
import com.safeguardme.app.ui.viewmodels.HomeUiState
import com.safeguardme.app.ui.viewmodels.HomeViewModel
import com.safeguardme.app.ui.viewmodels.TriggerViewModel
import com.safeguardme.app.ui.viewmodels.firstName
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    emergencyViewModel: EmergencyContactViewModel = hiltViewModel(),
    triggerViewModel: TriggerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val emergencyContacts by emergencyViewModel.emergencyReadyContacts.collectAsState()

    // Trigger setup state
    var showTriggerSetup by remember { mutableStateOf(false) }

    // Memoized navigation actions
    val navigationActions = remember(navController) {
        HomeNavigationActions(navController)
    }

    // Memoized UI handlers
    val uiHandlers = remember(viewModel) {
        HomeUiHandlers(
            onToggleSafety = viewModel::toggleSafetyStatus,
            onClearError = viewModel::clearError,
            onRefresh = viewModel::refresh,
            onToggleTriggerSetup = { showTriggerSetup = !showTriggerSetup }
        )
    }

    HomeContent(
        uiState = uiState,
        emergencyContactsCount = emergencyContacts.size,
        navigationActions = navigationActions,
        uiHandlers = uiHandlers,
        showTriggerSetup = showTriggerSetup,
        triggerViewModel = triggerViewModel
    )

    // Handle refresh trigger
    LaunchedEffect(uiState.isRefreshing) {
        if (uiState.isRefreshing) {
            uiHandlers.onRefresh()
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    emergencyContactsCount: Int,
    navigationActions: HomeNavigationActions,
    uiHandlers: HomeUiHandlers,
    showTriggerSetup: Boolean,
    triggerViewModel: TriggerViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HomeHeader(
                userName = uiState.user?.firstName ?: "User",
                navigationActions = navigationActions
            )
        }

        item {
            SafetyStatusCard(
                safetyStatus = uiState.safetyStatus,
                onToggleSafety = uiHandlers.onToggleSafety
            )
        }

        item {
            QuickActionsSection(
                safetyStatus = uiState.safetyStatus,
                navigationActions = navigationActions,
                onToggleTriggerSetup = uiHandlers.onToggleTriggerSetup,
                showTriggerSetup = showTriggerSetup
            )
        }

        // Voice Trigger Setup Section
        item {
            AnimatedVisibility(
                visible = showTriggerSetup,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                VoiceTriggerSetupCard(
                    triggerViewModel = triggerViewModel,
                    onDismiss = { uiHandlers.onToggleTriggerSetup() }
                )
            }
        }

        item {
            ServicesSection(
                emergencyContactsCount = emergencyContactsCount,
                navigationActions = navigationActions
            )
        }

        item {
            RecentIncidentsHeader(
                hasIncidents = uiState.recentIncidents.isNotEmpty(),
                onViewAll = { navigationActions.navigateToIncidentHistory() }
            )
        }

        when {
            uiState.isLoading -> item { LoadingState() }
            uiState.error != null -> item {
                ErrorState(
                    error = uiState.error,
                    onDismiss = uiHandlers.onClearError
                )
            }
            uiState.recentIncidents.isEmpty() -> item { EmptyIncidentsState() }
            else -> items(uiState.recentIncidents) { incident ->
                IncidentCard(
                    incident = incident,
                    onClick = { /* TODO: Navigate to incident detail */ }
                )
            }
        }
    }
}

@Composable
private fun VoiceTriggerSetupCard(
    triggerViewModel: TriggerViewModel,
    onDismiss: () -> Unit
) {
    // Form states
    val keyword by triggerViewModel.keyword.collectAsState()
    val keywordError by triggerViewModel.keywordError.collectAsState()

    // Recording states
    val isRecording by triggerViewModel.isRecording.collectAsState()
    val recordingTime by triggerViewModel.recordingTime.collectAsState()
    val amplitude by triggerViewModel.amplitude.collectAsState()
    val hasRecording by triggerViewModel.hasRecording.collectAsState()

    // Playback states
    val isPlaying by triggerViewModel.isPlaying.collectAsState()

    // Save states
    val isSaving by triggerViewModel.isSaving.collectAsState()
    val canSave by triggerViewModel.canSave.collectAsState()

    // Detection status
    val detectionEnabled by triggerViewModel.detectionEnabled.collectAsState()

    // UI states
    val error by triggerViewModel.error.collectAsState()
    val successMessage by triggerViewModel.successMessage.collectAsState()

    // Animations
    val recordButtonScale by animateFloatAsState(
        targetValue = if (isRecording) 1.2f else 1f,
        animationSpec = tween(200), label = "record_scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VoiceChat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Voice Trigger Setup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ExpandLess, contentDescription = "Collapse")
                }
            }

            // Detection Status
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (detectionEnabled)
                    MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (detectionEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = null,
                        tint = if (detectionEnabled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Detection: ${if (detectionEnabled) "ON" else "OFF"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (detectionEnabled)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Keyword Input
            OutlinedTextField(
                value = keyword,
                onValueChange = triggerViewModel::updateKeyword,
                label = { Text("Secret Keyword") },
                placeholder = { Text("e.g., Safeguard, Phoenix") },
                isError = keywordError != null,
                supportingText = keywordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                leadingIcon = {
                    Icon(Icons.Default.Key, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRecording && !isSaving
            )

            // Recording Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Voice Sample",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    if (!isRecording && !hasRecording) {
                        Text(
                            text = "Record yourself saying your keyword clearly",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Waveform or Recording Status
                    if (isRecording) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WaveformVisualizer(
                                amplitude = amplitude,
                                isActive = isRecording,
                                modifier = Modifier.height(40.dp)
                            )
                            Text(
                                text = "Recording... ${recordingTime}s / 5s",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (hasRecording) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Green,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Recording complete (${recordingTime}s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Green
                            )
                        }
                    }

                    // Recording Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Record Button
                        FloatingActionButton(
                            onClick = {
                                if (isRecording) {
                                    triggerViewModel.stopRecording()
                                } else {
                                    triggerViewModel.startRecording()
                                }
                            },
                            modifier = Modifier
                                .scale(recordButtonScale)
                                .size(48.dp),
                            containerColor = if (isRecording)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Playback Button
                        if (hasRecording) {
                            IconButton(
                                onClick = { triggerViewModel.playRecording() },
                                enabled = !isRecording
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop playback" else "Play recording",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Delete Button
                        if (hasRecording) {
                            IconButton(
                                onClick = { triggerViewModel.deleteRecording() },
                                enabled = !isRecording && !isPlaying
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete recording",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = { triggerViewModel.saveKeywordAndSample() },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSave
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isSaving) "Saving..." else "Save Voice Trigger"
                )
            }

            // Success/Error Messages
            successMessage?.let { message ->
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(3000)
                    triggerViewModel.clearSuccessMessage()
                    onDismiss()
                }

                Surface(
                    color = Color.Green.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green
                    )
                }
            }

            error?.let { errorMessage ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { triggerViewModel.clearError() }
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformVisualizer(
    amplitude: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animatedAmplitude by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isActive) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ), label = "amplitude"
    )

    Canvas(
        modifier = modifier.fillMaxWidth()
    ) {
        drawWaveform(
            amplitude = if (isActive) amplitude.toFloat() else 0f,
            animatedAmplitude = animatedAmplitude,
            color = androidx.compose.ui.graphics.Color.Blue
        )
    }
}

private fun DrawScope.drawWaveform(
    amplitude: Float,
    animatedAmplitude: Float,
    color: androidx.compose.ui.graphics.Color
) {
    val width = size.width
    val height = size.height
    val centerY = height / 2
    val barCount = 15
    val barWidth = width / barCount / 2

    for (i in 0 until barCount) {
        val x = (i * width / barCount) + barWidth / 2
        val normalizedAmplitude = (amplitude / 1000f).coerceIn(0f, 1f)
        val waveHeight = (sin(i * 0.5f + animatedAmplitude * 10) * normalizedAmplitude * height / 3).coerceAtLeast(4f)

        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(x - barWidth / 2, centerY - waveHeight / 2),
            size = androidx.compose.ui.geometry.Size(barWidth, waveHeight)
        )
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    navigationActions: HomeNavigationActions
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hi, $userName",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Stay safe and protected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OverflowMenu(
            expanded = showOverflowMenu,
            onExpandedChange = { showOverflowMenu = it },
            navigationActions = navigationActions
        )
    }
}

@Composable
private fun OverflowMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    navigationActions: HomeNavigationActions
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            MenuOption(
                text = "Profile",
                icon = Icons.Default.Person,
                onClick = {
                    onExpandedChange(false)
                    navigationActions.navigateToProfile()
                }
            )
            MenuOption(
                text = "Incident History",
                icon = Icons.Default.History,
                onClick = {
                    onExpandedChange(false)
                    navigationActions.navigateToIncidentHistory()
                }
            )
            Divider()
            MenuOption(
                text = "Sign Out",
                icon = Icons.Default.ExitToApp,
                onClick = {
                    onExpandedChange(false)
                    navigationActions.signOut()
                }
            )
        }
    }
}

@Composable
private fun MenuOption(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        leadingIcon = { Icon(icon, contentDescription = null) }
    )
}

@Composable
private fun SafetyStatusCard(
    safetyStatus: SafetyStatus,
    onToggleSafety: () -> Unit
) {
    val statusConfig = remember(safetyStatus) {
        SafetyStatusConfig.from(safetyStatus)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusConfig.backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = statusConfig.icon,
                    contentDescription = null,
                    tint = statusConfig.textColor
                )
                Text(
                    text = "Safety Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusConfig.statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = statusConfig.textColor,
                fontWeight = FontWeight.Medium
            )

            if (safetyStatus == SafetyStatus.ENABLED) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onToggleSafety,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disable Emergency Mode")
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    safetyStatus: SafetyStatus,
    navigationActions: HomeNavigationActions,
    onToggleTriggerSetup: () -> Unit,
    showTriggerSetup: Boolean
) {
    Text(
        text = "Quick Actions",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            icon = Icons.Default.Contacts,
            text = "Contacts",
            onClick = { navigationActions.navigateToContacts() },
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.Add,
            text = "Report",
            onClick = { navigationActions.navigateToIncidentReport() },
            modifier = Modifier.weight(1f)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            icon = Icons.Default.History,
            text = "History",
            onClick = { navigationActions.navigateToIncidentHistory() },
            modifier = Modifier.weight(1f)
        )
        VoiceTriggerToggleButton(
            onClick = onToggleTriggerSetup,
            isExpanded = showTriggerSetup,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(text, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun VoiceTriggerToggleButton(
    onClick: () -> Unit,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.VoiceChat,
                contentDescription = null
            )
            Text("Voice Setup", textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ServicesSection(
    emergencyContactsCount: Int,
    navigationActions: HomeNavigationActions
) {
    ServiceCard(
        title = "Emergency Contacts",
        subtitle = "$emergencyContactsCount contacts ready",
        icon = Icons.Default.ContactPhone,
        onClick = { navigationActions.navigateToEmergencyContacts() }
    )

    ServiceCard(
        title = "AI Assistant",
        subtitle = "Get help and guidance",
        icon = Icons.Default.Psychology,
        onClick = { navigationActions.navigateToAIAssistant() }
    )
}

@Composable
private fun ServiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun RecentIncidentsHeader(
    hasIncidents: Boolean,
    onViewAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recent Incidents",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (hasIncidents) {
            TextButton(onClick = onViewAll) {
                Text("View All")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Error",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun EmptyIncidentsState() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.Green
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No incidents reported",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Stay safe and report any incidents when needed",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IncidentCard(
    incident: Incident,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = remember(incident.date) {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(incident.date.toDate())
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                SeverityChip(severity = incident.severityLevel)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = remember(incident.description) {
                    incident.description.take(100).let { truncated ->
                        if (incident.description.length > 100) "$truncated..." else truncated
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (incident.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📍 ${incident.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SeverityChip(severity: SeverityLevel) {
    val config = remember(severity) {
        SeverityConfig.from(severity)
    }

    Surface(
        color = config.color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = config.text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = config.color,
            fontWeight = FontWeight.Medium
        )
    }
}

// Data classes for configuration
private data class SafetyStatusConfig(
    val statusText: String,
    val textColor: Color,
    val backgroundColor: Color,
    val icon: ImageVector
) {
    companion object {
        fun from(status: SafetyStatus): SafetyStatusConfig = when (status) {
            SafetyStatus.ENABLED -> SafetyStatusConfig(
                statusText = "Emergency Mode Active",
                textColor = Color(0xFFD32F2F),
                backgroundColor = Color(0xFFFFEBEE),
                icon = Icons.Default.Warning
            )
            SafetyStatus.EMERGENCY -> SafetyStatusConfig(
                statusText = "Emergency Triggered",
                textColor = Color(0xFFD32F2F),
                backgroundColor = Color(0xFFFFEBEE),
                icon = Icons.Default.Emergency
            )
            SafetyStatus.DISABLED -> SafetyStatusConfig(
                statusText = "System Ready",
                textColor = Color(0xFF388E3C),
                backgroundColor = Color(0xFFE8F5E8),
                icon = Icons.Default.Shield
            )
        }
    }
}

private data class SeverityConfig(
    val text: String,
    val color: Color
) {
    companion object {
        fun from(severity: SeverityLevel): SeverityConfig = when (severity) {
            SeverityLevel.LOW -> SeverityConfig("Low", Color(0xFF4CAF50))
            SeverityLevel.MEDIUM -> SeverityConfig("Medium", Color(0xFFFF9800))
            SeverityLevel.HIGH -> SeverityConfig("High", Color(0xFFF44336))
            SeverityLevel.CRITICAL -> SeverityConfig("Critical", Color(0xFF9C27B0))
        }
    }
}

// Navigation actions wrapper
private class HomeNavigationActions(private val navController: NavController) {
    fun navigateToProfile() = navController.navigate(Screen.Profile.route)
    fun navigateToIncidentHistory() = navController.navigate(Screen.IncidentHistory.route)
    fun navigateToContacts() = navController.navigate(Screen.Contacts.route)
    fun navigateToIncidentReport() = navController.navigate(Screen.IncidentReport.route)
    fun navigateToEmergencyContacts() = navController.navigate(Screen.EmergencyContacts.route)
    fun navigateToAIAssistant() = navController.navigate(Screen.AIAssistant.route)
    fun signOut() {
        navController.navigate(Screen.Login.route) {
            popUpTo(0) { inclusive = true }
        }
    }
}

// UI handlers wrapper
private class HomeUiHandlers(
    val onToggleSafety: () -> Unit,
    val onClearError: () -> Unit,
    val onRefresh: () -> Unit,
    val onToggleTriggerSetup: () -> Unit
)