// MainActivity.kt - FIXED with Proper Crash Handler and PermissionManager Passing
package com.safeguardme.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.safeguardme.app.managers.PermissionManager
import com.safeguardme.app.navigation.AppNavHost
import com.safeguardme.app.ui.screens.PermissionScreen
import com.safeguardme.app.ui.theme.SafeguardMeTheme
import com.safeguardme.app.ui.theme.ThemeViewModel
import com.safeguardme.app.utils.SecurityUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionManager: PermissionManager

    companion object {
        private const val TAG = "MainActivity"
    }

    // ✅ FIXED: Safe permission launcher with comprehensive error handling
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        try {
            Log.d(TAG, "📋 Permission results received: $permissions")

            if (::permissionManager.isInitialized) {
                permissionManager.onPermissionResult(permissions)
                permissionManager.refreshPermissions()
                Log.d(TAG, "✅ Permission results processed successfully")
            } else {
                Log.e(TAG, "❌ PermissionManager not initialized when processing results")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error processing permission results", e)
        }
    }

    // ✅ FIXED: Crash handler without infinite loop
    private var originalCrashHandler: Thread.UncaughtExceptionHandler? = null

    private val crashHandler = Thread.UncaughtExceptionHandler { thread, exception ->
        try {
            Log.e(TAG, "💥 UNCAUGHT EXCEPTION in thread ${thread.name}", exception)
            Log.e(TAG, "💥 Exception message: ${exception.message}")
            Log.e(TAG, "💥 Exception class: ${exception.javaClass.name}")

            // Log first few stack trace elements to avoid infinite loops
            val stackTrace = exception.stackTrace.take(10)
            stackTrace.forEach { element ->
                Log.e(TAG, "💥   at $element")
            }

            // You could send this to crash reporting service here

        } catch (loggingException: Exception) {
            // If logging fails, don't try to log the logging failure
            System.err.println("Failed to log crash: ${loggingException.message}")
        } finally {
            // ✅ FIXED: Call original handler, not default (which might be this handler)
            try {
                originalCrashHandler?.uncaughtException(thread, exception)
            } catch (e: Exception) {
                // If that fails too, exit gracefully
                System.exit(1)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ FIXED: Set up crash handler safely
        originalCrashHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)

        super.onCreate(savedInstanceState)

        Log.d(TAG, "🚀 MainActivity onCreate started")

        try {
            // ✅ CRITICAL: Initialize permission manager safely
            initializePermissionManager()

            // ✅ FIXED: Safe security utils
            try {
                SecurityUtils.disableScreenSecurity(this)
                Log.d(TAG, "✅ Security utils applied")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Security utils failed, continuing anyway", e)
            }

            enableEdgeToEdge()

            setupContent()

            Log.d(TAG, "✅ MainActivity onCreate completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in MainActivity onCreate", e)
            // Show error screen instead of crashing
            setContent {
                ErrorScreen(
                    error = e,
                    onRetry = { recreate() }
                )
            }
        }
    }

    private fun initializePermissionManager() {
        try {
            if (!::permissionManager.isInitialized) {
                Log.e(TAG, "❌ PermissionManager not injected by Hilt!")
                return
            }

            Log.d(TAG, "🔧 Setting up PermissionManager...")

            permissionManager.setActivity(this)

            permissionManager.setPermissionLauncher { permissions ->
                try {
                    Log.d(TAG, "🚀 Launching permission request for: ${permissions.contentToString()}")
                    permissionLauncher.launch(permissions)
                    Log.d(TAG, "✅ Permission launcher triggered successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to launch permission request", e)
                }
            }

            permissionManager.initialize(this)
            Log.d(TAG, "✅ PermissionManager initialized successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ PermissionManager initialization failed", e)
            throw e
        }
    }

    private fun setupContent() {
        setContent {

                val themeViewModel: ThemeViewModel = hiltViewModel()
                val isDarkModeEnabled by themeViewModel.isDarkModeEnabled.collectAsState()

                // ✅ FIXED: Safe permission state collection with default values
                val locationGranted by permissionManager.locationGranted.collectAsState(false)
                val cameraGranted by permissionManager.cameraGranted.collectAsState(false)
                val audioGranted by permissionManager.audioGranted.collectAsState(false)
                val phoneGranted by permissionManager.phoneGranted.collectAsState(false)
                val storageGranted by permissionManager.storageGranted.collectAsState(false)

                var showPermissionScreen by remember { mutableStateOf(true) }

                // ✅ UPDATED: Modified to use graceful degradation instead of blocking
                LaunchedEffect(locationGranted, cameraGranted, audioGranted, phoneGranted, storageGranted) {
                    try {
                        // ✅ CHANGED: Don't require all permissions - use graceful degradation
                        val hasMinimumPermissions = audioGranted || locationGranted // At least one essential

                        Log.d(TAG, "🔍 Permission check:")
                        Log.d(TAG, "   📍 Location: $locationGranted")
                        Log.d(TAG, "   📷 Camera: $cameraGranted")
                        Log.d(TAG, "   🎤 Audio: $audioGranted")
                        Log.d(TAG, "   📞 Phone: $phoneGranted")
                        Log.d(TAG, "   💾 Storage: $storageGranted")
                        Log.d(TAG, "   ✅ Has Minimum: $hasMinimumPermissions")

                        // ✅ CHANGED: Allow proceeding with partial permissions
                        showPermissionScreen = !hasMinimumPermissions

                        if (hasMinimumPermissions) {
                            Log.d(TAG, "✅ Minimum permissions available, proceeding to main app")
                        } else {
                            Log.w(TAG, "⚠️ No essential permissions granted yet")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error in permission check LaunchedEffect", e)
                        // On error, allow proceeding to main app
                        showPermissionScreen = false
                    }
                }

                SafeguardMeTheme(
                    darkTheme = isDarkModeEnabled || isSystemInDarkTheme(),
                    themeViewModel = themeViewModel
                ) {
                    if (showPermissionScreen) {
                        Log.d(TAG, "🎨 Rendering PermissionScreen")
                        PermissionScreen(
                            onPermissionsConfigured = {
                                try {
                                    Log.d(TAG, "📱 Permission screen configuration completed")
                                    showPermissionScreen = false
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Error in onPermissionsConfigured", e)
                                    showPermissionScreen = false // Proceed anyway
                                }
                            },
                            permissionManager = permissionManager
                        )
                    } else {
                        Log.d(TAG, "🎨 Rendering main AppNavHost")
                        // ✅ CRITICAL: Pass permissionManager to AppNavHost
                        AppNavHost(
                            navController = rememberNavController(),
                            permissionManager = permissionManager // Pass it down
                        )
                    }
                }

        }
    }

    override fun onResume() {
        super.onResume()
        try {
            Log.d(TAG, "🔄 MainActivity onResume")

            if (::permissionManager.isInitialized) {
                permissionManager.setActivity(this)
                permissionManager.refreshPermissions()
                Log.d(TAG, "✅ Permissions refreshed on resume")
            } else {
                Log.w(TAG, "⚠️ PermissionManager not initialized on resume")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in onResume", e)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "⏸️ MainActivity onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 MainActivity onDestroy")

        // ✅ ADDED: Restore original crash handler
        try {
            Thread.setDefaultUncaughtExceptionHandler(originalCrashHandler)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to restore original crash handler", e)
        }
    }
}

/**
 * ✅ NEW: Error screen to show instead of crashing
 */
@Composable
private fun ErrorScreen(
    error: Throwable,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "SafeguardMe encountered an error during startup",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Button(onClick = onRetry) {
                Text("Try Again")
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Error: ${error.message ?: error.javaClass.simpleName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
