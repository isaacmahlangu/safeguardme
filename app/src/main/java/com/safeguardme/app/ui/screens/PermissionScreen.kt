// ui/screens/PermissionScreen.kt
package com.safeguardme.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safeguardme.app.utils.AppPermission
import com.safeguardme.app.utils.CriticalityLevel
import com.safeguardme.app.utils.PermissionManager
import com.safeguardme.app.utils.PermissionStatus
import com.safeguardme.app.ui.viewmodels.PermissionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    onPermissionsConfigured: () -> Unit,
    permissionManager: PermissionManager,
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val permissionsState by permissionManager.permissionsState.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val showRationale by viewModel.showRationale.collectAsState()

    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.initialize(permissionManager, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "App Permissions",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Header section
            item {
                PermissionHeaderCard(
                    currentStep = currentStep,
                    totalSteps = 3,
                    permissionsState = permissionsState
                )
            }

            // Current step content
            when (currentStep) {
                PermissionStep.INTRODUCTION -> {
                    item {
                        IntroductionSection(
                            onNext = { viewModel.nextStep() }
                        )
                    }
                }

                PermissionStep.ESSENTIAL_PERMISSIONS -> {
                    item {
                        PermissionStepHeader(
                            title = "Essential Permissions",
                            subtitle = "Required for core safety features",
                            icon = Icons.Default.Security
                        )
                    }

                    items(AppPermission.values().filter { it.criticalityLevel == CriticalityLevel.ESSENTIAL }) { permission ->
                        PermissionCard(
                            permission = permission,
                            isGranted = when (permission) {
                                AppPermission.LOCATION -> permissionsState.locationGranted
                                AppPermission.CAMERA -> permissionsState.cameraGranted
                                else -> false
                            },
                            onRequestPermission = {
                                isProcessing = true
                                viewModel.requestPermission(permission) {
                                    isProcessing = false
                                }
                            },
                            showRationale = showRationale[permission] == true
                        )
                    }

                    item {
                        StepActionButton(
                            text = if (permissionsState.essentialPermissionsGranted) "Continue" else "Skip for Now",
                            isEnabled = !isProcessing,
                            isPrimary = permissionsState.essentialPermissionsGranted,
                            onClick = { viewModel.nextStep() }
                        )
                    }
                }

                PermissionStep.ENHANCED_PERMISSIONS -> {
                    item {
                        PermissionStepHeader(
                            title = "Enhanced Features",
                            subtitle = "Optional permissions for advanced functionality",
                            icon = Icons.Default.Star
                        )
                    }

                    items(AppPermission.values().filter { it.criticalityLevel == CriticalityLevel.ENHANCED }) { permission ->
                        PermissionCard(
                            permission = permission,
                            isGranted = when (permission) {
                                AppPermission.AUDIO_RECORDING -> permissionsState.audioGranted
                                AppPermission.PHONE_CALLS -> permissionsState.phoneGranted
                                else -> false
                            },
                            onRequestPermission = {
                                isProcessing = true
                                viewModel.requestPermission(permission) {
                                    isProcessing = false
                                }
                            },
                            showRationale = showRationale[permission] == true
                        )
                    }

                    item {
                        StepActionButton(
                            text = "Continue to App",
                            isEnabled = !isProcessing,
                            isPrimary = true,
                            onClick = onPermissionsConfigured
                        )
                    }
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PermissionHeaderCard(
    currentStep: PermissionStep,
    totalSteps: Int,
    permissionsState: com.safeguardme.app.utils.PermissionsState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = (currentStep.ordinal + 1) / totalSteps.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Secure Your Safety",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Grant permissions to unlock SafeguardMe's full protection capabilities",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Permission status summary
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusChip(
                    label = "Essential",
                    isGranted = permissionsState.essentialPermissionsGranted,
                    count = "2/2"
                )
                StatusChip(
                    label = "Enhanced",
                    isGranted = permissionsState.audioGranted && permissionsState.phoneGranted,
                    count = "${if (permissionsState.audioGranted) 1 else 0 + if (permissionsState.phoneGranted) 1 else 0}/2"
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    isGranted: Boolean,
    count: String
) {
    Surface(
        color = if (isGranted) Color.Green.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isGranted) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$label ($count)",
                style = MaterialTheme.typography.labelSmall,
                color = if (isGranted) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IntroductionSection(
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your Privacy is Protected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SafeguardMe needs certain permissions to protect you effectively. All data is encrypted and stored securely on your device.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Security promises
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecurityPromiseRow(
                    icon = Icons.Default.Lock,
                    text = "End-to-end encryption for all data"
                )
                SecurityPromiseRow(
                    icon = Icons.Default.Visibility,
                    text = "You control what data is shared"
                )
                SecurityPromiseRow(
                    icon = Icons.Default.Delete,
                    text = "Data can be deleted anytime"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
                Spacer(modifier = Modifier.width(8.dp))
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
private fun SecurityPromiseRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionStepHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionCard(
    permission: AppPermission,
    isGranted: Boolean,
    onRequestPermission: () -> Unit,
    showRationale: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                Color.Green.copy(alpha = 0.05f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                        imageVector = getPermissionIcon(permission),
                        contentDescription = null,
                        tint = if (isGranted) Color.Green else MaterialTheme.colorScheme.primary
                    )

                    Column {
                        Text(
                            text = permission.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = permission.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = Color.Green
                    )
                } else {
                    FilledTonalButton(
                        onClick = onRequestPermission
                    ) {
                        Text("Grant")
                    }
                }
            }

            // Expandable rationale
            AnimatedVisibility(
                visible = showRationale && !isGranted,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = permission.rationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepActionButton(
    text: String,
    isEnabled: Boolean,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = isEnabled
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = isEnabled
        ) {
            Text(text)
        }
    }
}

private fun getPermissionIcon(permission: AppPermission): ImageVector {
    return when (permission) {
        AppPermission.LOCATION -> Icons.Default.LocationOn
        AppPermission.CAMERA -> Icons.Default.Camera
        AppPermission.AUDIO_RECORDING -> Icons.Default.Mic
        AppPermission.PHONE_CALLS -> Icons.Default.Phone
    }
}

enum class PermissionStep {
    INTRODUCTION,
    ESSENTIAL_PERMISSIONS,
    ENHANCED_PERMISSIONS
}