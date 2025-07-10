// navigation/AppNavHost.kt - Complete Integration with Onboarding Check
package com.safeguardme.app.navigation

import androidx.compose.runtime.*
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
import com.safeguardme.app.ui.screens.*
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
        NavigationState.Main -> Screen.Home.route
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
                        NavigationState.Main -> Screen.Home.route
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

        // Protected Screens (require authentication)
        composable(Screen.Home.route) {
            if (authState) {
                HomeScreen(navController)
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Contacts.route) {
            if (authState) {
                ContactsScreen(navController)
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

        composable(Screen.EmergencyContacts.route) {
            EmergencyContactsScreen(navController)
        }
    }
}