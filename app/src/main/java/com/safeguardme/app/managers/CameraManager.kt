// managers/CameraManager.kt - Automatic Photo Capture for Safety Evidence
package com.safeguardme.app.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager as SystemCameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import androidx.core.app.ActivityCompat
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CameraManager"
        private const val IMAGE_FORMAT = ImageFormat.JPEG
        private const val MAX_IMAGES_IN_MEMORY = 3
        private const val PHOTO_QUALITY = 90 // JPEG quality 0-100
    }

    private val captureScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val systemCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as SystemCameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var isPeriodicCaptureActive = false
    private var currentSessionId: String? = null
    private val capturedPhotos = mutableListOf<File>()

    /**
     * Initialize camera for safety monitoring
     */
    suspend fun initializeCamera(): Boolean {
        if (!hasCameraPermission()) {
            Log.e(TAG, "❌ Camera permission not granted")
            return false
        }

        return try {
            Log.d(TAG, "📷 Initializing camera for safety monitoring")

            startBackgroundThread()
            setupCamera()

            Log.i(TAG, "✅ Camera initialized successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize camera", e)
            cleanup()
            false
        }
    }

    /**
     * Capture a single photo for safety evidence
     */
    suspend fun capturePhoto(sessionId: String): File? {
        if (!hasCameraPermission()) {
            Log.e(TAG, "❌ Camera permission not granted for photo capture")
            return null
        }

        return try {
            Log.d(TAG, "📷 Capturing safety photo for session: $sessionId")

            currentSessionId = sessionId

            // Initialize camera if not already done
            if (cameraDevice == null) {
                if (!initializeCamera()) {
                    return null
                }
            }

            val photoFile = createPhotoFile(sessionId)
            val success = captureImageToFile(photoFile)

            if (success) {
                capturedPhotos.add(photoFile)
                Log.i(TAG, "📷 Photo captured successfully: ${photoFile.name}")

                // Add metadata to photo
                addPhotoMetadata(photoFile)

                photoFile
            } else {
                photoFile.delete()
                Log.e(TAG, "❌ Failed to capture photo")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error capturing photo", e)
            null
        }
    }

    /**
     * Start periodic photo capture
     */
    fun startPeriodicCapture(sessionId: String, intervalMs: Long = 8000L) {
        if (isPeriodicCaptureActive) {
            Log.w(TAG, "⚠️ Periodic capture already active")
            return
        }

        Log.i(TAG, "📷 Starting periodic photo capture every ${intervalMs}ms")

        currentSessionId = sessionId
        isPeriodicCaptureActive = true

        captureScope.launch {
            try {
                // Initialize camera for periodic capture
                if (!initializeCamera()) {
                    Log.e(TAG, "❌ Failed to initialize camera for periodic capture")
                    return@launch
                }

                while (isActive && isPeriodicCaptureActive) {
                    capturePhoto(sessionId)
                    delay(intervalMs)
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in periodic photo capture", e)
            } finally {
                isPeriodicCaptureActive = false
            }
        }
    }

    /**
     * Stop periodic photo capture
     */
    fun stopPeriodicCapture() {
        if (!isPeriodicCaptureActive) {
            Log.w(TAG, "⚠️ No active periodic capture to stop")
            return
        }

        Log.i(TAG, "🛑 Stopping periodic photo capture")
        isPeriodicCaptureActive = false

        Log.i(TAG, "📊 Total photos captured: ${capturedPhotos.size}")
    }

    /**
     * Get list of captured photos for current session
     */
    fun getCapturedPhotos(): List<File> = capturedPhotos.toList()

    /**
     * Check if periodic capture is active
     */
    fun isPeriodicCaptureActive(): Boolean = isPeriodicCaptureActive

    /**
     * Cleanup camera resources
     */
    fun cleanup() {
        Log.d(TAG, "🧹 Cleaning up camera resources")

        isPeriodicCaptureActive = false

        try {
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

            stopBackgroundThread()

            Log.d(TAG, "✅ Camera cleanup completed")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during camera cleanup", e)
        }
    }

    // Private methods

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").apply {
            start()
            backgroundHandler = Handler(looper)
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    private suspend fun setupCamera(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                val cameraId = getBestCameraId()
                if (cameraId == null) {
                    Log.e(TAG, "❌ No suitable camera found")
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                }

                systemCameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        Log.d(TAG, "📷 Camera opened successfully")
                        cameraDevice = camera
                        createCaptureSession(camera) { success ->
                            continuation.resume(success)
                        }
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        Log.w(TAG, "⚠️ Camera disconnected")
                        camera.close()
                        cameraDevice = null
                        continuation.resume(false)
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.e(TAG, "❌ Camera error: $error")
                        camera.close()
                        cameraDevice = null
                        continuation.resume(false)
                    }
                }, backgroundHandler)

            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Security exception opening camera", e)
                continuation.resume(false)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception opening camera", e)
                continuation.resume(false)
            }
        }
    }

    private fun getBestCameraId(): String? {
        return try {
            val cameraIds = systemCameraManager.cameraIdList

            // Prefer back camera for safety evidence
            for (cameraId in cameraIds) {
                val characteristics = systemCameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    Log.d(TAG, "📷 Selected back camera: $cameraId")
                    return cameraId
                }
            }

            // Fallback to first available camera
            if (cameraIds.isNotEmpty()) {
                Log.d(TAG, "📷 Using fallback camera: ${cameraIds[0]}")
                return cameraIds[0]
            }

            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting camera ID", e)
            null
        }
    }

    private fun createCaptureSession(camera: CameraDevice, callback: (Boolean) -> Unit) {
        try {
            val characteristics = systemCameraManager.getCameraCharacteristics(camera.id)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            // Choose appropriate image size (balance between quality and storage)
            val size = map?.getOutputSizes(IMAGE_FORMAT)?.let { sizes ->
                sizes.filter { it.width <= 1920 && it.height <= 1080 } // Max 1080p
                    .maxByOrNull { it.width * it.height } // Largest under limit
                    ?: sizes.minByOrNull { it.width * it.height } // Fallback to smallest
            } ?: Size(1280, 720) // Default size

            Log.d(TAG, "📷 Image size: ${size.width}x${size.height}")

            // Create ImageReader
            imageReader = ImageReader.newInstance(size.width, size.height, IMAGE_FORMAT, MAX_IMAGES_IN_MEMORY)

            // Create capture session
            camera.createCaptureSession(
                listOf(imageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "📷 Capture session configured")
                        captureSession = session
                        callback(true)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "❌ Capture session configuration failed")
                        callback(false)
                    }
                },
                backgroundHandler
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating capture session", e)
            callback(false)
        }
    }

    private suspend fun captureImageToFile(photoFile: File): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                val reader = imageReader ?: run {
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                }

                val session = captureSession ?: run {
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                }

                val camera = cameraDevice ?: run {
                    continuation.resume(false)
                    return@suspendCancellableCoroutine
                }

                // Set up image available listener
                reader.setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    try {
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        FileOutputStream(photoFile).use { output ->
                            output.write(bytes)
                        }

                        Log.d(TAG, "📷 Image saved to file: ${photoFile.name}")
                        continuation.resume(true)

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error saving image", e)
                        continuation.resume(false)
                    } finally {
                        image.close()
                    }
                }, backgroundHandler)

                // Create capture request
                val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captureRequestBuilder.addTarget(reader.surface)

                // Configure capture settings for safety evidence
                captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                captureRequestBuilder.set(CaptureRequest.JPEG_QUALITY, PHOTO_QUALITY.toByte())

                // Add timestamp and orientation
                captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, getImageOrientation())

                // Capture
                session.capture(captureRequestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                    // Capture callback is handled by ImageReader listener
                }, backgroundHandler)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error capturing image", e)
                continuation.resume(false)
            }
        }
    }

    private fun createPhotoFile(sessionId: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        val photoDir = getPhotoDirectory(sessionId)

        if (!photoDir.exists()) {
            photoDir.mkdirs()
        }

        return File(photoDir, "safety_photo_${timestamp}.jpg")
    }

    private fun getPhotoDirectory(sessionId: String): File {
        return File(context.filesDir, "safety_photos/$sessionId")
    }

    private fun addPhotoMetadata(photoFile: File) {
        try {
            val exif = ExifInterface(photoFile.absolutePath)

            // Add timestamp
            val timestamp = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).format(Date())
            exif.setAttribute(ExifInterface.TAG_DATETIME, timestamp)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, timestamp)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, timestamp)

            // Add app identifier
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "SafeguardMe Safety Monitor")
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, "Safety evidence photo - Session: ${currentSessionId}")

            // Add GPS coordinates if available (requires location permission)
            // This would be added by the calling service that has access to location

            exif.saveAttributes()
            Log.d(TAG, "📝 Added metadata to photo: ${photoFile.name}")

        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Failed to add photo metadata", e)
        }
    }

    private fun getImageOrientation(): Int {
        // For simplicity, return 0 degrees. In a full implementation,
        // you would determine device orientation and adjust accordingly
        return 0
    }

    private fun hasCameraPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get photo storage summary
     */
    fun getPhotoStorageSummary(): PhotoStorageSummary {
        val totalSize = capturedPhotos.sumOf { if (it.exists()) it.length() else 0L }
        return PhotoStorageSummary(
            photoCount = capturedPhotos.size,
            totalSizeBytes = totalSize,
            sessionId = currentSessionId ?: "unknown"
        )
    }

    /**
     * Compress old photos to save storage
     */
    fun compressOldPhotos(maxAgeMs: Long = 24 * 60 * 60 * 1000L) { // 24 hours
        captureScope.launch(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                var compressedCount = 0

                capturedPhotos.removeAll { photoFile ->
                    if (!photoFile.exists()) {
                        true // Remove from list if file doesn't exist
                    } else if ((currentTime - photoFile.lastModified()) > maxAgeMs) {
                        // For simplicity, just delete old photos
                        // In production, you might want to actually compress them
                        val deleted = photoFile.delete()
                        if (deleted) compressedCount++
                        deleted
                    } else {
                        false
                    }
                }

                if (compressedCount > 0) {
                    Log.i(TAG, "🗜️ Compressed/deleted $compressedCount old photos")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error compressing old photos", e)
            }
        }
    }
}

/**
 * Data class for photo storage summary
 */
data class PhotoStorageSummary(
    val photoCount: Int,
    val totalSizeBytes: Long,
    val sessionId: String
) {
    fun getTotalSizeMB(): Double = totalSizeBytes / 1024.0 / 1024.0
}