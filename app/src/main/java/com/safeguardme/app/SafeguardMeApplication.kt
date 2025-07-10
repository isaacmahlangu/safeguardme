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

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Verify security rules (after user signs in)
        testFirebaseAccess()

        // Enable strict mode in debug builds
        if (isDebuggable()) {
            //enableStrictMode()
        }

        // Security: Disable debugging in production
        if (!isDebuggable()) {
            disableDebugging()
        }
    }

    private fun isDebuggable(): Boolean {
        return 0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
    }

    private fun testFirebaseAccess() {
        val auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.let { user ->
                Log.d("Firebase", "Testing emergency contact access...")

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .collection("emergencyContacts")
                    .limit(1)
                    .get()
                    .addOnSuccessListener {
                        Log.d("Firebase", "✅ Emergency contacts accessible")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firebase", "❌ Emergency contacts still blocked: ${e.message}")
                    }
            }
        }
    }

    private fun enableStrictMode() {
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
    }

    private fun disableDebugging() {
        // Additional security measures for production
        // This will be expanded in Phase 2
    }
}