// ui/viewmodels/ProfileViewModel.kt
package com.safeguardme.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.safeguardme.app.auth.AuthRepository
import com.safeguardme.app.data.models.User
import com.safeguardme.app.data.repositories.SettingsRepository
import com.safeguardme.app.data.repositories.UserRepository
import com.safeguardme.app.utils.FirebaseUtils
import com.safeguardme.app.utils.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // User profile state
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // Editable profile fields
    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    // UI states
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Validation states
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val _phoneError = MutableStateFlow<String?>(null)
    val phoneError: StateFlow<String?> = _phoneError.asStateFlow()

    // Settings states
    val appSettings = settingsRepository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsRepository.AppSettings())

    // Form validation
    val hasUnsavedChanges: StateFlow<Boolean> = combine(
        _user,
        _fullName,
        _phoneNumber
    ) { user, name, phone ->
        user != null && (
                name != user.fullName ||
                        phone != user.phoneNumber
                )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val canSave: StateFlow<Boolean> = combine(
        hasUnsavedChanges,
        _nameError,
        _phoneError,
        _isSaving
    ) { hasChanges, nameErr, phoneErr, saving ->
        hasChanges && nameErr == null && phoneErr == null && !saving
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Ensure profile exists first
                userRepository.ensureUserProfile()
                    .onSuccess { user ->
                        _user.value = user
                        _fullName.value = user.fullName
                        _phoneNumber.value = user.phoneNumber
                    }
                    .onFailure { error ->
                        handleError(error)
                    }

                // Then start observing
                userRepository.observeCurrentUserProfile()
                    .catch { e -> handleError(e) }
                    .collect { user ->
                        if (user != null) {
                            _user.value = user
                            _fullName.value = user.fullName
                            _phoneNumber.value = user.phoneNumber
                        }
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }



    fun updateFullName(name: String) {
        _fullName.value = name.take(100)
        _nameError.value = if (ValidationUtils.isValidName(name)) null else "Please enter a valid name"
    }

    fun updatePhoneNumber(phone: String) {
        val cleaned = phone.filter { it.isDigit() || it in "+()-" }
        _phoneNumber.value = cleaned.take(20)
        _phoneError.value = if (phone.isBlank() || ValidationUtils.isValidPhoneNumber(phone)) {
            null
        } else {
            "Please enter a valid phone number"
        }
    }

    fun saveProfile() {
        if (!validateForm()) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val currentUser = _user.value
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(
                        fullName = _fullName.value.trim(),
                        phoneNumber = _phoneNumber.value.trim(),
                    )

                    // This now uses set with merge internally
                    userRepository.createUserProfile(updatedUser)
                        .onSuccess {
                            _successMessage.value = "Profile updated successfully"
                        }
                        .onFailure { e -> handleError(e) }
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isSaving.value = false
            }
        }
    }


    fun resetForm() {
        _user.value?.let { user ->
            _fullName.value = user.fullName
            _phoneNumber.value = user.phoneNumber
            _nameError.value = null
            _phoneError.value = null
        }
    }

    // Settings management
    fun toggleDarkMode() {
        viewModelScope.launch {
            val currentSetting = appSettings.value.darkMode
            settingsRepository.setDarkMode(!currentSetting)
        }
    }

    fun toggleSounds() {
        viewModelScope.launch {
            val currentSetting = appSettings.value.soundsEnabled
            settingsRepository.setSoundsEnabled(!currentSetting)
        }
    }

    fun toggleOfflineMode() {
        viewModelScope.launch {
            val currentSetting = appSettings.value.offlineModeAllowed
            settingsRepository.setOfflineModeAllowed(!currentSetting)
        }
    }

    fun toggleBiometric() {
        viewModelScope.launch {
            val currentSetting = appSettings.value.biometricEnabled
            settingsRepository.setBiometricEnabled(!currentSetting)
        }
    }

    fun toggleEmergencyContactsOnly() {
        viewModelScope.launch {
            val currentSetting = appSettings.value.emergencyContactsOnly
            settingsRepository.setEmergencyContactsOnly(!currentSetting)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isSigningOut.value = true
            try {
                authRepository.signOut()
                    .onFailure { e -> handleError(e) }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isSigningOut.value = false
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (!ValidationUtils.isValidName(_fullName.value)) {
            _nameError.value = "Please enter a valid name"
            isValid = false
        }

        if (_phoneNumber.value.isNotBlank() && !ValidationUtils.isValidPhoneNumber(_phoneNumber.value)) {
            _phoneError.value = "Please enter a valid phone number"
            isValid = false
        }

        return isValid
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    private fun handleError(throwable: Throwable) {
        _error.value = FirebaseUtils.getErrorMessage(throwable as Exception)
        _isLoading.value = false
        _isSaving.value = false
        _isSigningOut.value = false
    }
}

