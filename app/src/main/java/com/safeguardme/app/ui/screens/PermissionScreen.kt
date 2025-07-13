// ui/screens/PermissionScreen.kt - Updated for All Essential Permissions
package com.safeguardme.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.safeguardme.app.managers.AppPermission
import com.safeguardme.app.managers.PermissionManager
import com.safeguardme.app.ui.viewmodels.AppReadinessLevel
import com.safeguardme.app.ui.viewmodels.PermissionGroupStatus
import com.safeguardme.app.ui.viewmodels.PermissionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionScreen(
    onPermissionsConfigured: () -> Unit,
    permissionManager: PermissionManager,
    viewModel: PermissionViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // ✅ UPDATED: Collect all permission states
    val locationGranted by permissionManager.locationGranted.collectAsState()
    val cameraGranted by permissionManager.cameraGranted.collectAsState()
    val audioGranted by permissionManager.audioGranted.collectAsState()
    val phoneGranted by permissionManager.phoneGranted.collectAsState()
    val storageGranted by permissionManager.storageGranted.collectAsState()
    val smsGranted by permissionManager.smsGranted.collectAsState()

    val currentStep by viewModel.currentStep.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    // ✅ FIXED: Safe permission status that waits for initialization
    val essentialStatus by remember {
        derivedStateOf {
            if (viewModel.isInitialized()) {
                viewModel.getEssentialPermissionsStatus()
            } else {
                // Return safe default while ViewModel initializes
                PermissionGroupStatus(
                    grantedCount = 0,
                    totalCount = 5, // All 5 essential permissions
                    missingPermissions = emptySet()
                )
            }
        }
    }

    // Initialize permission manager
    LaunchedEffect(Unit) {
        permissionManager.initialize(context)
        viewModel.initialize(permissionManager, context)
    }

    // ✅ NEW: Helper function to check if permission is granted
    fun isPermissionGranted(permission: AppPermission): Boolean {
        return when (permission) {
            AppPermission.LOCATION -> locationGranted
            AppPermission.CAMERA -> cameraGranted
            AppPermission.AUDIO_RECORDING -> audioGranted
            AppPermission.PHONE_CALLS -> phoneGranted
            AppPermission.STORAGE -> storageGranted
            AppPermission.SMS_MESSAGING -> smsGranted
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Essential Permissions",
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

            // ✅ UPDATED: Header with all essential permissions count
            item {
                PermissionHeaderCard(
                    currentStep = currentStep,
                    totalSteps = if (viewModel.getEnhancedPermissions().isEmpty()) 2 else 3,
                    essentialStatus = essentialStatus
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
                            title = "Essential Safety Permissions",
                            subtitle = "All permissions are required for SafeguardMe to protect you effectively",
                            icon = Icons.Default.Security
                        )
                    }

                    // ✅ UPDATED: Show all essential permissions from ViewModel
                    items(viewModel.getEssentialPermissions().toList()) { permission ->
                        PermissionCard(
                            permission = permission,
                            isGranted = isPermissionGranted(permission),
                            isProcessing = viewModel.isPermissionProcessing(permission),
                            onRequestPermission = {
                                viewModel.requestPermission(permission)
                            },
                            shouldShowRationale = permissionManager.shouldShowRationale(permission)
                        )
                    }

                    item {
                        EssentialPermissionsActionSection(
                            essentialStatus = essentialStatus,
                            isProcessing = isProcessing,
                            canProceed = viewModel.canProceedToNextStep(),
                            onContinue = {
                                if (viewModel.getEnhancedPermissions().isEmpty()) {
                                    onPermissionsConfigured()
                                } else {
                                    viewModel.nextStep()
                                }
                            }
                        )
                    }
                }

                PermissionStep.ENHANCED_PERMISSIONS -> {
                    // ✅ UPDATED: Only show if enhanced permissions exist
                    if (viewModel.getEnhancedPermissions().isNotEmpty()) {
                        item {
                            PermissionStepHeader(
                                title = "Enhanced Features",
                                subtitle = "Optional permissions for advanced functionality",
                                icon = Icons.Default.Star
                            )
                        }

                        items(viewModel.getEnhancedPermissions().toList()) { permission ->
                            PermissionCard(
                                permission = permission,
                                isGranted = isPermissionGranted(permission),
                                isProcessing = viewModel.isPermissionProcessing(permission),
                                onRequestPermission = {
                                    viewModel.requestPermission(permission)
                                },
                                shouldShowRationale = permissionManager.shouldShowRationale(permission)
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
    essentialStatus: PermissionGroupStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (essentialStatus.isComplete()) {
                Color.Green.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
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
                color = if (essentialStatus.isComplete()) Color.Green else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Icon(
                imageVector = if (essentialStatus.isComplete()) Icons.Default.CheckCircle else Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (essentialStatus.isComplete()) Color.Green else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (essentialStatus.isComplete()) {
                    "Safety Permissions Complete"
                } else {
                    "Essential Safety Setup"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = if (essentialStatus.isComplete()) {
                    "SafeguardMe is ready to protect you with full access to safety features"
                } else {
                    "Grant all essential permissions to enable SafeguardMe's protection capabilities"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ UPDATED: Single essential permissions status
            StatusChip(
                label = "Essential Permissions",
                isGranted = essentialStatus.isComplete(),
                count = "${essentialStatus.grantedCount}/${essentialStatus.totalCount}",
                isComplete = essentialStatus.isComplete()
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    isGranted: Boolean,
    count: String,
    isComplete: Boolean
) {
    Surface(
        color = when {
            isComplete -> Color.Green.copy(alpha = 0.15f)
            isGranted -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isComplete) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$label ($count)",
                style = MaterialTheme.typography.labelSmall,
                color = if (isComplete) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
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
                text = "SafeguardMe needs essential permissions to protect you effectively. All data is encrypted and stored securely on your device.",
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

/**
 * ✅ NEW: Action section specifically for essential permissions
 */
@Composable
private fun EssentialPermissionsActionSection(
    essentialStatus: PermissionGroupStatus,
    isProcessing: Boolean,
    canProceed: Boolean,  // This will now always be true
    onContinue: () -> Unit,
    viewModel: PermissionViewModel = hiltViewModel()  // Add viewModel parameter
) {
    val readinessLevel by remember {
        derivedStateOf { viewModel.getAppReadinessLevel() }
    }

    val availableFeatures by remember {
        derivedStateOf { viewModel.getAvailableFeatures() }
    }

    val missingFeatures by remember {
        derivedStateOf { viewModel.getMissingFeatures() }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ✅ UPDATED: Show informational card instead of warning
        if (!essentialStatus.isComplete()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = when (readinessLevel) {
                                    AppReadinessLevel.PARTIALLY_READY -> "Partial Features Available"
                                    AppReadinessLevel.BASIC_READY -> "Basic Features Available"
                                    else -> "SafeguardMe Ready"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "You can start using SafeguardMe now. Grant additional permissions to unlock more safety features.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Show available features
                    if (availableFeatures.isNotEmpty()) {
                        Text(
                            text = "✅ Available features (${availableFeatures.size}):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        availableFeatures.take(3).forEach { feature ->
                            Text(
                                text = "• ${feature.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (availableFeatures.size > 3) {
                            Text(
                                text = "... and ${availableFeatures.size - 3} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    // Show missing features (limited to avoid overwhelming)
                    if (missingFeatures.isNotEmpty()) {
                        Text(
                            text = "🔒 Grant permissions to unlock:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        missingFeatures.take(2).forEach { feature ->
                            Text(
                                text = "• ${feature.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (missingFeatures.size > 2) {
                            Text(
                                text = "... and ${missingFeatures.size - 2} more features",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        // ✅ UPDATED: Always enabled continue button with dynamic text
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isProcessing // Only disabled while processing, not based on permissions
        ) {
            Text(
                when (readinessLevel) {
                    AppReadinessLevel.FULLY_READY -> "Continue with Full Protection"
                    AppReadinessLevel.PARTIALLY_READY -> "Continue with Partial Protection"
                    AppReadinessLevel.BASIC_READY -> "Continue with Basic Protection"
                    AppReadinessLevel.INITIALIZING -> "Continue to App"
                }
            )
        }

        // ✅ NEW: Optional permission grant helper
        if (!essentialStatus.isComplete()) {
            OutlinedButton(
                onClick = {
                    // Could scroll to first ungranted permission or show help
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant More Permissions")
            }
        }
    }
}


@Composable
private fun PermissionCard(
    permission: AppPermission,
    isGranted: Boolean,
    isProcessing: Boolean,
    onRequestPermission: () -> Unit,
    shouldShowRationale: Boolean
) {
    var showDetails by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Get features unlocked by this permission
    val unlockedFeatures = getPermissionFeatures(permission)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isGranted -> Color.Green.copy(alpha = 0.08f)
                isProcessing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isGranted) 2.dp else 6.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Permission Icon with Status
                    PermissionIconWithStatus(
                        permission = permission,
                        isGranted = isGranted,
                        isProcessing = isProcessing
                    )

                    // Permission Info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = permission.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Optional Badge
                            OptionalBadge()
                        }

                        Text(
                            text = permission.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )

                        // Features Preview
                        if (!isGranted && unlockedFeatures.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Unlocks: ${unlockedFeatures.take(2).joinToString(", ")}${if (unlockedFeatures.size > 2) "..." else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Status Chip
                PermissionStatusChip(
                    isGranted = isGranted,
                    isProcessing = isProcessing,
                    shouldShowRationale = shouldShowRationale
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Section
            when {
                isGranted -> {
                    GrantedStatusCard(unlockedFeatures)
                }

                isProcessing -> {
                    ProcessingStatusCard()
                }

                shouldShowRationale -> {
                    PermanentDenialCard(
                        permission = permission,
                        onOpenSettings = { showSettingsDialog = true }
                    )
                }

                else -> {
                    RequestPermissionCard(
                        permission = permission,
                        unlockedFeatures = unlockedFeatures,
                        onRequestPermission = onRequestPermission,
                        onShowDetails = { showDetails = !showDetails }
                    )
                }
            }

            // Expandable Details Section
            AnimatedVisibility(
                visible = showDetails && !isGranted,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                PermissionDetailsSection(
                    permission = permission,
                    unlockedFeatures = unlockedFeatures
                )
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        PermissionSettingsDialog(
            permission = permission,
            onDismiss = { showSettingsDialog = false },
            onOpenSettings = {
                showSettingsDialog = false
                //openAppSettings(context)
            }
        )
    }
}

@Composable
private fun PermissionIconWithStatus(
    permission: AppPermission,
    isGranted: Boolean,
    isProcessing: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            isGranted -> Color.Green.copy(alpha = 0.15f)
            isProcessing -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        },
        modifier = Modifier.size(48.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = getPermissionIcon(permission),
                    contentDescription = null,
                    tint = when {
                        isGranted -> Color.Green
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(24.dp)
                )

                // Granted checkmark overlay
                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = Color.Green,
                        modifier = Modifier
                            .size(16.dp)
                            .offset(x = 8.dp, y = (-8).dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionalBadge() {
    Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "OPTIONAL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PermissionStatusChip(
    isGranted: Boolean,
    isProcessing: Boolean,
    shouldShowRationale: Boolean
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = when {
            isGranted -> Color.Green.copy(alpha = 0.15f)
            shouldShowRationale -> Color.Yellow.copy(alpha = 0.15f)
            isProcessing -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = when {
                    isGranted -> Icons.Default.CheckCircle
                    shouldShowRationale -> Icons.Default.Warning
                    isProcessing -> Icons.Default.Refresh
                    else -> Icons.Default.TouchApp
                },
                contentDescription = null,
                tint = when {
                    isGranted -> Color.Green
                    shouldShowRationale -> Color.Yellow
                    isProcessing -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = when {
                    isGranted -> "Granted"
                    shouldShowRationale -> "Blocked"
                    isProcessing -> "Requesting"
                    else -> "Available"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isGranted -> Color.Green
                    shouldShowRationale -> Color.Yellow
                    isProcessing -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun GrantedStatusCard(unlockedFeatures: List<String>) {
    Surface(
        color = Color.Green.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color.Green,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Permission Granted!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Green.copy(red = 0.2f)
                )
                if (unlockedFeatures.isNotEmpty()) {
                    Text(
                        text = "Unlocked: ${unlockedFeatures.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Green.copy(red = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingStatusCard() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Requesting permission...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun PermanentDenialCard(
    permission: AppPermission,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = Color.Yellow.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
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
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Permission Previously Blocked",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Yellow.copy(blue = 0.2f)
                )
            }

            Text(
                text = "This permission was denied with \"Don't ask again\". Enable it manually in Settings to unlock features.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Yellow,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Settings")
            }
        }
    }
}

@Composable
private fun RequestPermissionCard(
    permission: AppPermission,
    unlockedFeatures: List<String>,
    onRequestPermission: () -> Unit,
    onShowDetails: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Features Preview Card
        if (unlockedFeatures.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Features You'll Unlock:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    unlockedFeatures.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.weight(1f)
            ) {
                Text("Grant Permission")
            }

            OutlinedButton(
                onClick = onShowDetails
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionDetailsSection(
    permission: AppPermission,
    unlockedFeatures: List<String>
) {
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Why Needed
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
            Column {
                Text(
                    text = "Why is this needed?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = permission.rationale,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // All Features
        if (unlockedFeatures.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Column {
                    Text(
                        text = "Safety features you'll unlock:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    unlockedFeatures.forEach { feature ->
                        Text(
                            text = "• $feature",
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
private fun PermissionSettingsDialog(
    permission: AppPermission,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Enable ${permission.title}")
        },
        text = {
            Column {
                Text("To enable this permission:")
                Spacer(modifier = Modifier.height(8.dp))

                val steps = listOf(
                    "Tap 'Open Settings' below",
                    "Find 'Permissions' section",
                    "Locate '${permission.title}'",
                    "Enable the permission",
                    "Return to SafeguardMe"
                )

                steps.forEachIndexed { index, step ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (index < steps.size - 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper Functions



private fun getPermissionFeatures(permission: AppPermission): List<String> {
    return when (permission) {
        AppPermission.LOCATION -> listOf(
            "Share location in emergencies",
            "Location-based safety alerts",
            "Auto-location incident reports"
        )
        AppPermission.CAMERA -> listOf(
            "Capture incident photos",
            "Record safety videos",
            "Document evidence quickly"
        )
        AppPermission.AUDIO_RECORDING -> listOf(
            "Record audio evidence",
            "Voice-activated safety",
            "Audio incident documentation"
        )
        AppPermission.PHONE_CALLS -> listOf(
            "One-tap emergency calling",
            "Direct 911 access",
            "Emergency contact calling"
        )
        AppPermission.STORAGE -> listOf(
            "Import existing photos",
            "Save evidence securely",
            "Backup incident reports"
        )
        AppPermission.SMS_MESSAGING -> listOf(
            "Silent SOS text alerts",
            "Notify trusted contacts via SMS",
            "Receive safety verification codes"
        )
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
        AppPermission.STORAGE -> Icons.Default.Photo
        AppPermission.SMS_MESSAGING -> Icons.Default.Email
    }
}

enum class PermissionStep {
    INTRODUCTION,
    ESSENTIAL_PERMISSIONS,
    ENHANCED_PERMISSIONS
}