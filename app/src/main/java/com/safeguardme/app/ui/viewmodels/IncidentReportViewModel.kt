// ui/viewmodels/IncidentReportViewModel.kt
package com.safeguardme.app.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.safeguardme.app.data.models.Incident
import com.safeguardme.app.data.models.IncidentType
import com.safeguardme.app.data.models.SeverityLevel
import com.safeguardme.app.data.repositories.IncidentRepository
import com.safeguardme.app.data.repositories.StorageRepository
import com.safeguardme.app.utils.FirebaseUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class IncidentReportViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    // Form state
    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _incidentType = MutableStateFlow(IncidentType.OTHER)
    val incidentType: StateFlow<IncidentType> = _incidentType.asStateFlow()

    private val _severityLevel = MutableStateFlow(SeverityLevel.MEDIUM)
    val severityLevel: StateFlow<SeverityLevel> = _severityLevel.asStateFlow()

    private val _incidentDate = MutableStateFlow(Date())
    val incidentDate: StateFlow<Date> = _incidentDate.asStateFlow()

    private val _submittedToSAPS = MutableStateFlow(false)
    val submittedToSAPS: StateFlow<Boolean> = _submittedToSAPS.asStateFlow()

    private val _submittedToNGO = MutableStateFlow(false)
    val submittedToNGO: StateFlow<Boolean> = _submittedToNGO.asStateFlow()

    // Evidence state
    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    private val _imageUploadProgress = MutableStateFlow<Map<Uri, Float>>(emptyMap())
    val imageUploadProgress: StateFlow<Map<Uri, Float>> = _imageUploadProgress.asStateFlow()

    // UI states
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submitProgress = MutableStateFlow(0f)
    val submitProgress: StateFlow<Float> = _submitProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Validation states
    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    private val _descriptionError = MutableStateFlow<String?>(null)
    val descriptionError: StateFlow<String?> = _descriptionError.asStateFlow()

    // Form validation
    val isFormValid: StateFlow<Boolean> = combine(
        _description,
        _location
    ) { description, location ->
        description.trim().length >= 10 && location.trim().isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Character counts
    val descriptionCharCount: StateFlow<String> = _description
        .map { "${it.length}/5000" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0/5000")

    fun updateLocation(location: String) {
        _location.value = location.take(200)
        _locationError.value = null
    }

    fun updateDescription(description: String) {
        _description.value = description.take(5000)
        _descriptionError.value = null
    }

    fun updateIncidentType(type: IncidentType) {
        _incidentType.value = type
    }

    fun updateSeverityLevel(level: SeverityLevel) {
        _severityLevel.value = level
    }

    fun updateIncidentDate(date: Date) {
        _incidentDate.value = date
    }

    fun updateSubmittedToSAPS(submitted: Boolean) {
        _submittedToSAPS.value = submitted
    }

    fun updateSubmittedToNGO(submitted: Boolean) {
        _submittedToNGO.value = submitted
    }

    fun addImage(uri: Uri) {
        val currentImages = _selectedImages.value
        if (currentImages.size < 5) { // Limit to 5 images for security and performance
            _selectedImages.value = currentImages + uri
        } else {
            _error.value = "Maximum 5 images allowed per incident"
        }
    }

    fun removeImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value.filter { it != uri }
        _imageUploadProgress.value = _imageUploadProgress.value - uri
    }

    fun submitIncident() {
        if (!validateForm()) return

        viewModelScope.launch {
            _isSubmitting.value = true
            _submitProgress.value = 0f

            try {
                // Step 1: Upload images (70% of progress)
                val imageUrls = uploadImages()
                _submitProgress.value = 0.7f

                // Step 2: Create incident (20% of progress)
                val incident = createIncident(imageUrls)
                _submitProgress.value = 0.9f

                // Step 3: Save to database (10% of progress)
                incidentRepository.reportIncident(incident)
                    .onSuccess {
                        _submitProgress.value = 1f
                        _successMessage.value = "Incident reported successfully. Your evidence is secure."
                        clearForm()
                    }
                    .onFailure { e -> handleError(e) }

            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    private suspend fun uploadImages(): List<String> {
        val imageUrls = mutableListOf<String>()
        val images = _selectedImages.value

        if (images.isEmpty()) return imageUrls

        // Generate incident ID for file naming
        val tempIncidentId = UUID.randomUUID().toString()

        images.forEachIndexed { index, uri ->
            try {
                // Update progress for this image
                updateImageUploadProgress(uri, 0f)

                // Upload image
                storageRepository.uploadEvidenceImage(
                    uri = uri,
                    contentType = "image/jpeg", // TODO: Get actual content type
                    incidentId = tempIncidentId
                ).onSuccess { downloadUrl ->
                    imageUrls.add(downloadUrl)
                    updateImageUploadProgress(uri, 1f)
                }.onFailure { e ->
                    // Continue with other images even if one fails
                    handleError(e)
                }

                // Update overall progress
                val overallProgress = (index + 1).toFloat() / images.size * 0.7f
                _submitProgress.value = overallProgress

            } catch (e: Exception) {
                handleError(e)
            }
        }

        return imageUrls
    }

    private fun updateImageUploadProgress(uri: Uri, progress: Float) {
        val currentProgress = _imageUploadProgress.value.toMutableMap()
        currentProgress[uri] = progress
        _imageUploadProgress.value = currentProgress
    }

    private fun createIncident(imageUrls: List<String>): Incident {
        return Incident(
            date = Timestamp(_incidentDate.value),
            location = _location.value.trim(),
            description = _description.value.trim(),
            incidentType = _incidentType.value,
            severityLevel = _severityLevel.value,
            submittedToSAPS = _submittedToSAPS.value,
            submittedToNGO = _submittedToNGO.value,
            imageUrls = imageUrls,
            isEmergencyReport = _severityLevel.value == SeverityLevel.CRITICAL,
            requiresFollowUp = _severityLevel.value in listOf(SeverityLevel.HIGH, SeverityLevel.CRITICAL)
        )
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validate location
        if (_location.value.trim().isEmpty()) {
            _locationError.value = "Location is required"
            isValid = false
        } else if (_location.value.trim().length < 3) {
            _locationError.value = "Please provide more specific location details"
            isValid = false
        }

        // Validate description
        if (_description.value.trim().isEmpty()) {
            _descriptionError.value = "Description is required"
            isValid = false
        } else if (_description.value.trim().length < 10) {
            _descriptionError.value = "Please provide more details (minimum 10 characters)"
            isValid = false
        }

        // Validate date (not in future)
        if (_incidentDate.value.after(Date())) {
            _error.value = "Incident date cannot be in the future"
            isValid = false
        }

        return isValid
    }

    private fun clearForm() {
        _location.value = ""
        _description.value = ""
        _incidentType.value = IncidentType.OTHER
        _severityLevel.value = SeverityLevel.MEDIUM
        _incidentDate.value = Date()
        _submittedToSAPS.value = false
        _submittedToNGO.value = false
        _selectedImages.value = emptyList()
        _imageUploadProgress.value = emptyMap()
        _locationError.value = null
        _descriptionError.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    private fun handleError(throwable: Throwable) {
        _error.value = FirebaseUtils.getErrorMessage(throwable as Exception)
        _isSubmitting.value = false
        _submitProgress.value = 0f
    }
}