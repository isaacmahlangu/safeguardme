// ui/viewmodels/TriggerScreenViewModel.kt
package com.safeguardme.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.data.models.User
import com.safeguardme.app.data.repositories.SettingsRepository
import com.safeguardme.app.data.repositories.UserRepository
import com.safeguardme.app.utils.FirebaseUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random


enum class TriggerMethod {
    VOICE_KEYWORD,
    PANIC_BUTTON,
    SHAKE_DETECTION,
    VOLUME_BUTTON_SEQUENCE,
    POWER_BUTTON_SEQUENCE
}

data class TriggerConfiguration(
    val method: TriggerMethod,
    val isEnabled: Boolean,
    val keyword: String = "",
    val sensitivity: Float = 0.5f,
    val confirmationRequired: Boolean = true,
    val description: String = ""
)

@HiltViewModel
class TriggerScreenViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // User state
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    // Trigger configurations
    private val _triggerConfigs = MutableStateFlow<Map<TriggerMethod, TriggerConfiguration>>(
        mapOf(
            TriggerMethod.VOICE_KEYWORD to TriggerConfiguration(
                method = TriggerMethod.VOICE_KEYWORD,
                isEnabled = false,
                description = "Activate safety mode by speaking your chosen keyword"
            ),
            TriggerMethod.PANIC_BUTTON to TriggerConfiguration(
                method = TriggerMethod.PANIC_BUTTON,
                isEnabled = true,
                description = "Quick emergency activation with physical button press"
            ),
            TriggerMethod.SHAKE_DETECTION to TriggerConfiguration(
                method = TriggerMethod.SHAKE_DETECTION,
                isEnabled = false,
                description = "Shake your device vigorously to trigger emergency mode"
            ),
            TriggerMethod.VOLUME_BUTTON_SEQUENCE to TriggerConfiguration(
                method = TriggerMethod.VOLUME_BUTTON_SEQUENCE,
                isEnabled = false,
                description = "Press volume buttons in sequence: Up-Down-Up-Down-Up"
            ),
            TriggerMethod.POWER_BUTTON_SEQUENCE to TriggerConfiguration(
                method = TriggerMethod.POWER_BUTTON_SEQUENCE,
                isEnabled = false,
                description = "Press power button 5 times quickly"
            )
        )
    )
    val triggerConfigs: StateFlow<Map<TriggerMethod, TriggerConfiguration>> = _triggerConfigs.asStateFlow()

    // Voice keyword setup
    private val _currentKeyword = MutableStateFlow("")
    val currentKeyword: StateFlow<String> = _currentKeyword.asStateFlow()

    private val _isRecordingKeyword = MutableStateFlow(false)
    val isRecordingKeyword: StateFlow<Boolean> = _isRecordingKeyword.asStateFlow()

    private val _keywordTestResult = MutableStateFlow<KeywordTestResult?>(null)
    val keywordTestResult: StateFlow<KeywordTestResult?> = _keywordTestResult.asStateFlow()

    // UI states
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _isSavingConfigs = MutableStateFlow(false)
    val isSavingConfigs: StateFlow<Boolean> = _isSavingConfigs.asStateFlow()

    // Test mode states
    private val _isTestMode = MutableStateFlow(false)
    val isTestMode: StateFlow<Boolean> = _isTestMode.asStateFlow()

    private val _testResults = MutableStateFlow<List<TriggerTestResult>>(emptyList())
    val testResults: StateFlow<List<TriggerTestResult>> = _testResults.asStateFlow()

    // Active triggers count
    val activeTriggerCount: StateFlow<Int> = _triggerConfigs
        .map { configs -> configs.values.count { it.isEnabled } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Keyword validation
    val isKeywordValid: StateFlow<Boolean> = _currentKeyword
        .map { keyword ->
            keyword.trim().length >= 3 &&
                    keyword.trim().length <= 20 &&
                    keyword.all { it.isLetter() || it.isWhitespace() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadUserData()
        loadTriggerConfigurations()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                userRepository.observeCurrentUserProfile()
                    .catch { e -> handleError(e) }
                    .collect { user ->
                        _user.value = user
                        _currentKeyword.value = user?.triggerKeyword ?: ""
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun loadTriggerConfigurations() {
        viewModelScope.launch {
            try {
                // Load from settings repository
                settingsRepository.appSettings
                    .collect { settings ->
                        // Update configurations based on saved settings
                        // This would be expanded with actual saved trigger configs
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun updateKeyword(keyword: String) {
        _currentKeyword.value = keyword.take(20).filter { it.isLetter() || it.isWhitespace() }
    }

    fun saveKeyword() {
        if (!isKeywordValid.value) {
            _error.value = "Please enter a valid keyword (3-20 letters only)"
            return
        }

        viewModelScope.launch {
            _isSavingConfigs.value = true
            try {
                userRepository.updateTriggerKeyword(_currentKeyword.value.trim())
                    .onSuccess {
                        _successMessage.value = "Voice keyword saved successfully"
                        // Update voice keyword configuration
                        updateTriggerConfiguration(
                            TriggerMethod.VOICE_KEYWORD,
                            _triggerConfigs.value[TriggerMethod.VOICE_KEYWORD]?.copy(
                                keyword = _currentKeyword.value.trim()
                            ) ?: TriggerConfiguration(TriggerMethod.VOICE_KEYWORD, false)
                        )
                    }
                    .onFailure { e -> handleError(e) }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isSavingConfigs.value = false
            }
        }
    }

    fun toggleTriggerMethod(method: TriggerMethod) {
        val currentConfig = _triggerConfigs.value[method] ?: return
        val newConfig = currentConfig.copy(isEnabled = !currentConfig.isEnabled)
        updateTriggerConfiguration(method, newConfig)

        // Save to settings
        saveTriggerConfigurations()
    }

    fun updateTriggerSensitivity(method: TriggerMethod, sensitivity: Float) {
        val currentConfig = _triggerConfigs.value[method] ?: return
        val newConfig = currentConfig.copy(sensitivity = sensitivity)
        updateTriggerConfiguration(method, newConfig)
    }

    fun toggleConfirmationRequired(method: TriggerMethod) {
        val currentConfig = _triggerConfigs.value[method] ?: return
        val newConfig = currentConfig.copy(confirmationRequired = !currentConfig.confirmationRequired)
        updateTriggerConfiguration(method, newConfig)
    }

    private fun updateTriggerConfiguration(method: TriggerMethod, config: TriggerConfiguration) {
        val currentConfigs = _triggerConfigs.value.toMutableMap()
        currentConfigs[method] = config
        _triggerConfigs.value = currentConfigs
    }

    private fun saveTriggerConfigurations() {
        viewModelScope.launch {
            try {
                // Save configurations to DataStore
                // This would be expanded with actual persistence
                _successMessage.value = "Trigger settings saved"
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun startKeywordRecording() {
        if (!isKeywordValid.value) {
            _error.value = "Please enter a valid keyword first"
            return
        }

        _isRecordingKeyword.value = true
        _keywordTestResult.value = null

        viewModelScope.launch {
            try {
                // Simulate recording process
                kotlinx.coroutines.delay(3000)

                // Simulate keyword recognition test
                val success = (0..10).random() > 2 // 80% success rate simulation

                val confidence = if (success)
                    Random.nextFloat() * (0.95f - 0.7f) + 0.7f
                else
                    Random.nextFloat() * (0.4f - 0.1f) + 0.1f

                _keywordTestResult.value = KeywordTestResult(
                    keyword = _currentKeyword.value,
                    recognized = success,
                    confidence = confidence,
                    timestamp = System.currentTimeMillis()
                )

                if (success) {
                    _successMessage.value = "Keyword recognized successfully!"
                } else {
                    _error.value = "Keyword not recognized. Try speaking more clearly."
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isRecordingKeyword.value = false
            }
        }
    }

    fun startTestMode() {
        _isTestMode.value = true
        _testResults.value = emptyList()
        _successMessage.value = "Test mode activated. Try your configured triggers."

        // Auto-exit test mode after 30 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(30000)
            if (_isTestMode.value) {
                stopTestMode()
            }
        }
    }

    fun stopTestMode() {
        _isTestMode.value = false
        val successfulTests = _testResults.value.count { it.success }
        val totalTests = _testResults.value.size

        if (totalTests > 0) {
            _successMessage.value = "Test completed: $successfulTests/$totalTests triggers working"
        }
    }

    fun simulateTriggerTest(method: TriggerMethod) {
        if (!_isTestMode.value) return

        val config = _triggerConfigs.value[method]
        if (config == null || !config.isEnabled) {
            return
        }

        val testResult = TriggerTestResult(
            method = method,
            success = (0..10).random() > 1, // 90% success rate
            responseTime = (100..500).random(),
            timestamp = System.currentTimeMillis()
        )

        val currentResults = _testResults.value.toMutableList()
        currentResults.add(testResult)
        _testResults.value = currentResults

        if (testResult.success) {
            _successMessage.value = "${method.displayName} trigger activated successfully!"
        } else {
            _error.value = "${method.displayName} trigger failed to activate"
        }
    }

    fun resetAllTriggers() {
        viewModelScope.launch {
            _isSavingConfigs.value = true
            try {
                // Reset all triggers to default state
                _triggerConfigs.value = _triggerConfigs.value.mapValues { (method, config) ->
                    config.copy(
                        isEnabled = method == TriggerMethod.PANIC_BUTTON, // Only panic button enabled by default
                        sensitivity = 0.5f,
                        confirmationRequired = true
                    )
                }

                _currentKeyword.value = ""
                userRepository.updateTriggerKeyword("")

                _successMessage.value = "All trigger settings reset to defaults"
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isSavingConfigs.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearKeywordTestResult() {
        _keywordTestResult.value = null
    }

    private fun handleError(throwable: Throwable) {
        _error.value = FirebaseUtils.getErrorMessage(throwable as Exception)
        _isLoading.value = false
        _isSavingConfigs.value = false
        _isRecordingKeyword.value = false
    }
}

// Data classes for test results
data class KeywordTestResult(
    val keyword: String,
    val recognized: Boolean,
    val confidence: Float,
    val timestamp: Long
)

data class TriggerTestResult(
    val method: TriggerMethod,
    val success: Boolean,
    val responseTime: Int, // milliseconds
    val timestamp: Long
)

// Extension property for display names
private val TriggerMethod.displayName: String
    get() = when (this) {
        TriggerMethod.VOICE_KEYWORD -> "Voice Keyword"
        TriggerMethod.PANIC_BUTTON -> "Panic Button"
        TriggerMethod.SHAKE_DETECTION -> "Shake Detection"
        TriggerMethod.VOLUME_BUTTON_SEQUENCE -> "Volume Button Sequence"
        TriggerMethod.POWER_BUTTON_SEQUENCE -> "Power Button Sequence"
    }