
// navigation/Screen.kt
package com.safeguardme.app.navigation

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


    // Future screens
    object Settings : Screen("settings")
    object EmergencyMode : Screen("emergency_mode")
}


