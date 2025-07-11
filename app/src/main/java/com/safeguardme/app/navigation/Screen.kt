
// navigation/Screen.kt
package com.safeguardme.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.safeguardme.app.ui.screens.TabHomeScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object Contacts : Screen("contacts")
    object IncidentReport : Screen("incident_report")
    object IncidentHistory : Screen("incident_history")
    object SafetyTrigger : Screen("safety_trigger")
    object AIAssistant : Screen("ai_assistant")
    object Profile : Screen("profile")
    object EmergencyContacts : Screen("emergency_contacts")

    object Trigger : Screen("trigger")
    object Settings : Screen("settings")
    object EmergencyMode : Screen("emergency_mode")

    // Tab Navigation Routes
    object MainTabs : Screen("main_tabs")
    object HomeTab : Screen("home_tab")
    object EmergencyTab : Screen("emergency_tab")
    object ReportTab : Screen("report_tab")
    object AssistantTab : Screen("assistant_tab")


}


@Composable
fun IntegratedHomeScreen(
    navController: androidx.navigation.NavController,
    viewModel: com.safeguardme.app.ui.viewmodels.HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    emergencyViewModel: com.safeguardme.app.ui.viewmodels.EmergencyContactViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    triggerViewModel: com.safeguardme.app.ui.viewmodels.TriggerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    // This bridges the gap between old HomeScreen and new TabHomeScreen
    val paddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)

    TabHomeScreen(
        navController = navController,
        paddingValues = paddingValues,
        viewModel = viewModel,
        emergencyViewModel = emergencyViewModel
    )
}