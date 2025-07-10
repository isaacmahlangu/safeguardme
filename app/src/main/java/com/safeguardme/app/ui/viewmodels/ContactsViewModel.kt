// ui/viewmodels/ContactsViewModel.kt
package com.safeguardme.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.data.models.Contact
import com.safeguardme.app.data.models.TrustLevel
import com.safeguardme.app.data.repositories.ContactRepository
import com.safeguardme.app.utils.FirebaseUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {

    // Contacts state
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    // UI states
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _isAddingContact = MutableStateFlow(false)
    val isAddingContact: StateFlow<Boolean> = _isAddingContact.asStateFlow()

    // Add contact form states
    private val _newContactName = MutableStateFlow("")
    val newContactName: StateFlow<String> = _newContactName.asStateFlow()

    private val _newContactPhone = MutableStateFlow("")
    val newContactPhone: StateFlow<String> = _newContactPhone.asStateFlow()

    private val _newContactRelationship = MutableStateFlow("")
    val newContactRelationship: StateFlow<String> = _newContactRelationship.asStateFlow()

    private val _newContactIsPrimary = MutableStateFlow(false)
    val newContactIsPrimary: StateFlow<Boolean> = _newContactIsPrimary.asStateFlow()

    // Validation states
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val _phoneError = MutableStateFlow<String?>(null)
    val phoneError: StateFlow<String?> = _phoneError.asStateFlow()

    private val _relationshipError = MutableStateFlow<String?>(null)
    val relationshipError: StateFlow<String?> = _relationshipError.asStateFlow()

    // Contact limit check
    val canAddMoreContacts: StateFlow<Boolean> = _contacts
        .map { it.size < 10 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Primary contacts count
    val primaryContactsCount: StateFlow<Int> = _contacts
        .map { contacts -> contacts.count { it.isPrimary } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            try {
                contactRepository.observeContacts()
                    .catch { e -> handleError(e) }
                    .collect { contacts ->
                        _contacts.value = contacts
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun showAddContactDialog() {
        if (canAddMoreContacts.value) {
            clearAddContactForm()
            _showAddDialog.value = true
        } else {
            _error.value = "Maximum 10 contacts allowed for security and usability"
        }
    }

    fun hideAddContactDialog() {
        _showAddDialog.value = false
        clearAddContactForm()
    }

    fun updateNewContactName(name: String) {
        _newContactName.value = name.take(100) // Limit length
        _nameError.value = null
    }

    fun updateNewContactPhone(phone: String) {
        // Allow only numbers, +, -, (, ), and spaces
        val cleaned = phone.filter { it.isDigit() || it in "+()-" }
        _newContactPhone.value = cleaned.take(20)
        _phoneError.value = null
    }

    fun updateNewContactRelationship(relationship: String) {
        _newContactRelationship.value = relationship.take(50)
        _relationshipError.value = null
    }

    fun updateNewContactIsPrimary(isPrimary: Boolean) {
        if (isPrimary && primaryContactsCount.value >= 3) {
            _error.value = "Maximum 3 primary contacts allowed"
            return
        }
        _newContactIsPrimary.value = isPrimary
    }

    fun addContact() {
        if (!validateAddContactForm()) return

        viewModelScope.launch {
            _isAddingContact.value = true
            try {
                val contact = Contact(
                    name = _newContactName.value.trim(),
                    phoneNumber = _newContactPhone.value.trim(),
                    relationship = _newContactRelationship.value.trim(),
                    isPrimary = _newContactIsPrimary.value,
                    trustLevel = if (_newContactIsPrimary.value) TrustLevel.VERIFIED else TrustLevel.TRUSTED
                )

                contactRepository.addContact(contact)
                    .onSuccess {
                        _showAddDialog.value = false
                        clearAddContactForm()
                    }
                    .onFailure { e -> handleError(e) }

            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isAddingContact.value = false
            }
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            try {
                contactRepository.deleteContact(contact.contactId)
                    .onFailure { e -> handleError(e) }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun toggleContactPrimary(contact: Contact) {
        val newIsPrimary = !contact.isPrimary

        if (newIsPrimary && primaryContactsCount.value >= 3) {
            _error.value = "Maximum 3 primary contacts allowed"
            return
        }

        viewModelScope.launch {
            try {
                val updatedContact = contact.copy(
                    isPrimary = newIsPrimary,
                    trustLevel = if (newIsPrimary) TrustLevel.VERIFIED else TrustLevel.TRUSTED
                )

                contactRepository.updateContact(contact.contactId, updatedContact)
                    .onFailure { e -> handleError(e) }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun validateAddContactForm(): Boolean {
        var isValid = true

        // Validate name
        if (_newContactName.value.trim().isEmpty()) {
            _nameError.value = "Name is required"
            isValid = false
        } else if (_newContactName.value.trim().length < 2) {
            _nameError.value = "Name must be at least 2 characters"
            isValid = false
        }

        // Validate phone
        if (_newContactPhone.value.trim().isEmpty()) {
            _phoneError.value = "Phone number is required"
            isValid = false
        } else if (!FirebaseUtils.isValidPhoneNumber(_newContactPhone.value.trim())) {
            _phoneError.value = "Please enter a valid phone number"
            isValid = false
        }

        // Check for duplicate phone numbers
        val phoneExists = _contacts.value.any { contact ->
            contact.phoneNumber.replace(Regex("[^+0-9]"), "") ==
                    _newContactPhone.value.replace(Regex("[^+0-9]"), "")
        }
        if (phoneExists) {
            _phoneError.value = "This phone number is already added"
            isValid = false
        }

        // Validate relationship
        if (_newContactRelationship.value.trim().isEmpty()) {
            _relationshipError.value = "Relationship is required"
            isValid = false
        }

        return isValid
    }

    private fun clearAddContactForm() {
        _newContactName.value = ""
        _newContactPhone.value = ""
        _newContactRelationship.value = ""
        _newContactIsPrimary.value = false
        _nameError.value = null
        _phoneError.value = null
        _relationshipError.value = null
    }

    fun clearError() {
        _error.value = null
    }

    private fun handleError(throwable: Throwable) {
        _error.value = FirebaseUtils.getErrorMessage(throwable as Exception)
        _isLoading.value = false
        _isAddingContact.value = false
    }
}