// AuthenticationStateManager.kt - Centralized auth state management
package com.safeguardme.app.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationStateManager @Inject constructor() {

    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    fun setAuthenticationState(isLoggedIn: Boolean, userId: String? = null) {
        _isUserLoggedIn.value = isLoggedIn
        _currentUserId.value = if (isLoggedIn) userId else null
    }

    fun clearAuthenticationState() {
        _isUserLoggedIn.value = false
        _currentUserId.value = null
    }
}