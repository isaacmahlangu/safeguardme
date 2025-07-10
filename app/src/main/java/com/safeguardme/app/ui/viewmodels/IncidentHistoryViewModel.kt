// ui/viewmodels/IncidentHistoryViewModel.kt
package com.safeguardme.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.data.models.Incident
import com.safeguardme.app.data.repositories.IncidentRepository
import com.safeguardme.app.utils.FirebaseUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class IncidentFilter {
    ALL,
    SUBMITTED,
    NOT_SUBMITTED
}

@HiltViewModel
class IncidentHistoryViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    // All incidents from repository
    private val _allIncidents = MutableStateFlow<List<Incident>>(emptyList())

    // Current filter selection
    private val _selectedFilter = MutableStateFlow(IncidentFilter.ALL)
    val selectedFilter: StateFlow<IncidentFilter> = _selectedFilter.asStateFlow()

    // Filtered incidents based on current filter
    val filteredIncidents: StateFlow<List<Incident>> = combine(
        _allIncidents,
        _selectedFilter
    ) { incidents, filter ->
        when (filter) {
            IncidentFilter.ALL -> incidents
            IncidentFilter.SUBMITTED -> incidents.filter { it.submittedToSAPS || it.submittedToNGO }
            IncidentFilter.NOT_SUBMITTED -> incidents.filter { !it.submittedToSAPS && !it.submittedToNGO }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI states
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Statistics for UI
    val totalIncidents: StateFlow<Int> = _allIncidents
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val submittedCount: StateFlow<Int> = _allIncidents
        .map { incidents -> incidents.count { it.submittedToSAPS || it.submittedToNGO } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notSubmittedCount: StateFlow<Int> = _allIncidents
        .map { incidents -> incidents.count { !it.submittedToSAPS && !it.submittedToNGO } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val criticalIncidents: StateFlow<Int> = _allIncidents
        .map { incidents -> incidents.count { it.severityLevel == com.safeguardme.app.data.models.SeverityLevel.CRITICAL } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Filter badge counts
    val filterCounts: StateFlow<Map<IncidentFilter, Int>> = combine(
        totalIncidents,
        submittedCount,
        notSubmittedCount
    ) { total, submitted, notSubmitted ->
        mapOf(
            IncidentFilter.ALL to total,
            IncidentFilter.SUBMITTED to submitted,
            IncidentFilter.NOT_SUBMITTED to notSubmitted
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        loadIncidents()
    }

    private fun loadIncidents() {
        viewModelScope.launch {
            try {
                incidentRepository.observeIncidents()
                    .catch { e -> handleError(e) }
                    .collect { incidents ->
                        _allIncidents.value = incidents
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun setFilter(filter: IncidentFilter) {
        _selectedFilter.value = filter
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                incidentRepository.getAllIncidents()
                    .onSuccess { incidents -> _allIncidents.value = incidents }
                    .onFailure { e -> handleError(e) }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun deleteIncident(incident: Incident) {
        viewModelScope.launch {
            try {
                incidentRepository.deleteIncident(incident.incidentId)
                    .onFailure { e -> handleError(e) }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    private fun handleError(throwable: Throwable) {
        _error.value = FirebaseUtils.getErrorMessage(throwable as Exception)
        _isLoading.value = false
        _isRefreshing.value = false
    }
}