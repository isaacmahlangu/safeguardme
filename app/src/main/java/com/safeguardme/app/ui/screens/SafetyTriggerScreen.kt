// ui/screens/SafetyTriggerScreen.kt - Enhanced with Permission Management
package com.safeguardme.app.ui.screens

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat.performHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.managers.AppPermission
import com.safeguardme.app.ui.viewmodels.MonitoringStats
import com.safeguardme.app.ui.viewmodels.SafetyPermissionStatus
import com.safeguardme.app.ui.viewmodels.SafetyTriggerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyTriggerScreen(
    navController: NavController,
    viewModel: SafetyTriggerViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // State collection
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val showConfirmation by viewModel.showConfirmation.collectAsState()
    val confirmationTimeout by viewModel.confirmationTimeout.collectAsState()
    val safetyStatus by viewModel.safetyStatus.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val buttonText by viewModel.buttonText.collectAsState()
    val micStatusText by viewModel.micStatusText.collectAsState()
    val volumeButtonTriggerEnabled by viewModel.volumeButtonTriggerEnabled.collectAsState()
    val shakeTriggerEnabled by viewModel.shakeTriggerEnabled.collectAsState()
    val powerButtonTriggerEnabled by viewModel.powerButtonTriggerEnabled.collectAsState()

    // ✅ NEW: Permission state collection
    val permissionStatus by viewModel.permissionStatus.collectAsState()
    val permissionWarnings by viewModel.permissionWarnings.collectAsState()
    val canActivateSafety by viewModel.canActivateSafety.collectAsState()
    val canCollectFullEvidence by viewModel.canCollectFullEvidence.collectAsState()

    // Check permissions on screen entry
    LaunchedEffect(Unit) {
        viewModel.checkAllPermissions()
    }

    // Animation states
    val buttonScale by animateFloatAsState(
        targetValue = if (safetyStatus == SafetyStatus.ENABLED) 1.1f else 1f,
        animationSpec = tween(300), label = "button_scale"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            !canActivateSafety && safetyStatus == SafetyStatus.DISABLED -> Color.Yellow
            safetyStatus == SafetyStatus.DISABLED -> MaterialTheme.colorScheme.primary
            safetyStatus == SafetyStatus.ENABLED -> Color(0xFFE53E3E)
            safetyStatus == SafetyStatus.EMERGENCY -> Color(0xFFD32F2F)
            else -> MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(300), label = "button_color"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when (safetyStatus) {
            SafetyStatus.DISABLED -> MaterialTheme.colorScheme.background
            SafetyStatus.ENABLED -> Color(0xFFFFF5F5)
            SafetyStatus.EMERGENCY -> Color(0xFFFFEBEE)
        },
        animationSpec = tween(500), label = "background_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Bar
            item {
                TopAppBar(
                    title = {
                        Text(
                            "Emergency Safety",
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

            // ✅ NEW: Permission Status Card
            item {
                SafetyPermissionStatusCard(
                    permissionStatus = permissionStatus,
                    permissionWarnings = permissionWarnings,
                    onRequestPermission = { viewModel.showPermissionDialog(it) }
                )
            }

            // ✅ NEW: Safety Capability Overview
            if (!canCollectFullEvidence) {
                item {
                    SafetyCapabilityCard(
                        permissionStatus = permissionStatus,
                        onRequestPermission = { viewModel.showPermissionDialog(it) }
                    )
                }
            }

            // Mic Status Pill
            item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = when {
                        !permissionStatus.canRecordAudio && safetyStatus == SafetyStatus.DISABLED ->
                            Color.Yellow.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                safetyStatus != SafetyStatus.DISABLED && permissionStatus.canRecordAudio -> Icons.Default.Mic
                                else -> Icons.Default.MicOff
                            },
                            contentDescription = null,
                            tint = when {
                                !permissionStatus.canRecordAudio && safetyStatus == SafetyStatus.DISABLED -> Color.Yellow
                                safetyStatus != SafetyStatus.DISABLED -> Color.Red
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = micStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                !permissionStatus.canRecordAudio && safetyStatus == SafetyStatus.DISABLED -> Color.Yellow
                                safetyStatus != SafetyStatus.DISABLED -> Color.Red
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Central Safety Button
            item {
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Outer ring animation for emergency state
                    if (safetyStatus == SafetyStatus.ENABLED || safetyStatus == SafetyStatus.EMERGENCY) {
                        Surface(
                            modifier = Modifier.size(220.dp),
                            shape = CircleShape,
                            color = buttonColor.copy(alpha = 0.2f)
                        ) {}
                    }

                    // Main Safety Button
                    FilledIconButton(
                        onClick = {
                            performHapticFeedback(context)
                            viewModel.onSafetyButtonPressed()
                        },
                        modifier = Modifier
                            .size(180.dp)
                            .scale(buttonScale),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = buttonColor
                        ),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        !canActivateSafety && safetyStatus == SafetyStatus.DISABLED -> Icons.Default.Lock
                                        safetyStatus == SafetyStatus.DISABLED -> Icons.Default.Security
                                        safetyStatus == SafetyStatus.ENABLED -> Icons.Default.Warning
                                        safetyStatus == SafetyStatus.EMERGENCY -> Icons.Default.Emergency
                                        else -> Icons.Default.Security
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.White
                                )
                                Text(
                                    text = buttonText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Status Message
            item {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = when (safetyStatus) {
                        SafetyStatus.DISABLED -> {
                            if (canActivateSafety) MaterialTheme.colorScheme.onSurface
                            else Color.Yellow
                        }
                        SafetyStatus.ENABLED -> Color(0xFFD32F2F)
                        SafetyStatus.EMERGENCY -> Color(0xFFB71C1C)
                    },
                    fontWeight = if (safetyStatus != SafetyStatus.DISABLED) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Gesture Triggers (only when disabled)
            if (safetyStatus == SafetyStatus.DISABLED) {
                item {
                    EnhancedGestureTriggersCard(
                        volumeEnabled = volumeButtonTriggerEnabled,
                        shakeEnabled = shakeTriggerEnabled,
                        powerEnabled = powerButtonTriggerEnabled,
                        onVolumeToggle = viewModel::toggleVolumeButtonTrigger,
                        onShakeToggle = viewModel::toggleShakeTrigger,
                        onPowerToggle = viewModel::togglePowerButtonTrigger,
                        canActivateGestures = canActivateSafety,
                        onRequestPermissions = { viewModel.showPermissionDialog(AppPermission.AUDIO_RECORDING) }
                    )
                }
            }

            if (safetyStatus == SafetyStatus.DISABLED) {
                item {
                    /*VoiceDetectionToggleCard(
                        voiceDetectionEnabled = viewModel.voiceDetectionEnabled.collectAsState().value,
                        onToggleVoiceDetection = null,
                        canActivateVoiceDetection = canActivateSafety,
                        onRequestPermissions = { viewModel.showPermissionDialog(AppPermission.AUDIO_RECORDING) }
                    )*/
                }
            }

            // Emergency Escalation Button (only when safety enabled)
            if (safetyStatus == SafetyStatus.ENABLED) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.escalateToEmergency() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escalate to Emergency Services")
                    }
                }
            }

            // Disable Button (only when safety enabled)
            if (safetyStatus == SafetyStatus.ENABLED || safetyStatus == SafetyStatus.EMERGENCY) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.onSafetyButtonPressed() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disable Safety Mode")
                    }
                }
            }

            // ✅ NEW: Monitoring Stats (when active)
            if (safetyStatus != SafetyStatus.DISABLED) {
                item {
                    MonitoringStatsCard(
                        monitoringStats = viewModel.monitoringStats.collectAsState().value,
                        permissionStatus = permissionStatus
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Error Display
        error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(errorMessage)
            }
        }

        // Confirmation Dialog
        if (showConfirmation) {
            ConfirmationDialog(
                title = when (safetyStatus) {
                    SafetyStatus.DISABLED -> "Enable Safety Mode?"
                    SafetyStatus.ENABLED -> "Disable Safety Mode?"
                    SafetyStatus.EMERGENCY -> "Override Emergency Mode?"
                },
                message = when (safetyStatus) {
                    SafetyStatus.DISABLED -> "This will enable emergency monitoring and notify your trusted contacts if needed."
                    SafetyStatus.ENABLED -> "This will disable emergency monitoring. Are you sure you're safe?"
                    SafetyStatus.EMERGENCY -> "This will disable emergency mode. Only do this if you're safe."
                },
                confirmText = when (safetyStatus) {
                    SafetyStatus.DISABLED -> "Enable"
                    SafetyStatus.ENABLED -> "Disable"
                    SafetyStatus.EMERGENCY -> "Override"
                },
                timeoutSeconds = confirmationTimeout,
                onConfirm = {
                    performHapticFeedback(context, strong = true)
                    viewModel.confirmSafetyAction()
                },
                onCancel = { viewModel.cancelConfirmation() }
            )
        }

        // ✅ NEW: Permission Request Dialog
        PermissionRequestDialog(viewModel = viewModel)
    }
}

/**
 * ✅ NEW: Safety-specific permission status card
 */
@Composable
private fun SafetyPermissionStatusCard(
    permissionStatus: SafetyPermissionStatus,
    permissionWarnings: List<String>,
    onRequestPermission: (AppPermission) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                permissionStatus.hasAllOptimalPermissions() -> Color.Green.copy(alpha = 0.1f)
                permissionStatus.hasCriticalPermissions() -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> Color.Yellow.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        permissionStatus.hasAllOptimalPermissions() -> Icons.Default.CheckCircle
                        permissionStatus.hasCriticalPermissions() -> Icons.Default.Warning
                        else -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when {
                        permissionStatus.hasAllOptimalPermissions() -> Color.Green
                        permissionStatus.hasCriticalPermissions() -> Color.Yellow
                        else -> Color.Red
                    }
                )

                Text(
                    text = "Safety Capabilities",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Permission details
            SafetyPermissionRow(
                icon = Icons.Default.Mic,
                label = "Voice Evidence",
                isGranted = permissionStatus.canRecordAudio,
                isEssential = true,
                onRequest = { onRequestPermission(AppPermission.AUDIO_RECORDING) }
            )

            SafetyPermissionRow(
                icon = Icons.Default.LocationOn,
                label = "Location Tracking",
                isGranted = permissionStatus.canAccessLocation,
                isEssential = true,
                onRequest = { onRequestPermission(AppPermission.LOCATION) }
            )

            SafetyPermissionRow(
                icon = Icons.Default.Camera,
                label = "Photo Evidence",
                isGranted = permissionStatus.canTakePhotos,
                isEssential = false,
                onRequest = { onRequestPermission(AppPermission.CAMERA) }
            )

            SafetyPermissionRow(
                icon = Icons.Default.Save,
                label = "Evidence Storage",
                isGranted = permissionStatus.canSaveEvidence,
                isEssential = false,
                onRequest = { onRequestPermission(AppPermission.STORAGE) }
            )

            SafetyPermissionRow(
                icon = Icons.Default.Email,
                label = "SMS Messaging",
                isGranted = permissionStatus.canSendSMS,
                isEssential = false,
                onRequest = {
                    onRequestPermission(AppPermission.SMS_MESSAGING)
                }
            )

            // Warnings
            if (permissionWarnings.isNotEmpty()) {
                Divider()
                permissionWarnings.forEach { warning ->
                    Text(
                        text = "⚠️ $warning",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Yellow
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyPermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isGranted: Boolean,
    isEssential: Boolean,
    onRequest: () -> Unit
) {
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
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isGranted) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )

            if (isEssential) {
                Surface(
                    color = Color.Red.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "ESSENTIAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }

        if (isGranted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                modifier = Modifier.size(16.dp),
                tint = Color.Green
            )
        } else {
            OutlinedButton(
                onClick = onRequest,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Grant",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

/**
 * ✅ NEW: Safety capability overview card
 */
@Composable
private fun SafetyCapabilityCard(
    permissionStatus: SafetyPermissionStatus,
    onRequestPermission: (AppPermission) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Safety Features Available",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            val activeCapabilities = permissionStatus.getActiveCapabilities()
            val missingFeatures = permissionStatus.getMissingFeatures()

            if (activeCapabilities.isNotEmpty()) {
                Text(
                    text = "✅ Available: ${activeCapabilities.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Green.copy(red = 0.2f)
                )
            }

            if (missingFeatures.isNotEmpty()) {
                Text(
                    text = "⚠️ Limited: ${missingFeatures.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Yellow
                )
            }
        }
    }
}

/**
 * ✅ ENHANCED: Gesture triggers card with permission awareness
 */
@Composable
private fun EnhancedGestureTriggersCard(
    volumeEnabled: Boolean,
    shakeEnabled: Boolean,
    powerEnabled: Boolean,
    onVolumeToggle: () -> Unit,
    onShakeToggle: () -> Unit,
    onPowerToggle: () -> Unit,
    canActivateGestures: Boolean,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (canActivateGestures) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color.Yellow.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🎭 Gesture Triggers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (!canActivateGestures) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Yellow
                    )
                }
            }

            Text(
                text = if (canActivateGestures) {
                    "Trigger safety mode discreetly using phone gestures"
                } else {
                    "Grant essential permissions to enable gesture triggers"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (canActivateGestures) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    Color.Yellow
                }
            )

            if (!canActivateGestures) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                ) {
                    Text("Grant Permissions to Enable Gestures")
                }

                Divider()
            }

            GestureTriggerItem(
                icon = "🔊",
                title = "Volume Buttons",
                description = "Press volume up/down 3 times rapidly",
                enabled = volumeEnabled && canActivateGestures,
                canEnable = canActivateGestures,
                onToggle = onVolumeToggle
            )

            GestureTriggerItem(
                icon = "📳",
                title = "Phone Shake",
                description = "Shake phone vigorously for 2 seconds",
                enabled = shakeEnabled && canActivateGestures,
                canEnable = canActivateGestures,
                onToggle = onShakeToggle
            )

            GestureTriggerItem(
                icon = "⚡",
                title = "Power Button",
                description = "Press power button 5 times quickly",
                enabled = powerEnabled && canActivateGestures,
                canEnable = canActivateGestures,
                onToggle = onPowerToggle
            )
        }
    }
}

@Composable
private fun GestureTriggerItem(
    icon: String,
    title: String,
    description: String,
    enabled: Boolean,
    canEnable: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canEnable) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (canEnable) 1f else 0.6f
                    )
                )
            }
        }

        Switch(
            checked = enabled,
            onCheckedChange = { if (canEnable) onToggle() },
            enabled = canEnable
        )
    }
}

/**
 * ✅ NEW: Monitoring statistics card
 */
@Composable
private fun MonitoringStatsCard(
    monitoringStats: MonitoringStats,
    permissionStatus: SafetyPermissionStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Blue.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🔄 Active Monitoring",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Duration: ${monitoringStats.getDurationMinutes()}m",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Evidence: ${monitoringStats.evidenceCount} items",
                style = MaterialTheme.typography.bodySmall
            )

            if (permissionStatus.getActiveCapabilities().isNotEmpty()) {
                Text(
                    text = "Active: ${permissionStatus.getActiveCapabilities().joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Green.copy(red = 0.2f)
                )
            }
        }
    }
}

/**
 * ✅ NEW: Permission request dialog for safety features
 */
@Composable
private fun PermissionRequestDialog(
    viewModel: SafetyTriggerViewModel
) {
    val showPermissionRequest by viewModel.showPermissionRequest.collectAsState()

    showPermissionRequest?.let { permission ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionDialog() },
            icon = {
                Icon(
                    imageVector = when (permission) {
                        AppPermission.AUDIO_RECORDING -> Icons.Default.Mic
                        AppPermission.LOCATION -> Icons.Default.LocationOn
                        AppPermission.CAMERA -> Icons.Default.Camera
                        AppPermission.STORAGE -> Icons.Default.Save
                        else -> Icons.Default.Security
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("${permission.title} for Safety")
            },
            text = {
                Column {
                    Text(permission.rationale)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Safety impact without this permission:",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall
                    )

                    when (permission) {
                        AppPermission.AUDIO_RECORDING -> {
                            Text("• No voice evidence collection", style = MaterialTheme.typography.bodySmall)
                            Text("• Limited emergency documentation", style = MaterialTheme.typography.bodySmall)
                            Text("• Reduced evidence quality", style = MaterialTheme.typography.bodySmall)
                        }
                        AppPermission.LOCATION -> {
                            Text("• No location tracking for emergencies", style = MaterialTheme.typography.bodySmall)
                            Text("• Emergency contacts won't know your location", style = MaterialTheme.typography.bodySmall)
                            Text("• Limited incident context", style = MaterialTheme.typography.bodySmall)
                        }
                        AppPermission.CAMERA -> {
                            Text("• No photo evidence collection", style = MaterialTheme.typography.bodySmall)
                            Text("• Visual documentation unavailable", style = MaterialTheme.typography.bodySmall)
                        }
                        AppPermission.STORAGE -> {
                            Text("• Evidence may not be saved permanently", style = MaterialTheme.typography.bodySmall)
                            Text("• Limited data backup capabilities", style = MaterialTheme.typography.bodySmall)
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.requestPermission(permission) }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissPermissionDialog() }
                ) {
                    Text(
                        if (permission in listOf(AppPermission.AUDIO_RECORDING, AppPermission.LOCATION)) {
                            "Continue with Limited Safety"
                        } else {
                            "Continue Without"
                        }
                    )
                }
            }
        )
    }
}

// ✅ ENHANCED: Confirmation dialog (unchanged)
@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    timeoutSeconds: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(message)
                if (timeoutSeconds > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Auto-cancel in ${timeoutSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (confirmText == "Enable")
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

/**
 * ✅ NEW: Voice detection toggle card
 */
@Composable
private fun VoiceDetectionToggleCard(
    voiceDetectionEnabled: Boolean,
    onToggleVoiceDetection: () -> Unit,
    canActivateVoiceDetection: Boolean,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (canActivateVoiceDetection) {
                if (voiceDetectionEnabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color.Yellow.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (voiceDetectionEnabled && canActivateVoiceDetection)
                            Icons.Default.RecordVoiceOver else Icons.Default.MicOff,
                        contentDescription = null,
                        tint = if (voiceDetectionEnabled && canActivateVoiceDetection)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )

                    Column {
                        Text(
                            text = "🎙️ Always-On Voice Detection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (canActivateVoiceDetection) {
                                if (voiceDetectionEnabled) "Listening for your trigger word"
                                else "Activate to enable keyword detection"
                            } else {
                                "Grant microphone permission to enable"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (canActivateVoiceDetection && voiceDetectionEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Switch(
                    checked = voiceDetectionEnabled && canActivateVoiceDetection,
                    onCheckedChange = {
                        if (canActivateVoiceDetection) {
                            onToggleVoiceDetection()
                        } else {
                            onRequestPermissions()
                        }
                    },
                    enabled = canActivateVoiceDetection
                )
            }

            if (voiceDetectionEnabled && canActivateVoiceDetection) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🔒 Privacy: 100% on-device processing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "🔋 Battery impact: <1% per day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "📡 No data sent until emergency triggered",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (!canActivateVoiceDetection) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                ) {
                    Text("Grant Microphone Permission")
                }
            }
        }
    }
}

// ✅ UNCHANGED: Haptic feedback function
private fun performHapticFeedback(context: Context, strong: Boolean = false) {
    try {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val effect = if (strong) {
                VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            } else {
                VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (strong) 200 else 50)
        }
    } catch (e: Exception) {
        // Haptic feedback not available, continue silently
    }
}