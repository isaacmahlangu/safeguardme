// ui/screens/TabHomeScreen.kt
package com.safeguardme.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.navigation.Screen
import com.safeguardme.app.ui.components.CompactIncidentCard
import com.safeguardme.app.ui.viewmodels.EmergencyContactViewModel
import com.safeguardme.app.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabHomeScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
    emergencyViewModel: EmergencyContactViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val emergencyContacts by emergencyViewModel.emergencyReadyContacts.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Quick Status Overview
        item {
            QuickStatusCard(
                safetyStatus = uiState.safetyStatus,
                emergencyContactsCount = emergencyContacts.size,
                recentIncidentsCount = uiState.recentIncidents.size
            )
        }

        // Quick Actions Grid
        item {
            QuickActionsGrid(navController = navController)
        }

        // Recent Activity
        item {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.recentIncidents.isEmpty()) {
            item { EmptyActivityState() }
        } else {
            items(uiState.recentIncidents.take(3)) { incident ->
                CompactIncidentCard(
                    incident = incident,
                    onClick = {
                        // Navigate to incident detail or incident history
                        navController.navigate(Screen.IncidentHistory.route)
                    }
                )
            }

            if (uiState.recentIncidents.size > 3) {
                item {
                    TextButton(
                        onClick = { navController.navigate(Screen.IncidentHistory.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("View All Incidents")
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

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickStatusCard(
    safetyStatus: SafetyStatus,
    emergencyContactsCount: Int,
    recentIncidentsCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (safetyStatus) {
                SafetyStatus.ENABLED -> Color(0xFFFFF3E0)
                SafetyStatus.EMERGENCY -> Color(0xFFFFEBEE)
                SafetyStatus.DISABLED -> MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = when (safetyStatus) {
                        SafetyStatus.ENABLED -> Icons.Default.Warning
                        SafetyStatus.EMERGENCY -> Icons.Default.Emergency
                        SafetyStatus.DISABLED -> Icons.Default.Shield
                    },
                    contentDescription = null,
                    tint = when (safetyStatus) {
                        SafetyStatus.ENABLED -> Color(0xFFFF9800)
                        SafetyStatus.EMERGENCY -> Color(0xFFD32F2F)
                        SafetyStatus.DISABLED -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(28.dp)
                )

                Column {
                    Text(
                        text = when (safetyStatus) {
                            SafetyStatus.ENABLED -> "Safety Mode Active"
                            SafetyStatus.EMERGENCY -> "Emergency Active"
                            SafetyStatus.DISABLED -> "System Ready"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap Emergency tab for controls",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusMetric(
                    label = "Contacts",
                    value = emergencyContactsCount.toString(),
                    icon = Icons.Default.ContactPhone
                )
                StatusMetric(
                    label = "Incidents",
                    value = recentIncidentsCount.toString(),
                    icon = Icons.Default.Report
                )
                StatusMetric(
                    label = "Status",
                    value = "Secure",
                    icon = Icons.Default.Security
                )
            }
        }
    }
}

@Composable
private fun StatusMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionsGrid(navController: NavController) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Emergency Contacts",
                subtitle = "Manage trusted contacts",
                icon = Icons.Default.ContactPhone,
                onClick = { navController.navigate(Screen.EmergencyContacts.route) },
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "Voice Setup",
                subtitle = "Configure voice triggers",
                icon = Icons.Default.RecordVoiceOver,
                onClick = { navController.navigate(Screen.Trigger.route) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
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
                modifier = Modifier.size(24.dp),
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

@Composable
private fun EmptyActivityState() {
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
                text = "All Clear",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "No recent incidents reported",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}