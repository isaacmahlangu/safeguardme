// ui/components/SafeguardBottomNavigation.kt
package com.safeguardme.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.outlined.EmergencyShare
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SafeguardTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val description: String
) {
    HOME(
        route = "home_tab",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        description = "Dashboard & Overview"
    ),
    EMERGENCY(
        route = "emergency_tab",
        title = "Emergency",
        selectedIcon = Icons.Filled.Emergency,
        unselectedIcon = Icons.Outlined.EmergencyShare,
        description = "Safety & Emergency Features"
    ),
    REPORT(
        route = "report_tab",
        title = "Report",
        selectedIcon = Icons.Filled.Report,
        unselectedIcon = Icons.Outlined.ReportProblem,
        description = "Incident Management"
    ),
    ASSISTANT(
        route = "assistant_tab",
        title = "Assistant",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology,
        description = "AI Help & Guidance"
    ),
    PROFILE(
        route = "profile_tab",
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        description = "Profile & Settings"
    )

}

@Composable
fun SafeguardBottomNavigation(
    selectedTab: SafeguardTab,
    onTabSelected: (SafeguardTab) -> Unit,
    modifier: Modifier = Modifier,
    emergencyActive: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SafeguardTab.entries.forEach { tab ->
                SafeguardTabItem(
                    tab = tab,
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    emergencyActive = emergencyActive && tab == SafeguardTab.EMERGENCY,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SafeguardTabItem(
    tab: SafeguardTab,
    selected: Boolean,
    onClick: () -> Unit,
    emergencyActive: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tab_scale"
    )

    val animatedColor by animateColorAsState(
        targetValue = when {
            emergencyActive -> Color(0xFFD32F2F)
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300),
        label = "tab_color"
    )

    val animatedBackgroundColor by animateColorAsState(
        targetValue = when {
            emergencyActive -> Color(0xFFD32F2F).copy(alpha = 0.1f)
            selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "tab_background"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(animatedBackgroundColor)
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .scale(animatedScale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp)
        ) {
            // Emergency pulse animation
            if (emergencyActive) {
                Box(contentAlignment = Alignment.Center) {
                    val infiniteTransition = rememberInfiniteTransition(label = "emergency_pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Surface(
                        modifier = Modifier
                            .size(32.dp)
                            .scale(pulseScale),
                        shape = CircleShape,
                        color = Color(0xFFD32F2F).copy(alpha = 0.2f)
                    ) {}
                }
            }

            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.description,
                tint = animatedColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = tab.title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = animatedColor,
            maxLines = 1
        )

        // Active indicator dot
        if (selected) {
            Surface(
                modifier = Modifier.size(3.dp),
                shape = CircleShape,
                color = animatedColor
            ) {}
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}