package com.safeguardme.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.safeguardme.app.ui.theme.SafeguardMeTheme
import com.safeguardme.app.ui.theme.ThemeViewModel
import com.safeguardme.app.utils.SecurityUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable screen security for sensitive screens
        SecurityUtils.disableScreenSecurity(this)

        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkModeEnabled by themeViewModel.isDarkModeEnabled.collectAsState()

            SafeguardMeTheme(
                darkTheme = isDarkModeEnabled || isSystemInDarkTheme(),
                themeViewModel = themeViewModel
            ) {
                SafeguardMeApp()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //SecurityUtils.disableScreenSecurity(this)
    }
}