// ui/viewmodels/TriggerViewModel.kt
package com.safeguardme.app.ui.viewmodels

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.data.repositories.StorageRepository
import com.safeguardme.app.data.repositories.UserRepository
import com.safeguardme.app.utils.FirebaseUtils
import com.safeguardme.app.utils.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class TriggerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

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

    // Form validation
    val canSave: StateFlow<Boolean> = combine(
        _keyword,
        _hasRecording,
        _isSaving
    ) { keyword, hasRecording, isSaving ->
        keyword.trim().length >= 3 && hasRecording && !isSaving
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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

    fun updateKeyword(newKeyword: String) {
        val sanitized = newKeyword.lowercase().filter { it.isLetter() }.take(20)
        _keyword.value = sanitized
        _keywordError.value = null
    }

    fun startRecording() {
        if (!PermissionUtils.isAudioRecordingPermissionGranted(context)) {
            _error.value = "Microphone permission is required for voice keyword setup"
            return
        }

        if (!validateKeyword()) return

        viewModelScope.launch {
            try {
                initializeRecording()
                _isRecording.value = true
                _recordingTime.value = 0

                // Start recording timer and amplitude monitoring
                startRecordingTimer()

            } catch (e: Exception) {
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

            } catch (e: Exception) {
                handleError(e)
                deleteRecording()
            }
        }
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
    }

    fun saveKeywordAndSample() {
        if (!canSave.value) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val file = audioFile ?: throw Exception("No recording found")

                // Generate sample hash for integrity
                val sampleHash = generateFileHash(file)

                // Upload audio sample to Firebase Storage
                val audioUri = Uri.fromFile(file)
                val uploadResult = storageRepository.uploadAudioEvidence(
                    uri = audioUri,
                    contentType = "audio/mpeg",
                    incidentId = "trigger_sample_${System.currentTimeMillis()}"
                )

                uploadResult.onSuccess { audioUrl ->
                    // Update user profile with trigger keyword and audio URL
                    userRepository.updateTriggerKeyword(_keyword.value.trim())
                        .onSuccess {
                            _successMessage.value = "Voice keyword saved successfully! Your trigger word is now active."
                            clearForm()
                        }
                        .onFailure { e -> handleError(e) }

                }.onFailure { e -> handleError(e) }

            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isSaving.value = false
            }
        }
    }

    // TODO: Phase 2 - Enable/disable detection service
    fun toggleDetection() {
        // Phase 1: No-op
        // Phase 2: Start/stop foreground service for keyword detection
    }

    private fun initializeRecording() {
        // Create audio file
        val audioDir = File(context.cacheDir, "audio")
        if (!audioDir.exists()) audioDir.mkdirs()

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

    private fun generateFileHash(file: File): String {
        return try {
            val bytes = file.readBytes()
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(bytes)
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "hash_${System.currentTimeMillis()}"
        }
    }

    private fun clearForm() {
        _keyword.value = ""
        _keywordError.value = null
        deleteRecording()
        _recordingTime.value = 0
        _amplitude.value = 0
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    private fun handleError(throwable: Throwable) {
        _error.value = FirebaseUtils.getErrorMessage(throwable as Exception)
        _isRecording.value = false
        _isPlaying.value = false
        _isSaving.value = false
    }

    override fun onCleared() {
        super.onCleared()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }
}