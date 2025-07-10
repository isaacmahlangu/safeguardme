// Updated SafeguardMeApp.kt with proper theme integration
package com.safeguardme.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.safeguardme.app.navigation.AppNavHost
import com.safeguardme.app.utils.SecureScreen
import com.safeguardme.app.utils.SecurityUtils

@Composable
fun SafeguardMeApp() {
    // Apply security screen protection
    SecureScreen()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AppNavHost()
    }
}