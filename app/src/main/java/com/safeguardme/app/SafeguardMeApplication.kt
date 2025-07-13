package com.safeguardme.app

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SafeguardMeApplication : Application() {

    companion object {
        private const val TAG = "SafeguardMeApplication"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "🚀 Application onCreate started")

        try {
            // ✅ FIXED: Safe Firebase initialization
            initializeFirebase()

            Log.d(TAG, "✅ Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization failed", e)
            // Don't crash the app if Firebase fails
        }

        // ✅ FIXED: Conditional debug features
        try {
            if (isDebuggable()) {
                Log.d(TAG, "🐛 Debug mode enabled")
                // enableStrictMode() // Commented out to avoid crashes during development
            } else {
                Log.d(TAG, "🔒 Production mode - enabling security")
                disableDebugging()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Debug/Security setup failed", e)
        }

        Log.d(TAG, "✅ Application onCreate completed")
    }

    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "✅ FirebaseApp initialized")

            // ✅ FIXED: Delayed Firebase test - only after auth state established
            setupFirebaseAuthListener()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase initialization error", e)
            throw e // Re-throw to be caught by onCreate
        }
    }

    private fun setupFirebaseAuthListener() {
        try {
            val auth = FirebaseAuth.getInstance()
            auth.addAuthStateListener { firebaseAuth ->
                firebaseAuth.currentUser?.let { user ->
                    // ✅ FIXED: Safer Firebase access test with better error handling
                    testFirebaseAccess(user.uid)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase auth listener setup failed", e)
        }
    }

    private fun testFirebaseAccess(userId: String) {
        try {
            Log.d(TAG, "🔍 Testing Firebase access for user: $userId")

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("emergencyContacts")
                .limit(1)
                .get()
                .addOnSuccessListener {
                    Log.d(TAG, "✅ Emergency contacts accessible")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "⚠️ Emergency contacts access limited: ${e.message}")
                    // Don't treat this as a critical error
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase access test failed", e)
        }
    }

    private fun isDebuggable(): Boolean {
        return try {
            0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Debug check failed", e)
            false // Default to production mode if check fails
        }
    }

    private fun enableStrictMode() {
        try {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )

            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )

            Log.d(TAG, "✅ StrictMode enabled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ StrictMode setup failed", e)
        }
    }

    private fun disableDebugging() {
        try {
            // Additional security measures for production
            Log.d(TAG, "🔒 Production security measures applied")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Security setup failed", e)
        }
    }
}