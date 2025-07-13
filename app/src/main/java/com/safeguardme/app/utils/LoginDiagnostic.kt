// utils/LoginDiagnostic.kt - For debugging navigation issues
package com.safeguardme.app.utils

import android.util.Log
import androidx.compose.runtime.*
import androidx.navigation.NavController
import com.safeguardme.app.auth.LoginViewModel

@Composable
fun LoginNavigationDiagnostic(
    viewModel: LoginViewModel,
    navController: NavController,
    onNavigationAttempt: (Boolean) -> Unit = {}
) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Debug logging
    LaunchedEffect(isAuthenticated, isLoading, error) {
        Log.d("LoginDiagnostic", """
            Authentication State:
            - isAuthenticated: $isAuthenticated
            - isLoading: $isLoading
            - error: $error
            - currentDestination: ${navController.currentDestination?.route}
        """.trimIndent())
    }

    // Monitor navigation attempts
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            Log.d("LoginDiagnostic", "Attempting navigation to main screen...")
            onNavigationAttempt(true)

            try {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                    launchSingleTop = true
                }
                Log.d("LoginDiagnostic", "Navigation successful")
            } catch (e: Exception) {
                Log.e("LoginDiagnostic", "Navigation failed: ${e.message}", e)
                onNavigationAttempt(false)
            }
        }
    }
}