// ui/viewmodels/HomeViewModel.kt
package com.safeguardme.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.data.models.Incident
import com.safeguardme.app.data.models.SafetyStatus
import com.safeguardme.app.data.models.User
import com.safeguardme.app.data.repositories.IncidentRepository
import com.safeguardme.app.data.repositories.SafetyRepository
import com.safeguardme.app.data.repositories.UserRepository
import com.safeguardme.app.utils.FirebaseUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val incidentRepository: IncidentRepository,
    private val safetyRepository: SafetyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Exposed properties for backward compatibility (if needed)
    val user: StateFlow<User?> = uiState.map { it.user }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val recentIncidents: StateFlow<List<Incident>> = uiState.map { it.recentIncidents }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isLoading: StateFlow<Boolean> = uiState.map { it.isLoading }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val error: StateFlow<String?> = uiState.map { it.error }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val isRefreshing: StateFlow<Boolean> = uiState.map { it.isRefreshing }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val safetyStatus: StateFlow<SafetyStatus> = uiState.map { it.safetyStatus }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SafetyStatus.DISABLED
    )

    val safetyStatusText: StateFlow<String> = uiState.map { it.safetyStatusText }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Loading..."
    )

    val safetyStatusColor: StateFlow<androidx.compose.ui.graphics.Color> = uiState.map { it.safetyStatusColor }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = androidx.compose.ui.graphics.Color.Gray
    )

    init {
        initializeDataStreams()
    }

    private fun initializeDataStreams() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Combine all data streams with error handling
                combine(
                    userRepository.observeCurrentUserProfile().catch { emit(null) },
                    incidentRepository.observeIncidents().map { it.take(3) }.catch { emit(emptyList()) },
                    safetyRepository.observeSafetyStatus().catch { emit(SafetyStatus.DISABLED) }
                ) { user, incidents, safetyStatus ->
                    createUiState(
                        user = user,
                        incidents = incidents,
                        safetyStatus = safetyStatus,
                        isLoading = false,
                        error = null
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                handleError(e, "Failed to initialize data")
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                // Execute all refresh operations concurrently
                val refreshOperations = listOf(
                    //async { userRepository.refreshCurrentUser() },
                    //async { incidentRepository.refreshIncidents() },
                    async { safetyRepository.refreshSafetyStatus() }
                )

                // Wait for all operations to complete
                refreshOperations.awaitAll()

                _uiState.update { it.copy(isRefreshing = false, error = null) }
            } catch (e: Exception) {
                handleError(e, "Failed to refresh data")
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun toggleSafetyStatus() {
        viewModelScope.launch {
            try {
                val currentStatus = _uiState.value.safetyStatus
                val newStatus = when (currentStatus) {
                    SafetyStatus.DISABLED -> SafetyStatus.ENABLED
                    SafetyStatus.ENABLED -> SafetyStatus.DISABLED
                    SafetyStatus.EMERGENCY -> SafetyStatus.DISABLED // Allow emergency override
                }

                // Optimistically update UI state
                _uiState.update {
                    createUiState(
                        user = it.user,
                        incidents = it.recentIncidents,
                        safetyStatus = newStatus,
                        isLoading = it.isLoading,
                        error = it.error
                    )
                }

                // Perform backend update
                safetyRepository.updateSafetyStatus(newStatus)
                    .onFailure { error ->
                        // Revert on failure
                        _uiState.update {
                            createUiState(
                                user = it.user,
                                incidents = it.recentIncidents,
                                safetyStatus = currentStatus,
                                isLoading = it.isLoading,
                                error = it.error
                            )
                        }
                        throw error
                    }

            } catch (e: Exception) {
                handleError(e, "Failed to update safety status")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun createUiState(
        user: User?,
        incidents: List<Incident>,
        safetyStatus: SafetyStatus,
        isLoading: Boolean,
        error: String?
    ): HomeUiState {
        return HomeUiState(
            user = user,
            recentIncidents = incidents,
            safetyStatus = safetyStatus,
            safetyStatusText = safetyStatus.toDisplayText(),
            safetyStatusColor = safetyStatus.toDisplayColor(),
            isLoading = isLoading,
            isRefreshing = _uiState.value.isRefreshing,
            error = error
        )
    }

    private fun handleError(throwable: Throwable, context: String = "An error occurred") {
        val errorMessage = when (throwable) {
            is Exception -> FirebaseUtils.getErrorMessage(throwable)
            else -> throwable.message ?: context
        }

        _uiState.update {
            it.copy(
                error = errorMessage,
                isLoading = false,
                isRefreshing = false
            )
        }
    }
}

// Consolidated UI State
data class HomeUiState(
    val user: User? = null,
    val recentIncidents: List<Incident> = emptyList(),
    val safetyStatus: SafetyStatus = SafetyStatus.DISABLED,
    val safetyStatusText: String = "Loading...",
    val safetyStatusColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

// Extension functions for type safety and consistency
private fun SafetyStatus.toDisplayText(): String = when (this) {
    SafetyStatus.ENABLED -> "Emergency Mode Active"
    SafetyStatus.EMERGENCY -> "EMERGENCY - Help Dispatched"
    SafetyStatus.DISABLED -> "Safety Mode Disabled"
}

private fun SafetyStatus.toDisplayColor(): androidx.compose.ui.graphics.Color = when (this) {
    SafetyStatus.ENABLED -> androidx.compose.ui.graphics.Color.Red
    SafetyStatus.EMERGENCY -> androidx.compose.ui.graphics.Color(0xFFFF1744)
    SafetyStatus.DISABLED -> androidx.compose.ui.graphics.Color.Green
}

// Extension for computed properties
val User.firstName: String
    get() = fullName.split(" ").firstOrNull() ?: "User"