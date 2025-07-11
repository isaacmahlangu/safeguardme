package com.safeguardme.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.safeguardme.app.navigation.AppNavHost
import com.safeguardme.app.ui.screens.PermissionScreen
import com.safeguardme.app.ui.theme.SafeguardMeTheme
import com.safeguardme.app.ui.theme.ThemeViewModel
import com.safeguardme.app.utils.PermissionManager
import com.safeguardme.app.utils.SecurityUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission manager
        permissionManager.initialize(this)

        // Enable screen security for sensitive screens
        SecurityUtils.disableScreenSecurity(this)

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkModeEnabled by themeViewModel.isDarkModeEnabled.collectAsState()
            val permissionsState by permissionManager.permissionsState.collectAsState()

            var showPermissionScreen by remember { mutableStateOf(!permissionsState.essentialPermissionsGranted) }

            SafeguardMeTheme(
                darkTheme = isDarkModeEnabled || isSystemInDarkTheme(),
                themeViewModel = themeViewModel
            ) {
                if (showPermissionScreen) {
                    PermissionScreen(
                        onPermissionsConfigured = {
                            showPermissionScreen = false
                        },
                        permissionManager = permissionManager
                    )
                } else {
                    AppNavHost(navController = rememberNavController())
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // SecurityUtils.disableScreenSecurity(this)
    }
}
