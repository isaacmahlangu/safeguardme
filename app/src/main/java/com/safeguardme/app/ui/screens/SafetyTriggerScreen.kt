// ui/screens/SafetyTriggerScreen.kt
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.ui.viewmodels.SafetyTriggerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyTriggerScreen(
    navController: NavController,
    viewModel: SafetyTriggerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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



    // Animation states
    val buttonScale by animateFloatAsState(
        targetValue = if (safetyStatus == SafetyStatus.ENABLED) 1.1f else 1f,
        animationSpec = tween(300), label = "button_scale"
    )

    val buttonColor by animateColorAsState(
        targetValue = when (safetyStatus) {
            SafetyStatus.DISABLED -> MaterialTheme.colorScheme.primary
            SafetyStatus.ENABLED -> Color(0xFFE53E3E)
            SafetyStatus.EMERGENCY -> Color(0xFFD32F2F)
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
            .padding(24.dp)
    ) {
        // Top Bar
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {

            // Mic Status Pill
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff, // Phase 1: Always off
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = micStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Central Safety Button
            Box(
                contentAlignment = Alignment.Center
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
                        // Haptic feedback
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
                                imageVector = when (safetyStatus) {
                                    SafetyStatus.DISABLED -> Icons.Default.Security
                                    SafetyStatus.ENABLED -> Icons.Default.Warning
                                    SafetyStatus.EMERGENCY -> Icons.Default.Emergency
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

            // Status Message
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = when (safetyStatus) {
                    SafetyStatus.DISABLED -> MaterialTheme.colorScheme.onSurface
                    SafetyStatus.ENABLED -> Color(0xFFD32F2F)
                    SafetyStatus.EMERGENCY -> Color(0xFFB71C1C)
                },
                fontWeight = if (safetyStatus != SafetyStatus.DISABLED) FontWeight.SemiBold else FontWeight.Normal
            )

            if (safetyStatus == SafetyStatus.DISABLED) {
                GestureTriggersCard(
                    volumeEnabled = volumeButtonTriggerEnabled,
                    shakeEnabled = shakeTriggerEnabled,
                    powerEnabled = powerButtonTriggerEnabled,
                    onVolumeToggle = viewModel::toggleVolumeButtonTrigger,
                    onShakeToggle = viewModel::toggleShakeTrigger,
                    onPowerToggle = viewModel::togglePowerButtonTrigger
                )
            }

            // Emergency Escalation Button (only when safety enabled)
            if (safetyStatus == SafetyStatus.ENABLED) {
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

            // Disable Button (only when safety enabled)
            if (safetyStatus == SafetyStatus.ENABLED || safetyStatus == SafetyStatus.EMERGENCY) {
                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = { viewModel.onSafetyButtonPressed() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disable Safety Mode")
                }
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
    }



}

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

@Composable
private fun GestureTriggersCard(
    volumeEnabled: Boolean,
    shakeEnabled: Boolean,
    powerEnabled: Boolean,
    onVolumeToggle: () -> Unit,
    onShakeToggle: () -> Unit,
    onPowerToggle: () -> Unit
) {
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
            Text(
                text = "🎭 Gesture Triggers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Trigger safety mode discreetly using phone gestures",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            GestureTriggerItem(
                icon = "🔊",
                title = "Volume Buttons",
                description = "Press volume up/down 3 times rapidly",
                enabled = volumeEnabled,
                onToggle = onVolumeToggle
            )

            GestureTriggerItem(
                icon = "📳",
                title = "Phone Shake",
                description = "Shake phone vigorously for 2 seconds",
                enabled = shakeEnabled,
                onToggle = onShakeToggle
            )

            GestureTriggerItem(
                icon = "⚡",
                title = "Power Button",
                description = "Press power button 5 times quickly",
                enabled = powerEnabled,
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
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() }
        )
    }
}

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