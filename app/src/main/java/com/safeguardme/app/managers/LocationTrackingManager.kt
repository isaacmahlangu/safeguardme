// managers/LocationTrackingManager.kt - GPS Location Tracking for Safety
package com.safeguardme.app.managers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationTrackingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LocationTrackingManager"
        private const val MIN_TIME_BETWEEN_UPDATES = 5000L // 5 seconds
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES = 10f // 10 meters
        private const val LOCATION_TIMEOUT_MS = 10000L // 10 seconds
    }

    private val trackingScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var isTracking = false
    private var locationCallback: LocationCallback? = null
    private var locationListener: LocationListener? = null
    private var onLocationUpdateCallback: ((Location) -> Unit)? = null
    private var lastKnownLocation: Location? = null
    private val locationHistory = mutableListOf<LocationData>()

    /**
     * Start location tracking with specified interval
     */
    @SuppressLint("MissingPermission")
    fun startTracking(
        intervalMs: Long = MIN_TIME_BETWEEN_UPDATES,
        onLocationUpdate: (Location) -> Unit
    ) {
        if (isTracking) {
            Log.w(TAG, "⚠️ Location tracking already active")
            return
        }

        if (!hasLocationPermissions()) {
            Log.e(TAG, "❌ Location permissions not granted")
            return
        }

        Log.i(TAG, "📍 Starting location tracking with ${intervalMs}ms interval")

        onLocationUpdateCallback = onLocationUpdate
        isTracking = true

        try {
            // Use Google Play Services if available, fallback to Android LocationManager
            if (isGooglePlayServicesAvailable()) {
                startFusedLocationTracking(intervalMs)
            } else {
                startStandardLocationTracking(intervalMs)
            }

            Log.i(TAG, "✅ Location tracking started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start location tracking", e)
            isTracking = false
        }
    }

    /**
     * Stop location tracking
     */
    fun stopTracking() {
        if (!isTracking) {
            Log.w(TAG, "⚠️ No active location tracking to stop")
            return
        }

        Log.i(TAG, "🛑 Stopping location tracking")

        try {
            // Stop fused location updates
            locationCallback?.let { callback ->
                fusedLocationClient.removeLocationUpdates(callback)
                locationCallback = null
            }

            // Stop standard location updates
            locationListener?.let { listener ->
                locationManager.removeUpdates(listener)
                locationListener = null
            }

            isTracking = false
            onLocationUpdateCallback = null

            Log.i(TAG, "✅ Location tracking stopped")
            Log.i(TAG, "📊 Total locations collected: ${locationHistory.size}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping location tracking", e)
        }
    }

    /**
     * Get current location (one-time request)
     */
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermissions()) {
            Log.e(TAG, "❌ Location permissions not granted for current location")
            return null
        }

        return try {
            // Try fused location provider first
            if (isGooglePlayServicesAvailable()) {
                getCurrentLocationFromFused()
            } else {
                getCurrentLocationFromManager()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get current location", e)
            lastKnownLocation // Return last known location as fallback
        }
    }

    /**
     * Get location history for current session
     */
    fun getLocationHistory(): List<LocationData> = locationHistory.toList()

    /**
     * Get last known location
     */
    fun getLastKnownLocation(): Location? = lastKnownLocation

    /**
     * Check if currently tracking location
     */
    fun isTracking(): Boolean = isTracking

    /**
     * Get location accuracy status
     */
    fun getLocationAccuracyStatus(): LocationAccuracyStatus {
        val lastLocation = lastKnownLocation
        return when {
            lastLocation == null -> LocationAccuracyStatus.NO_LOCATION
            lastLocation.accuracy <= 10f -> LocationAccuracyStatus.HIGH_ACCURACY
            lastLocation.accuracy <= 50f -> LocationAccuracyStatus.MEDIUM_ACCURACY
            else -> LocationAccuracyStatus.LOW_ACCURACY
        }
    }

    /**
     * Clear location history (for privacy)
     */
    fun clearLocationHistory() {
        locationHistory.clear()
        Log.d(TAG, "🧹 Location history cleared")
    }

    // Private methods

    @SuppressLint("MissingPermission")
    private fun startFusedLocationTracking(intervalMs: Long) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_CHANGE_FOR_UPDATES)
            .setMaxUpdateDelayMillis(intervalMs * 2) // Max delay is 2x the interval
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location, LocationSource.FUSED)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )

        Log.d(TAG, "📍 Started fused location tracking")
    }

    @SuppressLint("MissingPermission")
    private fun startStandardLocationTracking(intervalMs: Long) {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                handleLocationUpdate(location, LocationSource.GPS)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                Log.d(TAG, "📍 Location status changed: $provider -> $status")
            }

            override fun onProviderEnabled(provider: String) {
                Log.d(TAG, "📍 Location provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                Log.w(TAG, "⚠️ Location provider disabled: $provider")
            }
        }

        // Try to use GPS provider first, then network
        when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    intervalMs,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    locationListener!!
                )
                Log.d(TAG, "📍 Started GPS location tracking")
            }
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    intervalMs,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    locationListener!!
                )
                Log.d(TAG, "📍 Started network location tracking")
            }
            else -> {
                Log.e(TAG, "❌ No location providers available")
                throw Exception("No location providers available")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocationFromFused(): Location? {
        return suspendCancellableCoroutine { continuation ->
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
                .setMaxUpdateDelayMillis(LOCATION_TIMEOUT_MS)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = locationResult.lastLocation
                    continuation.resume(location)
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )

                // Set up timeout
                continuation.invokeOnCancellation {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                }

                // Timeout after specified duration
                trackingScope.launch {
                    delay(LOCATION_TIMEOUT_MS)
                    if (continuation.isActive) {
                        fusedLocationClient.removeLocationUpdates(locationCallback)
                        continuation.resume(null)
                    }
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocationFromManager(): Location? {
        return try {
            // Try to get last known location first
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            // Return the most recent location
            when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting location from manager", e)
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun handleLocationUpdate(location: Location, source: LocationSource) {
        try {
            // Validate location quality
            if (!isValidLocation(location)) {
                Log.w(TAG, "⚠️ Invalid location received, skipping")
                return
            }

            lastKnownLocation = location

            // Create location data entry
            val locationData = LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = if (location.hasAltitude()) location.altitude else null,
                accuracy = location.accuracy,
                bearing = if (location.hasBearing()) location.bearing else null,
                speed = if (location.hasSpeed()) location.speed else null,
                timestamp = System.currentTimeMillis(),
                source = source,
                provider = location.provider ?: "unknown"
            )

            // Add to history
            locationHistory.add(locationData)

            // Limit history size to prevent memory issues
            if (locationHistory.size > 1000) {
                locationHistory.removeFirst()
            }

            // Notify callback
            onLocationUpdateCallback?.invoke(location)

            Log.d(TAG, "📍 Location updated: ${location.latitude}, ${location.longitude} " +
                    "(±${location.accuracy}m) from ${source.name}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling location update", e)
        }
    }

    private fun isValidLocation(location: Location): Boolean {
        return location.latitude != 0.0 &&
                location.longitude != 0.0 &&
                location.accuracy > 0 &&
                location.accuracy < 500 // Reject locations with accuracy worse than 500m
    }

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        return try {
            val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val result = googleApiAvailability.isGooglePlayServicesAvailable(context)
            result == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get formatted location string for notifications
     */
    fun getFormattedLocation(location: Location): String {
        return "📍 Location: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}\n" +
                "Accuracy: ±${location.accuracy.toInt()}m\n" +
                "Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
    }

    /**
     * Get Google Maps URL for location
     */
    fun getGoogleMapsUrl(location: Location): String {
        return "https://maps.google.com/?q=${location.latitude},${location.longitude}"
    }

    /**
     * Calculate distance between two locations
     */
    fun calculateDistance(location1: Location, location2: Location): Float {
        return location1.distanceTo(location2)
    }

    /**
     * Get location data as JSON for storage
     */
    fun getLocationDataAsJson(location: Location): String {
        return """
            {
                "latitude": ${location.latitude},
                "longitude": ${location.longitude},
                "altitude": ${if (location.hasAltitude()) location.altitude else null},
                "accuracy": ${location.accuracy},
                "bearing": ${if (location.hasBearing()) location.bearing else null},
                "speed": ${if (location.hasSpeed()) location.speed else null},
                "timestamp": ${System.currentTimeMillis()},
                "provider": "${location.provider ?: "unknown"}",
                "maps_url": "${getGoogleMapsUrl(location)}"
            }
        """.trimIndent()
    }
}

/**
 * Data class for storing location information
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float,
    val bearing: Float?,
    val speed: Float?,
    val timestamp: Long,
    val source: LocationSource,
    val provider: String
) {
    fun toLocation(): Location {
        return Location(provider).apply {
            latitude = this@LocationData.latitude
            longitude = this@LocationData.longitude
            altitude?.let { altitude = it }
            accuracy = this@LocationData.accuracy
            bearing?.let { bearing = it }
            speed?.let { speed = it }
            time = timestamp
        }
    }
}

/**
 * Enum for location source
 */
enum class LocationSource {
    GPS,
    NETWORK,
    FUSED,
    PASSIVE
}

/**
 * Enum for location accuracy status
 */
enum class LocationAccuracyStatus {
    NO_LOCATION,
    HIGH_ACCURACY,    // ≤ 10m
    MEDIUM_ACCURACY,  // ≤ 50m
    LOW_ACCURACY      // > 50m
}