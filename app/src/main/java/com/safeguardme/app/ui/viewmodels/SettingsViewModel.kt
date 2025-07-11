// ui/viewmodels/SettingsViewModel.kt
package com.safeguardme.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.safeguardme.app.data.repositories.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settingsRepository: SettingsRepository
) : ViewModel() {
    // This is a simple wrapper ViewModel that provides access to SettingsRepository
    // for Composables that need dependency injection access
}