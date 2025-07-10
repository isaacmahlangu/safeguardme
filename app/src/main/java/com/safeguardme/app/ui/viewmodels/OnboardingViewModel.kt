// ui/viewmodels/OnboardingViewModel.kt
package com.safeguardme.app.ui.viewmodels

import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.data.repositories.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingPage(
    val title: String,
    val description: String,
    val illustration: androidx.compose.ui.graphics.vector.ImageVector,
    val ctaText: String,
    val isLastPage: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isCompleting = MutableStateFlow(false)
    val isCompleting: StateFlow<Boolean> = _isCompleting.asStateFlow()

    val pages = listOf(
        OnboardingPage(
            title = "Welcome to SafeguardMe",
            description = "Your personal safety companion designed to protect and support you in challenging times.",
            illustration = androidx.compose.material.icons.Icons.Default.Security,
            ctaText = "Next"
        ),
        OnboardingPage(
            title = "Trusted Contacts",
            description = "Add people you trust who can be notified when you need help. Your safety network is always just a tap away.",
            illustration = androidx.compose.material.icons.Icons.Default.Contacts,
            ctaText = "Next"
        ),
        OnboardingPage(
            title = "Voice Activation",
            description = "Set up a keyword that can trigger alerts even when you can't reach your phone. Your voice can be your lifeline.",
            illustration = androidx.compose.material.icons.Icons.Default.Mic,
            ctaText = "Next"
        ),
        OnboardingPage(
            title = "Your Privacy Matters",
            description = "All your data is encrypted and stored securely. Only you can access your information - we protect your privacy absolutely.",
            illustration = androidx.compose.material.icons.Icons.Default.Lock,
            ctaText = "Get Started",
            isLastPage = true
        )
    )

    fun nextPage() {
        if (_currentPage.value < pages.size - 1) {
            _currentPage.value++
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value--
        }
    }

    fun skipToEnd() {
        _currentPage.value = pages.size - 1
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            _isCompleting.value = true
            try {
                settingsRepository.setOnboardingCompleted(true)
            } finally {
                _isCompleting.value = false
            }
        }
    }

    fun goToPage(page: Int) {
        if (page in 0 until pages.size) {
            _currentPage.value = page
        }
    }
}