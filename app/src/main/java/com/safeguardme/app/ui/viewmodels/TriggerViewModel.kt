// ui/viewmodels/TriggerViewModel.kt - Enhanced with Comprehensive Permission Management
package com.safeguardme.app.ui.viewmodels

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.data.repositories.StorageRepository
import com.safeguardme.app.data.repositories.UserRepository
import com.safeguardme.app.utils.FirebaseUtils
import com.safeguardme.app.managers.PermissionManager
import com.safeguardme.app.managers.AppPermission
import com.safeguardme.app.managers.SpeechRecognitionManager
import com.safeguardme.app.managers.TranscriptionResult
import com.safeguardme.app.managers.KeywordMatchResult
import com.safeguardme.app.managers.MatchType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class TriggerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository,
    private val permissionManager: PermissionManager // ✅ NEW: Inject PermissionManager
) : ViewModel() {

    // ✅ NEW: Permission state tracking
    private val _permissionStatus = MutableStateFlow(PermissionCheckResult())
    val permissionStatus: StateFlow<PermissionCheckResult> = _permissionStatus.asStateFlow()

    private val _showPermissionRequest = MutableStateFlow<AppPermission?>(null)
    val showPermissionRequest: StateFlow<AppPermission?> = _showPermissionRequest.asStateFlow()

    // Keyword state
    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> = _keyword.asStateFlow()

    private val _keywordError = MutableStateFlow<String?>(null)
    val keywordError: StateFlow<String?> = _keywordError.asStateFlow()

    // Recording state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingTime = MutableStateFlow(0)
    val recordingTime: StateFlow<Int> = _recordingTime.asStateFlow()

    private val _amplitude = MutableStateFlow(0)
    val amplitude: StateFlow<Int> = _amplitude.asStateFlow()

    private val _hasRecording = MutableStateFlow(false)
    val hasRecording: StateFlow<Boolean> = _hasRecording.asStateFlow()

    // Transcription state
    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing.asStateFlow()

    private val _transcriptionResult = MutableStateFlow<TranscriptionResult?>(null)
    val transcriptionResult: StateFlow<TranscriptionResult?> = _transcriptionResult.asStateFlow()

    private val _keywordMatchResult = MutableStateFlow<KeywordMatchResult?>(null)
    val keywordMatchResult: StateFlow<KeywordMatchResult?> = _keywordMatchResult.asStateFlow()

    private val _showTranscriptionDialog = MutableStateFlow(false)
    val showTranscriptionDialog: StateFlow<Boolean> = _showTranscriptionDialog.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Save state
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // UI states
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Detection status (Phase 1: always OFF)
    private val _detectionEnabled = MutableStateFlow(false)
    val detectionEnabled: StateFlow<Boolean> = _detectionEnabled.asStateFlow()

    // ✅ ENHANCED: Form validation includes permission check
    val canSave: StateFlow<Boolean> = combine(
        _keyword,
        _hasRecording,
        _isSaving,
        _keywordMatchResult,
        _permissionStatus
    ) { keyword, hasRecording, isSaving, matchResult, permissionStatus ->
        keyword.trim().length >= 3 &&
                hasRecording &&
                !isSaving &&
                matchResult?.isMatch == true &&
                permissionStatus.canSaveToStorage
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ✅ ENHANCED: Recording capability based on permissions
    val canRecord: StateFlow<Boolean> = _permissionStatus.map { status ->
        status.canRecordAudio
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Speech recognition manager
    private lateinit var speechRecognitionManager: SpeechRecognitionManager

    // Recording objects
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null

    companion object {
        private const val MAX_RECORDING_TIME = 5 // 5 seconds max
        private const val MIN_RECORDING_TIME = 1500L // 1.5 seconds min
        private val FORBIDDEN_KEYWORDS = setOf(
            "hello", "hi", "hey", "okay", "yes", "no", "stop", "go", "start",
            "android", "google", "alexa", "siri", "cortana"
        )
    }

    init {
        speechRecognitionManager = SpeechRecognitionManager(context)

        // ✅ NEW: Initialize permission checking
        initializePermissionChecking()

        // ✅ NEW: Check permissions on startup
        checkAllPermissions()
    }

    /**
     * ✅ NEW: Initialize permission state monitoring
     */
    private fun initializePermissionChecking() {
        // Monitor permission changes from PermissionManager
        viewModelScope.launch {
            combine(
                permissionManager.audioGranted,
                permissionManager.storageGranted
            ) { audioGranted, storageGranted ->
                PermissionCheckResult(
                    canRecordAudio = audioGranted,
                    canSaveToStorage = storageGranted,
                    lastChecked = System.currentTimeMillis()
                )
            }.collect { result ->
                _permissionStatus.value = result
            }
        }
    }

    /**
     * ✅ NEW: Comprehensive permission checking
     */
    fun checkAllPermissions() {
        viewModelScope.launch {
            try {
                val audioGranted = permissionManager.isPermissionGranted(AppPermission.AUDIO_RECORDING)
                val storageGranted = permissionManager.isPermissionGranted(AppPermission.STORAGE)

                _permissionStatus.value = PermissionCheckResult(
                    canRecordAudio = audioGranted,
                    canSaveToStorage = storageGranted,
                    lastChecked = System.currentTimeMillis()
                )

                // Log permission status for debugging
                android.util.Log.d("TriggerViewModel", "🔍 Permission check:")
                android.util.Log.d("TriggerViewModel", "   🎤 Audio: $audioGranted")
                android.util.Log.d("TriggerViewModel", "   💾 Storage: $storageGranted")

            } catch (e: Exception) {
                android.util.Log.e("TriggerViewModel", "❌ Error checking permissions", e)
                _error.value = "Error checking permissions: ${e.message}"
            }
        }
    }

    /**
     * ✅ NEW: Request specific permission
     */
    fun requestPermission(permission: AppPermission) {
        viewModelScope.launch {
            try {
                permissionManager.requestPermission(permission) { granted ->
                    android.util.Log.d("TriggerViewModel", "🎯 Permission ${permission.name} result: $granted")

                    if (granted) {
                        _successMessage.value = when (permission) {
                            AppPermission.AUDIO_RECORDING -> "Microphone access granted! You can now record your voice keyword."
                            AppPermission.STORAGE -> "Storage access granted! You can now save your voice trigger settings."
                            else -> "Permission granted successfully!"
                        }
                    } else {
                        _error.value = when (permission) {
                            AppPermission.AUDIO_RECORDING -> "Microphone access is required to record your voice keyword. Please grant this permission in Settings."
                            AppPermission.STORAGE -> "Storage access is required to save your voice trigger. You can still use the app with limited functionality."
                            else -> "Permission denied. Some features may not work properly."
                        }
                    }

                    // Hide permission request dialog
                    _showPermissionRequest.value = null

                    // Refresh permission status
                    checkAllPermissions()
                }
            } catch (e: Exception) {
                android.util.Log.e("TriggerViewModel", "❌ Error requesting permission", e)
                _error.value = "Error requesting permission: ${e.message}"
                _showPermissionRequest.value = null
            }
        }
    }

    /**
     * ✅ NEW: Show permission request dialog
     */
    fun showPermissionDialog(permission: AppPermission) {
        _showPermissionRequest.value = permission
    }

    /**
     * ✅ NEW: Dismiss permission request dialog
     */
    fun dismissPermissionDialog() {
        _showPermissionRequest.value = null
    }

    fun updateKeyword(newKeyword: String) {
        val sanitized = newKeyword.lowercase().filter { it.isLetter() }.take(20)
        _keyword.value = sanitized
        _keywordError.value = null

        // Clear previous transcription results when keyword changes
        _transcriptionResult.value = null
        _keywordMatchResult.value = null
    }

    /**
     * ✅ ENHANCED: Permission-aware recording start
     */
    fun startRecording() {
        // ✅ CRITICAL: Check permissions before proceeding
        if (!_permissionStatus.value.canRecordAudio) {
            android.util.Log.w("TriggerViewModel", "🎤 Audio permission not granted")
            _showPermissionRequest.value = AppPermission.AUDIO_RECORDING
            return
        }

        if (!validateKeyword()) return

        viewModelScope.launch {
            try {
                android.util.Log.d("TriggerViewModel", "🎤 Starting recording with permissions verified")

                // Clear previous results
                _transcriptionResult.value = null
                _keywordMatchResult.value = null

                initializeRecording()
                _isRecording.value = true
                _recordingTime.value = 0

                // Start recording timer and amplitude monitoring
                startRecordingTimer()

            } catch (e: Exception) {
                android.util.Log.e("TriggerViewModel", "❌ Recording failed", e)
                handleError(e)
                stopRecording()
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            try {
                _isRecording.value = false
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null

                // Check recording duration
                val duration = _recordingTime.value
                if (duration * 1000L < MIN_RECORDING_TIME) {
                    _error.value = "Recording too short. Please record for at least 1.5 seconds."
                    deleteRecording()
                    return@launch
                }

                _hasRecording.value = true

                // Automatically transcribe the recording
                transcribeRecording()

            } catch (e: Exception) {
                android.util.Log.e("TriggerViewModel", "❌ Stop recording failed", e)
                handleError(e)
                deleteRecording()
            }
        }
    }

    /**
     * ✅ ENHANCED: Permission-aware transcription
     */
    private fun transcribeRecording() {
        val file = audioFile ?: return

        // ✅ NEW: Check if transcription is available (might need network/permissions)
        if (!isTranscriptionAvailable()) {
            android.util.Log.w("TriggerViewModel", "⚠️ Transcription not available, skipping verification")
            _keywordMatchResult.value = KeywordMatchResult(
                isMatch = true,
                confidence = 0.8f,
                matchType = MatchType.FUZZY,
                transcribedText = "Transcription skipped - no verification needed"
            )
            return
        }

        viewModelScope.launch {
            _isTranscribing.value = true

            try {
                android.util.Log.d("TriggerViewModel", "🔤 Starting transcription")

                val result = speechRecognitionManager.transcribeAudioFile(file)

                result.onSuccess { transcription ->
                    android.util.Log.d("TriggerViewModel", "✅ Transcription successful: ${transcription.primaryText}")
                    _transcriptionResult.value = transcription

                    // Check if transcription matches the expected keyword
                    val matchResult = speechRecognitionManager.isKeywordMatch(
                        transcription = transcription.primaryText,
                        expectedKeyword = _keyword.value,
                        threshold = 0.7f
                    )

                    _keywordMatchResult.value = matchResult
                    _showTranscriptionDialog.value = true

                }.onFailure { error ->
                    android.util.Log.w("TriggerViewModel", "⚠️ Transcription failed: ${error.message}")
                    _error.value = "Could not transcribe audio: ${error.message}. You can still save without verification."
                    // Allow saving even if transcription fails
                    _keywordMatchResult.value = KeywordMatchResult(
                        isMatch = true,
                        confidence = 0.5f,
                        matchType = MatchType.NO_MATCH,
                        transcribedText = "Transcription failed"
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e("TriggerViewModel", "❌ Transcription error", e)
                handleError(e)
            } finally {
                _isTranscribing.value = false
            }
        }
    }

    /**
     * ✅ NEW: Check if transcription service is available
     */
    private fun isTranscriptionAvailable(): Boolean {
        // Check if speech recognition is available and we have necessary permissions
        return try {
                    _permissionStatus.value.canRecordAudio
        } catch (e: Exception) {
            android.util.Log.w("TriggerViewModel", "⚠️ Transcription availability check failed", e)
            false
        }
    }

    fun acceptTranscription() {
        _showTranscriptionDialog.value = false
    }

    fun retryTranscription() {
        _showTranscriptionDialog.value = false
        transcribeRecording()
    }

    fun skipTranscriptionVerification() {
        _showTranscriptionDialog.value = false
        _keywordMatchResult.value = KeywordMatchResult(
            isMatch = true,
            confidence = 1.0f,
            matchType = MatchType.EXACT,
            transcribedText = "Manual verification"
        )
    }

    fun playRecording() {
        audioFile?.let { file ->
            if (!file.exists()) {
                _error.value = "Recording not found. Please record again."
                return
            }

            viewModelScope.launch {
                try {
                    if (_isPlaying.value) {
                        // Stop current playback
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        _isPlaying.value = false
                        return@launch
                    }

                    // Start playback
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(file.absolutePath)
                        prepare()
                        setOnCompletionListener {
                            _isPlaying.value = false
                            release()
                            mediaPlayer = null
                        }
                        start()
                    }
                    _isPlaying.value = true

                } catch (e: Exception) {
                    android.util.Log.e("TriggerViewModel", "❌ Playback failed", e)
                    handleError(e)
                    _isPlaying.value = false
                }
            }
        }
    }

    fun deleteRecording() {
        mediaPlayer?.release()
        mediaPlayer = null
        audioFile?.delete()
        audioFile = null
        _hasRecording.value = false
        _isPlaying.value = false

        // Clear transcription results
        _transcriptionResult.value = null
        _keywordMatchResult.value = null
        _showTranscriptionDialog.value = false
    }

    /**
     * ✅ ENHANCED: Permission-aware save with comprehensive checks
     */
    fun saveKeywordAndSample() {
        // ✅ CRITICAL: Check can save before proceeding
        if (!canSave.value) {
            android.util.Log.w("TriggerViewModel", "❌ Cannot save - requirements not met")

            val status = _permissionStatus.value
            when {
                !status.canSaveToStorage -> {
                    _showPermissionRequest.value = AppPermission.STORAGE
                    return
                }
                _keyword.value.trim().length < 3 -> {
                    _error.value = "Please enter a keyword of at least 3 characters"
                    return
                }
                !_hasRecording.value -> {
                    _error.value = "Please record your voice keyword first"
                    return
                }
                _keywordMatchResult.value?.isMatch != true -> {
                    _error.value = "Please verify your recording matches the keyword"
                    return
                }
                else -> {
                    _error.value = "Unable to save. Please check all requirements."
                    return
                }
            }
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val file = audioFile ?: throw Exception("No recording found")
                val keyword = _keyword.value.trim()
                val transcription = _transcriptionResult.value
                val matchResult = _keywordMatchResult.value

                android.util.Log.d("TriggerViewModel", "💾 Starting save process")
                android.util.Log.d("TriggerViewModel", "   📝 Keyword: $keyword")
                android.util.Log.d("TriggerViewModel", "   📁 File size: ${file.length()} bytes")

                // Check storage permission one more time before upload
                if (!_permissionStatus.value.canSaveToStorage) {
                    throw Exception("Storage permission required for saving")
                }

                // Upload audio to Firebase Storage
                val audioUri = Uri.fromFile(file)
                val uploadResult = storageRepository.uploadVoiceTriggerAudio(
                    audioUri = audioUri,
                    keyword = keyword
                )

                uploadResult.onSuccess { audioUrl ->
                    android.util.Log.d("TriggerViewModel", "✅ Audio uploaded successfully")

                    // Update user profile with comprehensive trigger data
                    val triggerData = mapOf(
                        "triggerKeyword" to keyword,
                        "voiceAudioUrl" to audioUrl,
                        "transcriptionData" to mapOf(
                            "primaryText" to (transcription?.primaryText ?: ""),
                            "alternativeTexts" to (transcription?.alternativeTexts ?: emptyList<String>()),
                            "confidence" to (transcription?.confidence ?: 0f),
                            "matchResult" to mapOf(
                                "isMatch" to (matchResult?.isMatch ?: false),
                                "confidence" to (matchResult?.confidence ?: 0f),
                                "matchType" to (matchResult?.matchType?.name ?: "UNKNOWN"),
                                "transcribedText" to (matchResult?.transcribedText ?: "")
                            )
                        ),
                        "createdAt" to System.currentTimeMillis(),
                        "audioFileSize" to file.length(),
                        "recordingDuration" to _recordingTime.value,
                        "permissionStatus" to mapOf(
                            "audioGranted" to _permissionStatus.value.canRecordAudio,
                            "storageGranted" to _permissionStatus.value.canSaveToStorage
                        )
                    )

                    userRepository.updateUserProfile(triggerData)
                        .onSuccess {
                            android.util.Log.d("TriggerViewModel", "✅ Profile updated successfully")
                            _successMessage.value = "Voice keyword saved successfully! Your trigger word '$keyword' is now active."
                            clearForm()
                        }
                        .onFailure { e ->
                            android.util.Log.e("TriggerViewModel", "❌ Profile update failed", e)
                            handleError(e)
                        }

                }.onFailure { e ->
                    android.util.Log.e("TriggerViewModel", "❌ Audio upload failed", e)
                    handleError(e)
                }

            } catch (e: Exception) {
                android.util.Log.e("TriggerViewModel", "❌ Save operation failed", e)
                handleError(e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * ✅ ENHANCED: Load existing trigger data with permission awareness
     */
    fun loadExistingTriggerData() {
        viewModelScope.launch {
            try {
                android.util.Log.d("TriggerViewModel", "📋 Loading existing trigger data")

                userRepository.getCurrentUserProfile()
                    .onSuccess { user ->
                        user?.triggerKeyword?.let { keyword ->
                            if (keyword.isNotBlank()) {
                                _keyword.value = keyword
                                _successMessage.value = "Existing trigger keyword: '$keyword'"
                                android.util.Log.d("TriggerViewModel", "✅ Loaded existing keyword: $keyword")
                            }
                        }
                    }
                    .onFailure {
                        android.util.Log.w("TriggerViewModel", "⚠️ Could not load user profile")
                        // Non-critical error - user might not have profile yet
                    }
            } catch (e: Exception) {
                android.util.Log.w("TriggerViewModel", "⚠️ Error loading trigger data", e)
                // Non-critical error
            }
        }
    }

    // TODO: Phase 2 - Enable/disable detection service
    fun toggleDetection() {
        // Phase 1: No-op
        // Phase 2: Start/stop foreground service for keyword detection
        android.util.Log.d("TriggerViewModel", "🔄 Detection toggle (Phase 2 feature)")
    }

    /**
     * ✅ ENHANCED: Permission-aware recording initialization
     */
    private fun initializeRecording() {
        // Double-check audio permission
        if (!_permissionStatus.value.canRecordAudio) {
            throw SecurityException("Audio recording permission not granted")
        }

        // Create audio file
        val audioDir = File(context.cacheDir, "audio")
        if (!audioDir.exists()) {
            if (!audioDir.mkdirs()) {
                throw Exception("Could not create audio directory")
            }
        }

        audioFile = File(audioDir, "trigger_sample_${System.currentTimeMillis()}.m4a")

        // Initialize MediaRecorder
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64000)
            setAudioSamplingRate(16000)
            setOutputFile(audioFile!!.absolutePath)

            prepare()
            start()
        }

        android.util.Log.d("TriggerViewModel", "🎤 Recording initialized: ${audioFile?.name}")
    }

    private suspend fun startRecordingTimer() {
        while (_isRecording.value && _recordingTime.value < MAX_RECORDING_TIME) {
            delay(1000L)
            if (_isRecording.value) {
                _recordingTime.value += 1

                // Update amplitude for waveform visualization
                try {
                    mediaRecorder?.maxAmplitude?.let { amplitude ->
                        _amplitude.value = amplitude / 1000 // Normalize for UI
                    }
                } catch (e: Exception) {
                    // MediaRecorder might not be ready
                }
            }
        }

        // Auto-stop if max time reached
        if (_isRecording.value && _recordingTime.value >= MAX_RECORDING_TIME) {
            stopRecording()
        }
    }

    private fun validateKeyword(): Boolean {
        val keyword = _keyword.value.trim()

        when {
            keyword.isEmpty() -> {
                _keywordError.value = "Please enter a keyword"
                return false
            }
            keyword.length < 3 -> {
                _keywordError.value = "Keyword must be at least 3 characters"
                return false
            }
            keyword.length > 15 -> {
                _keywordError.value = "Keyword must be 15 characters or less"
                return false
            }
            FORBIDDEN_KEYWORDS.contains(keyword.lowercase()) -> {
                _keywordError.value = "This keyword may cause false triggers. Please choose a unique word."
                return false
            }
            !keyword.all { it.isLetter() } -> {
                _keywordError.value = "Keyword must contain only letters"
                return false
            }
            else -> {
                _keywordError.value = null
                return true
            }
        }
    }

    private fun clearForm() {
        _keyword.value = ""
        _keywordError.value = null
        deleteRecording()
        _recordingTime.value = 0
        _amplitude.value = 0
        _transcriptionResult.value = null
        _keywordMatchResult.value = null
        _showTranscriptionDialog.value = false
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun dismissTranscriptionDialog() {
        _showTranscriptionDialog.value = false
    }

    private fun handleError(throwable: Throwable) {
        val errorMessage = when (throwable) {
            is SecurityException -> "Permission denied: ${throwable.message}"
            else -> FirebaseUtils.getErrorMessage(throwable as? Exception ?: Exception(throwable))
        }

        _error.value = errorMessage
        _isRecording.value = false
        _isPlaying.value = false
        _isSaving.value = false
        _isTranscribing.value = false

        android.util.Log.e("TriggerViewModel", "❌ Error handled: $errorMessage", throwable)
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaPlayer?.release()
        android.util.Log.d("TriggerViewModel", "🧹 ViewModel cleared")
    }
}

/**
 * ✅ NEW: Permission check result data class
 */
data class PermissionCheckResult(
    val canRecordAudio: Boolean = false,
    val canSaveToStorage: Boolean = false,
    val lastChecked: Long = 0L
) {
    val hasAllRequiredPermissions: Boolean = canRecordAudio
    val hasOptionalPermissions: Boolean = canSaveToStorage
    val hasCriticalPermissions: Boolean = canRecordAudio

    fun getMissingPermissions(): List<AppPermission> {
        val missing = mutableListOf<AppPermission>()
        if (!canRecordAudio) missing.add(AppPermission.AUDIO_RECORDING)
        if (!canSaveToStorage) missing.add(AppPermission.STORAGE)
        return missing
    }

    fun getPermissionSummary(): String {
        return when {
            hasAllRequiredPermissions && hasOptionalPermissions -> "All permissions granted"
            hasAllRequiredPermissions -> "Core permissions granted (storage optional)"
            else -> "Microphone permission required"
        }
    }
}