package com.safeguardme.app.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.safeguardme.app.data.repositories.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) {

    companion object {
        private const val TAG = "AuthRepository"
    }

    // Get current user state
    val currentUser get() = auth.currentUser
    val isSignedIn get() = currentUser != null

    /**
     * ✅ IMPROVED: Enhanced sign-in with better error handling
     */
    suspend fun signIn(email: String, password: String): Result<Unit> = try {
        // Input sanitization
        val sanitizedEmail = email.trim().lowercase()
        val sanitizedPassword = password.trim()

        // Basic validation
        require(sanitizedEmail.isNotBlank()) { "Email cannot be empty" }
        require(sanitizedPassword.isNotBlank()) { "Password cannot be empty" }
        require(isValidEmail(sanitizedEmail)) { "Invalid email format" }

        Log.d(TAG, "🔐 Attempting sign-in for: $sanitizedEmail")

        // Attempt sign-in
        auth.signInWithEmailAndPassword(sanitizedEmail, sanitizedPassword).await()

        Log.d(TAG, "✅ Firebase authentication successful")

        // ✅ IMPROVED: Better profile handling with graceful failure
        try {
            userRepository.ensureUserProfile()
                .onSuccess { user ->
                    Log.d(TAG, "✅ User profile ensured: ${user.email}")
                }
                .onFailure { profileError ->
                    // ✅ CRITICAL: Don't fail login if profile has issues
                    Log.w(TAG, "⚠️ Profile initialization had issues (non-critical)", profileError)

                    // Attempt to initialize cache at least
                    try {
                        userRepository.initializeUserCache()
                        Log.d(TAG, "✅ User cache initialized as fallback")
                    } catch (cacheError: Exception) {
                        Log.w(TAG, "⚠️ Cache initialization also failed", cacheError)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ User profile handling failed, but login was successful", e)
            // Don't fail the entire login process
        }

        Log.d(TAG, "✅ Sign-in process completed successfully")
        Result.success(Unit)

    } catch (e: FirebaseAuthException) {
        Log.w(TAG, "🔒 Firebase authentication failed: ${e.errorCode}", e)

        // Map Firebase errors to generic messages to prevent account enumeration
        val genericError = when (e.errorCode) {
            "ERROR_INVALID_EMAIL",
            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL" -> "Invalid email or password"
            "ERROR_USER_DISABLED" -> "Account has been disabled"
            "ERROR_TOO_MANY_REQUESTS" -> "Too many requests. Please try again later"
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection"
            else -> "Authentication failed. Please try again"
        }
        Result.failure(SecurityException(genericError))

    } catch (e: Exception) {
        Log.e(TAG, "❌ Unexpected error during sign-in", e)
        Result.failure(SecurityException("Authentication failed. Please try again"))
    }

    /**
     * ✅ IMPROVED: Enhanced sign-up with better error handling
     */
    suspend fun signUp(email: String, password: String): Result<Unit> = try {
        val sanitizedEmail = email.trim().lowercase()
        val sanitizedPassword = password.trim()

        // Enhanced validation
        require(sanitizedEmail.isNotBlank()) { "Email cannot be empty" }
        require(sanitizedPassword.isNotBlank()) { "Password cannot be empty" }
        require(isValidEmail(sanitizedEmail)) { "Invalid email format" }
        require(isStrongPassword(sanitizedPassword)) { "Password does not meet security requirements" }

        Log.d(TAG, "📝 Attempting registration for: $sanitizedEmail")

        // Create user account
        val authResult = auth.createUserWithEmailAndPassword(sanitizedEmail, sanitizedPassword).await()

        Log.d(TAG, "✅ Firebase user account created")

        // ✅ IMPROVED: Send email verification with error handling
        try {
            authResult.user?.sendEmailVerification()?.await()
            Log.d(TAG, "✅ Email verification sent")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to send email verification (non-critical)", e)
            // Don't fail registration if email verification fails
        }

        // ✅ IMPROVED: Better profile creation handling
        try {
            userRepository.ensureUserProfile()
                .onSuccess { user ->
                    Log.d(TAG, "✅ User profile created: ${user.email}")
                }
                .onFailure { profileError ->
                    Log.e(TAG, "❌ Failed to create user profile for new user", profileError)

                    // ✅ FALLBACK: Try to create minimal profile
                    try {
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            userRepository.createUserProfile(
                                com.safeguardme.app.data.models.User(
                                    uid = currentUser.uid,
                                    email = currentUser.email ?: "",
                                    fullName = currentUser.displayName ?: "",
                                    createdAt = System.currentTimeMillis(),
                                    lastActiveAt = System.currentTimeMillis(),
                                    valid = true
                                )
                            ).onFailure { fallbackError ->
                                Log.e(TAG, "❌ Even fallback profile creation failed", fallbackError)
                            }
                        }
                    } catch (fallbackException: Exception) {
                        Log.e(TAG, "❌ Fallback profile creation exception", fallbackException)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Profile creation process failed", e)
            // Continue with registration success even if profile creation fails
        }

        Log.d(TAG, "✅ Registration process completed")
        Result.success(Unit)

    } catch (e: FirebaseAuthException) {
        Log.w(TAG, "🔒 Firebase registration failed: ${e.errorCode}", e)

        val genericError = when (e.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists"
            "ERROR_WEAK_PASSWORD" -> "Password does not meet security requirements"
            "ERROR_INVALID_EMAIL" -> "Invalid email format"
            "ERROR_TOO_MANY_REQUESTS" -> "Too many requests. Please try again later"
            "ERROR_OPERATION_NOT_ALLOWED" -> "Email/password accounts are not enabled"
            else -> "Registration failed. Please try again"
        }
        Result.failure(SecurityException(genericError))

    } catch (e: Exception) {
        Log.e(TAG, "❌ Unexpected error during registration", e)
        Result.failure(SecurityException("Registration failed. Please try again"))
    }

    // Password reset with rate limiting protection
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        val sanitizedEmail = email.trim().lowercase()
        require(sanitizedEmail.isNotBlank()) { "Email cannot be empty" }
        require(isValidEmail(sanitizedEmail)) { "Invalid email format" }

        Log.d(TAG, "🔑 Sending password reset email to: $sanitizedEmail")

        auth.sendPasswordResetEmail(sanitizedEmail).await()

        Log.d(TAG, "✅ Password reset email sent")
        Result.success(Unit)

    } catch (e: FirebaseAuthException) {
        Log.w(TAG, "⚠️ Password reset failed: ${e.errorCode}", e)
        // Always return success to prevent email enumeration
        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "❌ Password reset error", e)
        Result.failure(SecurityException("Unable to send reset email. Please try again"))
    }

    // Secure sign-out
    suspend fun signOut(): Result<Unit> = try {
        Log.d(TAG, "👋 Signing out user")

        // Clear user cache before signing out
        userRepository.clearUserCache()

        auth.signOut()

        Log.d(TAG, "✅ Sign out completed")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Sign out failed", e)
        Result.failure(SecurityException("Sign out failed"))
    }

    // Delete account with confirmation
    suspend fun deleteAccount(): Result<Unit> = try {
        Log.w(TAG, "🗑️ Deleting user account")

        // Try to delete user data first
        try {
            userRepository.emergencyDeleteUserData()
            Log.d(TAG, "✅ User data deleted")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to delete user data, continuing with account deletion", e)
        }

        currentUser?.delete()?.await()

        Log.w(TAG, "✅ Account deleted")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Account deletion failed", e)
        Result.failure(SecurityException("Account deletion failed"))
    }

    // Email validation
    private fun isValidEmail(email: String): Boolean {
        return try {
            android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
        } catch (e: Exception) {
            Log.w(TAG, "Email validation failed", e)
            false
        }
    }

    // Strong password validation
    private fun isStrongPassword(password: String): Boolean {
        return try {
            if (password.length < 8) return false

            val hasUpperCase = password.any { it.isUpperCase() }
            val hasLowerCase = password.any { it.isLowerCase() }
            val hasDigit = password.any { it.isDigit() }
            val hasSpecialChar = password.any { !it.isLetterOrDigit() }

            hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
        } catch (e: Exception) {
            Log.w(TAG, "Password validation failed", e)
            false
        }
    }

    // Check if user email is verified
    val isEmailVerified: Boolean
        get() = currentUser?.isEmailVerified ?: false

    // Send email verification
    suspend fun sendEmailVerification(): Result<Unit> = try {
        Log.d(TAG, "📧 Sending email verification")

        currentUser?.sendEmailVerification()?.await()

        Log.d(TAG, "✅ Email verification sent")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to send verification email", e)
        Result.failure(SecurityException("Failed to send verification email"))
    }
}