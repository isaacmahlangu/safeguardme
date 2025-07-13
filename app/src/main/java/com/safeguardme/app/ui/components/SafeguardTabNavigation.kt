// navigation/SafeguardTabNavigation.kt - Updated with PermissionManager Integration
package com.safeguardme.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.safeguardme.app.managers.PermissionManager
import com.safeguardme.app.ui.components.SafeguardTab
import com.safeguardme.app.ui.components.SafeguardTabScaffold
import com.safeguardme.app.ui.screens.TabAssistantScreen
import com.safeguardme.app.ui.screens.TabEmergencyScreen
import com.safeguardme.app.ui.screens.TabHomeScreen
import com.safeguardme.app.ui.screens.TabProfileScreen
import com.safeguardme.app.ui.screens.TabReportScreen
import com.safeguardme.app.ui.viewmodels.HomeViewModel
import com.safeguardme.app.ui.viewmodels.SettingsViewModel

/**
 * ✅ UPDATED: SafeguardTabNavigation now accepts PermissionManager
 */
@Composable
fun SafeguardTabNavigation(
    mainNavController: NavController,
    startTab: SafeguardTab = SafeguardTab.HOME,
    permissionManager: PermissionManager // ✅ NEW: Add PermissionManager parameter
) {
    var selectedTab by remember { mutableStateOf(startTab) }
    val homeViewModel: HomeViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val uiState by homeViewModel.uiState.collectAsState()

    SafeguardTabScaffold(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        navController = mainNavController,
        safetyStatus = uiState.safetyStatus
    ) { paddingValues ->
        when (selectedTab) {
            SafeguardTab.HOME -> {
                TabHomeScreen(
                    navController = mainNavController,
                    paddingValues = paddingValues
                )
            }

            SafeguardTab.EMERGENCY -> {
                TabEmergencyScreen(
                    navController = mainNavController,
                    paddingValues = paddingValues
                    // ✅ NOTE: Current TabEmergencyScreen doesn't need PermissionManager
                    // ✅ But it's available here if you want enhanced features later:
                    // permissionManager = permissionManager
                )
            }

            SafeguardTab.REPORT -> {
                TabReportScreen(
                    navController = mainNavController,
                    paddingValues = paddingValues
                    // ✅ FUTURE: Could add PermissionManager for evidence collection features
                    // permissionManager = permissionManager
                )
            }

            SafeguardTab.ASSISTANT -> {
                TabAssistantScreen(
                    navController = mainNavController,
                    paddingValues = paddingValues
                    // ✅ FUTURE: Could add PermissionManager for voice/audio features
                    // permissionManager = permissionManager
                )
            }

            SafeguardTab.PROFILE -> {
                TabProfileScreen(
                    navController = mainNavController,
                    paddingValues = paddingValues,
                    settingsRepository = settingsViewModel.settingsRepository
                    // ✅ FUTURE: Could add PermissionManager for profile-related permission management
                    // permissionManager = permissionManager
                )
            }
        }
    }
}