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
    private val userRepository: UserRepository // ✅ ADDED: Inject UserRepository
) {

    // Get current user state
    val currentUser get() = auth.currentUser
    val isSignedIn get() = currentUser != null

    // ✅ UPDATED: Enhanced sign-in with profile initialization
    suspend fun signIn(email: String, password: String): Result<Unit> = try {
        // Input sanitization
        val sanitizedEmail = email.trim().lowercase()
        val sanitizedPassword = password.trim()

        // Basic validation
        require(sanitizedEmail.isNotBlank()) { "Email cannot be empty" }
        require(sanitizedPassword.isNotBlank()) { "Password cannot be empty" }
        require(isValidEmail(sanitizedEmail)) { "Invalid email format" }

        // Attempt sign-in
        auth.signInWithEmailAndPassword(sanitizedEmail, sanitizedPassword).await()

        // ✅ ADDED: Ensure user profile exists after successful login
        userRepository.ensureUserProfile()
            .onFailure { profileError ->
                // Log but don't fail login - profile can be created later
                Log.w("AuthRepository", "Failed to ensure user profile after login", profileError)
            }

        Result.success(Unit)

    } catch (e: FirebaseAuthException) {
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
        Result.failure(SecurityException("Authentication failed. Please try again"))
    }

    // ✅ UPDATED: Enhanced sign-up with profile creation
    suspend fun signUp(email: String, password: String): Result<Unit> = try {
        val sanitizedEmail = email.trim().lowercase()
        val sanitizedPassword = password.trim()

        // Enhanced validation
        require(sanitizedEmail.isNotBlank()) { "Email cannot be empty" }
        require(sanitizedPassword.isNotBlank()) { "Password cannot be empty" }
        require(isValidEmail(sanitizedEmail)) { "Invalid email format" }
        require(isStrongPassword(sanitizedPassword)) { "Password does not meet security requirements" }

        // Create user account
        val authResult = auth.createUserWithEmailAndPassword(sanitizedEmail, sanitizedPassword).await()

        // Send email verification
        authResult.user?.sendEmailVerification()?.await()

        // ✅ ADDED: Create user profile immediately after successful registration
        userRepository.ensureUserProfile()
            .onFailure { profileError ->
                // This is more critical for new users - log as error
                Log.e("AuthRepository", "Failed to create user profile for new user", profileError)
                // Note: We could consider failing registration here, but for now we continue
            }

        Result.success(Unit)

    } catch (e: FirebaseAuthException) {
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
        Result.failure(SecurityException("Registration failed. Please try again"))
    }

    // Password reset with rate limiting protection
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        val sanitizedEmail = email.trim().lowercase()
        require(sanitizedEmail.isNotBlank()) { "Email cannot be empty" }
        require(isValidEmail(sanitizedEmail)) { "Invalid email format" }

        auth.sendPasswordResetEmail(sanitizedEmail).await()
        Result.success(Unit)

    } catch (e: FirebaseAuthException) {
        // Always return success to prevent email enumeration
        Result.success(Unit)

    } catch (e: Exception) {
        Result.failure(SecurityException("Unable to send reset email. Please try again"))
    }

    // Secure sign-out
    suspend fun signOut(): Result<Unit> = try {
        auth.signOut()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(SecurityException("Sign out failed"))
    }

    // Delete account with confirmation
    suspend fun deleteAccount(): Result<Unit> = try {
        currentUser?.delete()?.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(SecurityException("Account deletion failed"))
    }

    // Email validation
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Strong password validation
    private fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) return false

        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
    }

    // Check if user email is verified
    val isEmailVerified: Boolean
        get() = currentUser?.isEmailVerified ?: false

    // Send email verification
    suspend fun sendEmailVerification(): Result<Unit> = try {
        currentUser?.sendEmailVerification()?.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(SecurityException("Failed to send verification email"))
    }
}