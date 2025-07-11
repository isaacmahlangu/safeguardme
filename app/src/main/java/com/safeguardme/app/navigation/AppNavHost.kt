package com.safeguardme.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.safeguardme.app.data.repositories.SettingsRepository
import com.safeguardme.app.ui.components.SafeguardTab
import com.safeguardme.app.ui.screens.AIAssistanceScreen
import com.safeguardme.app.ui.screens.EmergencyContactsScreen
import com.safeguardme.app.ui.screens.ForgotPasswordScreen
import com.safeguardme.app.ui.screens.IncidentHistoryScreen
import com.safeguardme.app.ui.screens.IncidentReportScreen
import com.safeguardme.app.ui.screens.LoginScreen
import com.safeguardme.app.ui.screens.OnboardingScreen
import com.safeguardme.app.ui.screens.ProfileScreen
import com.safeguardme.app.ui.screens.RegisterScreen
import com.safeguardme.app.ui.screens.SafetyTriggerScreen
import com.safeguardme.app.ui.screens.SplashScreen
import com.safeguardme.app.ui.screens.TriggerScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Loading)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    init {
        checkAppState()
    }

    private fun checkAppState() {
        viewModelScope.launch {
            val hasSeenOnboarding = settingsRepository.hasSeenOnboarding.first()
            val isUserAuthenticated = FirebaseAuth.getInstance().currentUser != null

            _navigationState.value = when {
                !hasSeenOnboarding -> NavigationState.Onboarding
                !isUserAuthenticated -> NavigationState.Authentication
                else -> NavigationState.Main
            }
        }
    }
}

sealed class NavigationState {
    object Loading : NavigationState()
    object Onboarding : NavigationState()
    object Authentication : NavigationState()
    object Main : NavigationState()
}

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    navigationViewModel: AppNavigationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val navigationState by navigationViewModel.navigationState.collectAsState()

    // Observe auth state for automatic navigation
    val authState by remember {
        derivedStateOf { auth.currentUser != null }
    }

    // Security: Clear back stack when auth state changes
    LaunchedEffect(authState) {
        if (!authState) {
            // User signed out - clear back stack and go to login
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Determine start destination based on navigation state
    val startDestination = when (navigationState) {
        NavigationState.Loading -> Screen.Splash.route
        NavigationState.Onboarding -> Screen.Onboarding.route
        NavigationState.Authentication -> Screen.Login.route
        NavigationState.Main -> "main_tabs"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen()

            // Auto-navigate based on navigation state
            LaunchedEffect(navigationState) {
                if (navigationState != NavigationState.Loading) {
                    kotlinx.coroutines.delay(2000) // 2 second splash
                    val destination = when (navigationState) {
                        NavigationState.Onboarding -> Screen.Onboarding.route
                        NavigationState.Authentication -> Screen.Login.route
                        NavigationState.Main -> "main_tabs"
                        NavigationState.Loading -> return@LaunchedEffect
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
        }

        // Onboarding Screen
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController)
        }

        // Authentication Screens
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController)
        }

        // Main Tab Navigation
        composable("main_tabs") {
            if (authState) {
                SafeguardTabNavigation(
                    mainNavController = navController,
                    startTab = SafeguardTab.HOME
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        // Individual Screens (accessible from tabs)
        composable(Screen.Profile.route) {
            if (authState) {
                ProfileScreen(navController)
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.IncidentReport.route) {
            if (authState) {
                IncidentReportScreen(navController)
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.IncidentHistory.route) {
            if (authState) {
                IncidentHistoryScreen(navController)
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.EmergencyContacts.route) {
            if (authState) {
                EmergencyContactsScreen(navController)
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.AIAssistant.route) {
            if (authState) {
                AIAssistanceScreen(navController)
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.SafetyTrigger.route) {
            if (authState) {
                SafetyTriggerScreen(navController)
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Trigger.route) {
            if (authState) {
                TriggerScreen(navController)
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
}