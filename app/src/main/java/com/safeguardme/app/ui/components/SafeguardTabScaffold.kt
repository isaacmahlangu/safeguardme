// ui/components/SafeguardTabScaffold.kt
package com.safeguardme.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.safeguardme.app.data.models.SafetyStatus

@Composable
fun SafeguardTabScaffold(
    selectedTab: SafeguardTab,
    onTabSelected: (SafeguardTab) -> Unit,
    navController: NavController,
    safetyStatus: SafetyStatus = SafetyStatus.DISABLED,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        bottomBar = {
            SafeguardBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
                emergencyActive = safetyStatus == SafetyStatus.ENABLED || safetyStatus == SafetyStatus.EMERGENCY
            )
        },
        content = content
    )
}