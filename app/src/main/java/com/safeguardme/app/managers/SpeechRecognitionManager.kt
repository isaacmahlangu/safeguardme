// utils/SpeechRecognitionManager.kt - Voice Transcription System
package com.safeguardme.app.managers

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.safeguardme.app.utils.PermissionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * ✅ Speech Recognition Manager for Voice Trigger Transcription
 * Uses Android's built-in speech recognition to transcribe recorded audio
 */

@Singleton
class SpeechRecognitionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SpeechRecognition"
    }

    /**
     * ✅ Transcribe recorded audio file to text
     * This plays the audio and captures speech recognition in real-time
     */
    suspend fun transcribeAudioFile(audioFile: File): Result<TranscriptionResult> {
        return suspendCancellableCoroutine { continuation ->

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                continuation.resume(
                    Result.failure(Exception("Speech recognition not available on this device"))
                )
                return@suspendCancellableCoroutine
            }

            var mediaPlayer: MediaPlayer? = null
            var speechRecognizer: SpeechRecognizer? = null

            try {
                Log.d(TAG, "Starting transcription for: ${audioFile.name}")

                // Create speech recognizer
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

                val recognitionResults = mutableListOf<String>()
                var isCompleted = false

                speechRecognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "Ready for speech recognition")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "Beginning of speech detected")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Audio level changes - can be used for visual feedback
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Audio buffer received
                    }

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "End of speech detected")
                    }

                    override fun onError(error: Int) {
                        if (!isCompleted) {
                            isCompleted = true
                            val errorMessage = getErrorMessage(error)
                            Log.e(TAG, "Speech recognition error: $errorMessage")

                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            speechRecognizer?.destroy()

                            continuation.resume(
                                Result.failure(Exception("Speech recognition failed: $errorMessage"))
                            )
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        if (!isCompleted) {
                            isCompleted = true

                            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
                                recognitionResults.addAll(matches)
                                Log.d(TAG, "Recognition results: $matches")
                            }

                            val confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            speechRecognizer?.destroy()

                            val transcriptionResult = TranscriptionResult(
                                primaryText = recognitionResults.firstOrNull() ?: "",
                                alternativeTexts = recognitionResults.drop(1),
                                confidence = confidenceScores?.firstOrNull() ?: 0f,
                                allConfidenceScores = confidenceScores?.toList() ?: emptyList()
                            )

                            continuation.resume(Result.success(transcriptionResult))
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        // Partial results during recognition
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { partial ->
                            Log.d(TAG, "Partial results: $partial")
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Recognition events
                    }
                })

                // Prepare recognition intent
                val recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                // Start playback and recognition simultaneously
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioFile.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        // If speech recognition hasn't completed when playback ends
                        if (!isCompleted) {
                            CoroutineScope(Dispatchers.Main).launch {
                                delay(1000) // Give recognition a moment to finish
                                if (!isCompleted) {
                                    speechRecognizer?.stopListening()
                                }
                            }
                        }
                    }
                }

                // Start recognition first, then playback
                speechRecognizer.startListening(recognitionIntent)

                // Small delay to ensure recognition is ready
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    mediaPlayer?.start()
                }

                // Set up cancellation
                continuation.invokeOnCancellation {
                    try {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        speechRecognizer?.cancel()
                        speechRecognizer?.destroy()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error during cancellation cleanup", e)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error setting up transcription", e)
                mediaPlayer?.release()
                speechRecognizer?.destroy()
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * ✅ Simple speech-to-text using RecognizerIntent (alternative approach)
     * This approach requires user interaction but is more reliable
     */
    fun createRecognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your keyword clearly")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
    }

    /**
     * ✅ Live speech recognition (for real-time keyword detection)
     */
    suspend fun startLiveRecognition(
        onPartialResult: (String) -> Unit = {},
        onFinalResult: (List<String>) -> Unit = {},
        onError: (String) -> Unit = {}
    ): SpeechRecognizer? {

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available")
            return null
        }

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                onError(getErrorMessage(error))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                onFinalResult(matches)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partial?.firstOrNull()?.let { onPartialResult(it) }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        speechRecognizer.startListening(intent)
        return speechRecognizer
    }

    /**
     * ✅ Check if transcription matches keyword (fuzzy matching)
     */
    fun isKeywordMatch(
        transcription: String,
        expectedKeyword: String,
        threshold: Float = 0.8f
    ): KeywordMatchResult {

        val cleanTranscription = transcription.lowercase().trim()
        val cleanKeyword = expectedKeyword.lowercase().trim()

        // Exact match
        if (cleanTranscription == cleanKeyword) {
            return KeywordMatchResult(
                isMatch = true,
                confidence = 1.0f,
                matchType = MatchType.EXACT,
                transcribedText = transcription
            )
        }

        // Contains match
        if (cleanTranscription.contains(cleanKeyword)) {
            return KeywordMatchResult(
                isMatch = true,
                confidence = 0.9f,
                matchType = MatchType.CONTAINS,
                transcribedText = transcription
            )
        }

        // Fuzzy match using simple similarity
        val similarity = calculateSimilarity(cleanTranscription, cleanKeyword)

        return KeywordMatchResult(
            isMatch = similarity >= threshold,
            confidence = similarity,
            matchType = if (similarity >= threshold) MatchType.FUZZY else MatchType.NO_MATCH,
            transcribedText = transcription
        )
    }

    private fun calculateSimilarity(str1: String, str2: String): Float {
        val longer = if (str1.length > str2.length) str1 else str2
        val shorter = if (str1.length > str2.length) str2 else str1

        if (longer.isEmpty()) return 1.0f

        val editDistance = levenshteinDistance(longer, shorter)
        return (longer.length - editDistance).toFloat() / longer.length
    }

    private fun levenshteinDistance(str1: String, str2: String): Int {
        val matrix = Array(str1.length + 1) { IntArray(str2.length + 1) }

        for (i in 0..str1.length) {
            matrix[i][0] = i
        }
        for (j in 0..str2.length) {
            matrix[0][j] = j
        }

        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,      // deletion
                    matrix[i][j - 1] + 1,      // insertion
                    matrix[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return matrix[str1.length][str2.length]
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error ($errorCode)"
        }
    }
}

// ===============================================
// Data Classes for Speech Recognition Results
// ===============================================

data class TranscriptionResult(
    val primaryText: String,
    val alternativeTexts: List<String>,
    val confidence: Float,
    val allConfidenceScores: List<Float>
) {
    fun getBestMatch(): String = primaryText

    fun getAllResults(): List<String> = listOf(primaryText) + alternativeTexts

    fun hasHighConfidence(): Boolean = confidence >= 0.7f
}

data class KeywordMatchResult(
    val isMatch: Boolean,
    val confidence: Float,
    val matchType: MatchType,
    val transcribedText: String
)

enum class MatchType {
    EXACT,      // Perfect match
    CONTAINS,   // Keyword found within transcription
    FUZZY,      // Similar but not exact
    NO_MATCH    // No reasonable match found
}

// ===============================================
// Permission and Availability Checks
// ===============================================

object SpeechRecognitionUtils {

    fun isSpeechRecognitionAvailable(context: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun isAudioPermissionGranted(context: Context): Boolean {
        return PermissionUtils.isAudioRecordingPermissionGranted(context)
    }

    fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        )
    }
}