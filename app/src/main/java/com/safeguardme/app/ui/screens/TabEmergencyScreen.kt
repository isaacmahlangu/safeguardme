// ui/screens/TabEmergencyScreen.kt
package com.safeguardme.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.navigation.Screen
import com.safeguardme.app.ui.viewmodels.HomeViewModel

@Composable
fun TabEmergencyScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Text(
                text = "Emergency & Safety",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Emergency Button
        item {
            EmergencyTriggerCard(
                safetyStatus = uiState.safetyStatus,
                onToggle = viewModel::toggleSafetyStatus
            )
        }

        // Emergency Features
        item {
            EmergencyFeaturesGrid(navController = navController)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun EmergencyTriggerCard(
    safetyStatus: SafetyStatus,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (safetyStatus) {
                SafetyStatus.ENABLED -> Color(0xFFFFF3E0)
                SafetyStatus.EMERGENCY -> Color(0xFFFFEBEE)
                SafetyStatus.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Status indicator with pulse animation for active states
            Box(contentAlignment = Alignment.Center) {
                if (safetyStatus != SafetyStatus.DISABLED) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Surface(
                        modifier = Modifier
                            .size(80.dp)
                            .scale(pulseScale),
                        shape = CircleShape,
                        color = when (safetyStatus) {
                            SafetyStatus.ENABLED -> Color(0xFFFF9800).copy(alpha = 0.3f)
                            SafetyStatus.EMERGENCY -> Color(0xFFD32F2F).copy(alpha = 0.3f)
                            else -> Color.Transparent
                        }
                    ) {}
                }

                Icon(
                    imageVector = when (safetyStatus) {
                        SafetyStatus.ENABLED -> Icons.Default.Warning
                        SafetyStatus.EMERGENCY -> Icons.Default.Emergency
                        SafetyStatus.DISABLED -> Icons.Default.Security
                    },
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = when (safetyStatus) {
                        SafetyStatus.ENABLED -> Color(0xFFFF9800)
                        SafetyStatus.EMERGENCY -> Color(0xFFD32F2F)
                        SafetyStatus.DISABLED -> MaterialTheme.colorScheme.primary
                    }
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when (safetyStatus) {
                        SafetyStatus.ENABLED -> "🟡 Safety Mode Active"
                        SafetyStatus.EMERGENCY -> "🔴 Emergency Active"
                        SafetyStatus.DISABLED -> "🟢 Safety Ready"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = when (safetyStatus) {
                        SafetyStatus.ENABLED -> "Emergency monitoring active • Contacts notified • Gesture triggers enabled"
                        SafetyStatus.EMERGENCY -> "🚨 EMERGENCY ALERT SENT • Emergency services contacted"
                        SafetyStatus.DISABLED -> "Tap to activate safety monitoring • Gesture triggers ready"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (safetyStatus) {
                        SafetyStatus.DISABLED -> MaterialTheme.colorScheme.primary
                        else -> Color(0xFFD32F2F)
                    }
                )
            ) {
                Text(
                    text = when (safetyStatus) {
                        SafetyStatus.DISABLED -> "🛡️ Activate Safety Mode"
                        SafetyStatus.ENABLED -> "🔄 Deactivate Safety Mode"
                        SafetyStatus.EMERGENCY -> "⚠️ Override Emergency Mode"
                    },
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ✅ ADDED: Gesture triggers status
            if (safetyStatus == SafetyStatus.DISABLED) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "📱 Gesture Triggers: Volume buttons (3x) • Phone shake • Power button (5x)",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}


@Composable
private fun EmergencyFeaturesGrid(navController: NavController) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Emergency Features",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmergencyFeatureCard(
                title = "Emergency Contacts",
                subtitle = "Manage trusted contacts",
                icon = Icons.Default.ContactPhone,
                onClick = { navController.navigate(Screen.EmergencyContacts.route) },
                modifier = Modifier.weight(1f)
            )
            EmergencyFeatureCard(
                title = "Safety Trigger",
                subtitle = "Advanced safety controls",
                icon = Icons.Default.TouchApp,
                onClick = { navController.navigate(Screen.SafetyTrigger.route) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EmergencyFeatureCard(
                title = "Voice Triggers",
                subtitle = "Hands-free activation",
                icon = Icons.Default.RecordVoiceOver,
                onClick = { navController.navigate(Screen.Trigger.route) },
                modifier = Modifier.weight(1f)
            )
            EmergencyFeatureCard(
                title = "Location Sharing",
                subtitle = "Real-time location",
                icon = Icons.Default.LocationOn,
                onClick = { /* TODO: Implement location sharing */ },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EmergencyFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}