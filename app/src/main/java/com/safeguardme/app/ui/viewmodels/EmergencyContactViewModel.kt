// =============================================
// EmergencyContactViewModel.kt
// =============================================

package com.safeguardme.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.data.models.ContactType
import com.safeguardme.app.data.models.EmergencyContact
import com.safeguardme.app.data.repositories.EmergencyContactRepository
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

@HiltViewModel
class EmergencyContactViewModel @Inject constructor(
    private val repository: EmergencyContactRepository
) : ViewModel() {

    // State management
    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Edit contact state
    private val _editingContact = MutableStateFlow<EmergencyContact?>(null)
    val editingContact: StateFlow<EmergencyContact?> = _editingContact.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()

    // Computed properties
    val highPriorityContacts: StateFlow<List<EmergencyContact>> = contacts
        .map { it.filter { contact -> contact.isHighPriority() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emergencyReadyContacts: StateFlow<List<EmergencyContact>> = contacts
        .map { it.filter { contact -> contact.canReceiveEmergencyNotifications() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contactsByType: StateFlow<Map<ContactType, List<EmergencyContact>>> = contacts
        .map { it.groupBy { contact -> contact.contactType } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        loadContacts()
        observeContacts()
    }

    /**
     * Load emergency contacts from repository
     */
    private fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllContacts()
                    .onSuccess { contacts ->
                        _contacts.value = contacts
                        if (contacts.isEmpty()) {
                            // Initialize default contacts for new users
                            repository.initializeDefaultContacts()
                        }
                    }
                    .onFailure { error ->
                        handleError(error)
                    }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Observe real-time contact changes
     */
    private fun observeContacts() {
        viewModelScope.launch {
            repository.observeContacts()
                .catch { e -> handleError(e) }
                .collect { contacts ->
                    _contacts.value = contacts
                }
        }
    }

    /**
     * Add or update emergency contact
     */
    fun saveContact(contact: EmergencyContact) {
        viewModelScope.launch {
            _isLoading.value = true
            _validationErrors.value = emptyList()

            try {
                repository.saveContact(contact)
                    .onSuccess { savedContact ->
                        _successMessage.value = if (contact.id == savedContact.id) {
                            "Contact updated successfully"
                        } else {
                            "Contact added successfully"
                        }
                        _editingContact.value = null
                        _showAddDialog.value = false
                    }
                    .onFailure { error ->
                        if (error is IllegalArgumentException) {
                            // Validation errors
                            _validationErrors.value = listOf(error.message ?: "Validation failed")
                        } else {
                            handleError(error)
                        }
                    }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete emergency contact
     */
    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteContact(contactId)
                    .onSuccess {
                        _successMessage.value = "Contact deleted successfully"
                    }
                    .onFailure { error ->
                        handleError(error)
                    }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Start editing a contact
     */
    fun startEditingContact(contact: EmergencyContact) {
        _editingContact.value = contact
        _showAddDialog.value = true
        _validationErrors.value = emptyList()
    }

    /**
     * Start adding a new contact
     */
    fun startAddingContact() {
        _editingContact.value = null
        _showAddDialog.value = true
        _validationErrors.value = emptyList()
    }

    /**
     * Cancel editing/adding contact
     */
    fun cancelEditing() {
        _editingContact.value = null
        _showAddDialog.value = false
        _validationErrors.value = emptyList()
    }

    /**
     * Verify a contact (simulate call/text verification)
     */
    fun verifyContact(contactId: String) {
        viewModelScope.launch {
            try {
                repository.updateContactVerification(contactId, true)
                    .onSuccess {
                        _successMessage.value = "Contact verified successfully"
                    }
                    .onFailure { error ->
                        handleError(error)
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Reorder contacts by priority
     */
    fun reorderContacts(contactIds: List<String>) {
        viewModelScope.launch {
            try {
                repository.reorderContacts(contactIds)
                    .onSuccess {
                        _successMessage.value = "Contacts reordered successfully"
                    }
                    .onFailure { error ->
                        handleError(error)
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Get emergency contacts for crisis situations
     */
    fun getEmergencyContacts(callback: (List<EmergencyContact>) -> Unit) {
        viewModelScope.launch {
            try {
                repository.getEmergencyContacts()
                    .onSuccess { contacts ->
                        callback(contacts)
                    }
                    .onFailure { error ->
                        handleError(error)
                        callback(emptyList())
                    }
            } catch (e: Exception) {
                handleError(e)
                callback(emptyList())
            }
        }
    }

    /**
     * Validate contact completeness for emergency situations
     */
    /*fun validateEmergencyReadiness(): ValidationResult {
        val currentContacts = _contacts.value
        return com.safeguardme.app.data.models.validateContactList(currentContacts)
    }*/

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Handle errors consistently
     */
    private fun handleError(throwable: Throwable) {
        _error.value = when (throwable) {
            is SecurityException -> throwable.message
            is IllegalArgumentException -> throwable.message
            else -> "An unexpected error occurred. Please try again."
        }
        _isLoading.value = false
    }
}

//private fun Unit.validateContactList(currentContacts: kotlin.collections.List<com.safeguardme.app.data.models.EmergencyContact>): com.safeguardme.app.data.models.ValidationResult {}
