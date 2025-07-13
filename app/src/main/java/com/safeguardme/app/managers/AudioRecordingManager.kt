// managers/AudioRecordingManager.kt - Background Audio Recording for Safety
package com.safeguardme.app.managers

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecordingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AudioRecordingManager"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SEGMENT_DURATION_MS = 30000L // 30 seconds per segment
        private const val MAX_RECORDING_DURATION_HOURS = 4 // Maximum 4 hours continuous
    }

    private val recordingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var currentSessionId: String? = null
    private var recordingStartTime: Long = 0
    private var currentSegmentFile: File? = null
    private val recordedSegments = mutableListOf<File>()

    // Audio recording parameters
    private val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

    /**
     * Start continuous audio recording with automatic segmentation
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startContinuousRecording(sessionId: String): File? {
        if (isRecording) {
            Log.w(TAG, "⚠️ Audio recording already in progress")
            return currentSegmentFile
        }

        Log.i(TAG, "🎤 Starting continuous audio recording for session: $sessionId")

        try {
            currentSessionId = sessionId
            recordingStartTime = System.currentTimeMillis()

            // Create recording directory
            val recordingDir = getRecordingDirectory(sessionId)
            if (!recordingDir.exists()) {
                recordingDir.mkdirs()
            }

            // Initialize AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize * 4 // Larger buffer for stability
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "❌ Failed to initialize AudioRecord")
                return null
            }

            isRecording = true

            // Start recording in background
            recordingJob = recordingScope.launch {
                performContinuousRecording()
            }

            Log.i(TAG, "✅ Continuous audio recording started")
            return currentSegmentFile

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start audio recording", e)
            cleanup()
            return null
        }
    }

    /**
     * Stop audio recording and finalize files
     */
    fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "⚠️ No active recording to stop")
            return
        }

        Log.i(TAG, "🛑 Stopping audio recording")

        isRecording = false
        recordingJob?.cancel()

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null

            // Finalize current segment
            currentSegmentFile?.let { finalizeSegment(it) }

            Log.i(TAG, "✅ Audio recording stopped successfully")
            Log.i(TAG, "📊 Total segments recorded: ${recordedSegments.size}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping audio recording", e)
        } finally {
            cleanup()
        }
    }

    /**
     * Get the most recent audio segment for transcription
     */
    fun getRecentAudioSegment(durationSeconds: Int): File? {
        return recordedSegments.lastOrNull()?.let { file ->
            if (file.exists() && file.length() > 0) {
                // For simplicity, return the last complete segment
                // In production, you might want to extract the exact duration
                file
            } else null
        }
    }

    /**
     * Finalize recording and create summary
     */
    fun finalizeRecording(): RecordingSummary? {
        if (isRecording) {
            stopRecording()
        }

        return try {
            val totalDuration = if (recordingStartTime > 0) {
                System.currentTimeMillis() - recordingStartTime
            } else 0L

            val totalSize = recordedSegments.sumOf { it.length() }

            RecordingSummary(
                sessionId = currentSessionId ?: "unknown",
                totalDurationMs = totalDuration,
                segmentCount = recordedSegments.size,
                totalSizeBytes = totalSize,
                segmentFiles = recordedSegments.toList()
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating recording summary", e)
            null
        }
    }

    /**
     * Check if currently recording
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Get current recording session ID
     */
    fun getCurrentSessionId(): String? = currentSessionId

    /**
     * Get list of recorded segments for current session
     */
    fun getRecordedSegments(): List<File> = recordedSegments.toList()

    // Private methods

    private suspend fun performContinuousRecording() {
        Log.d(TAG, "🎵 Starting continuous recording loop")

        val buffer = ByteArray(bufferSize)
        var segmentStartTime = System.currentTimeMillis()

        try {
            audioRecord?.startRecording()

            while (recordingScope.isActive && isRecording) {

                // Check if we need to start a new segment
                val currentTime = System.currentTimeMillis()
                if (currentSegmentFile == null || (currentTime - segmentStartTime) >= SEGMENT_DURATION_MS) {
                    startNewSegment()
                    segmentStartTime = currentTime
                }

                // Check maximum recording duration
                if ((currentTime - recordingStartTime) >= (MAX_RECORDING_DURATION_HOURS * 60 * 60 * 1000)) {
                    Log.w(TAG, "⚠️ Maximum recording duration reached, stopping")
                    break
                }

                // Read audio data
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (bytesRead > 0) {
                    writeAudioDataToSegment(buffer, bytesRead)
                } else {
                    // Handle read error or no data
                    delay(10) // Short delay to prevent tight loop
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in continuous recording loop", e)
        } finally {
            Log.d(TAG, "🏁 Continuous recording loop ended")
        }
    }

    private fun startNewSegment() {
        try {
            // Finalize previous segment if exists
            currentSegmentFile?.let { finalizeSegment(it) }

            // Create new segment file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val segmentFileName = "audio_segment_${timestamp}.wav"

            currentSegmentFile = File(getRecordingDirectory(currentSessionId!!), segmentFileName)

            // Create WAV header (will be updated when segment is finalized)
            writeWavHeader(currentSegmentFile!!, 0)

            Log.d(TAG, "📝 Started new audio segment: ${currentSegmentFile!!.name}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start new segment", e)
        }
    }

    private fun writeAudioDataToSegment(buffer: ByteArray, bytesRead: Int) {
        try {
            currentSegmentFile?.let { file ->
                FileOutputStream(file, true).use { fos ->
                    fos.write(buffer, 0, bytesRead)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to write audio data to segment", e)
        }
    }

    private fun finalizeSegment(segmentFile: File) {
        try {
            if (!segmentFile.exists() || segmentFile.length() <= 44) { // WAV header is 44 bytes
                Log.w(TAG, "⚠️ Segment file too small or doesn't exist: ${segmentFile.name}")
                segmentFile.delete()
                return
            }

            // Update WAV header with correct file size
            val audioDataSize = segmentFile.length() - 44 // Subtract header size
            updateWavHeader(segmentFile, audioDataSize.toInt())

            // Add to recorded segments list
            recordedSegments.add(segmentFile)

            Log.d(TAG, "✅ Finalized audio segment: ${segmentFile.name} (${audioDataSize} bytes)")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to finalize segment", e)
        }
    }

    private fun writeWavHeader(file: File, audioDataSize: Int) {
        try {
            FileOutputStream(file).use { fos ->
                val header = createWavHeader(audioDataSize)
                fos.write(header)
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to write WAV header", e)
        }
    }

    private fun updateWavHeader(file: File, audioDataSize: Int) {
        try {
            val tempFile = File(file.parent, "${file.name}.tmp")

            FileOutputStream(tempFile).use { fos ->
                // Write updated header
                val header = createWavHeader(audioDataSize)
                fos.write(header)

                // Copy audio data
                FileInputStream(file).use { fis ->
                    fis.skip(44) // Skip old header
                    fis.copyTo(fos)
                }
            }

            // Replace original file
            file.delete()
            tempFile.renameTo(file)

        } catch (e: IOException) {
            Log.e(TAG, "❌ Failed to update WAV header", e)
        }
    }

    private fun createWavHeader(audioDataSize: Int): ByteArray {
        val header = ByteArray(44)
        val totalDataLen = audioDataSize + 36
        val byteRate = SAMPLE_RATE * 2 * 1 // SampleRate * BytesPerSample * Channels

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()

        // File size
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()

        // WAVE header
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        // fmt subchunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        // Subchunk1 size (16 for PCM)
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0

        // Audio format (1 for PCM)
        header[20] = 1
        header[21] = 0

        // Number of channels
        header[22] = 1
        header[23] = 0

        // Sample rate
        header[24] = (SAMPLE_RATE and 0xff).toByte()
        header[25] = ((SAMPLE_RATE shr 8) and 0xff).toByte()
        header[26] = ((SAMPLE_RATE shr 16) and 0xff).toByte()
        header[27] = ((SAMPLE_RATE shr 24) and 0xff).toByte()

        // Byte rate
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()

        // Block align
        header[32] = 2
        header[33] = 0

        // Bits per sample
        header[34] = 16
        header[35] = 0

        // data subchunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()

        // Data size
        header[40] = (audioDataSize and 0xff).toByte()
        header[41] = ((audioDataSize shr 8) and 0xff).toByte()
        header[42] = ((audioDataSize shr 16) and 0xff).toByte()
        header[43] = ((audioDataSize shr 24) and 0xff).toByte()

        return header
    }

    private fun getRecordingDirectory(sessionId: String): File {
        return File(context.filesDir, "safety_recordings/$sessionId")
    }

    private fun cleanup() {
        currentSessionId = null
        recordingStartTime = 0
        currentSegmentFile = null
        recordedSegments.clear()
    }

    /**
     * Get available storage space for recordings
     */
    fun getAvailableStorageBytes(): Long {
        return context.filesDir.freeSpace
    }

    /**
     * Clean up old recordings to free space
     */
    fun cleanupOldRecordings(maxAgeMs: Long = 7 * 24 * 60 * 60 * 1000L) { // 7 days
        try {
            val recordingsDir = File(context.filesDir, "safety_recordings")
            if (!recordingsDir.exists()) return

            val currentTime = System.currentTimeMillis()
            var deletedCount = 0
            var freedBytes = 0L

            recordingsDir.listFiles()?.forEach { sessionDir ->
                if (sessionDir.isDirectory() && sessionDir.lastModified() < (currentTime - maxAgeMs)) {
                    val dirSize = sessionDir.walkTopDown().filter { it.isFile() }.map { it.length() }.sum()

                    if (sessionDir.deleteRecursively()) {
                        deletedCount++
                        freedBytes += dirSize
                    }
                }
            }

            if (deletedCount > 0) {
                Log.i(TAG, "🧹 Cleaned up $deletedCount old recording sessions, freed ${freedBytes / 1024 / 1024}MB")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cleaning up old recordings", e)
        }
    }
}

/**
 * Data class representing a completed recording session
 */
data class RecordingSummary(
    val sessionId: String,
    val totalDurationMs: Long,
    val segmentCount: Int,
    val totalSizeBytes: Long,
    val segmentFiles: List<File>
) {
    fun getTotalDurationSeconds(): Long = totalDurationMs / 1000
    fun getTotalSizeMB(): Double = totalSizeBytes / 1024.0 / 1024.0
}