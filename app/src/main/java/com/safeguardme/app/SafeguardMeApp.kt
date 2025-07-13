// Updated SafeguardMeApp.kt - Simplified since MainActivity handles permissions
package com.safeguardme.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.safeguardme.app.managers.PermissionManager
import com.safeguardme.app.navigation.AppNavHost

/**
 * ✅ SIMPLIFIED: Main app composable
 * Permission handling is now done in MainActivity
 */
@Composable
fun SafeguardMeApp(
    navController: NavHostController = rememberNavController()
) {
    // Apply security screen protection
    //SecureScreen()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AppNavHost(navController = navController, permissionManager = PermissionManager())
    }
}