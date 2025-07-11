// services/GestureDetectionService.kt
package com.safeguardme.app.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.sqrt

@AndroidEntryPoint
class GestureDetectionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeThreshold = 15.0f
    private var lastShakeTime = 0L

    // Volume button tracking
    private var volumeButtonPressCount = 0
    private var lastVolumeButtonPress = 0L
    private val volumeButtonResetTime = 2000L // Reset after 2 seconds

    // Power button tracking
    private var powerButtonPressCount = 0
    private var lastPowerButtonPress = 0L
    private val powerButtonResetTime = 3000L // Reset after 3 seconds

    override fun onCreate() {
        super.onCreate()
        setupSensors()
    }

    private fun setupSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                detectShake(sensorEvent.values)
            }
        }
    }

    private fun detectShake(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]

        val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH

        if (acceleration > shakeThreshold) {
            val currentTime = System.currentTimeMillis()

            // Prevent multiple shake detections in quick succession
            if (currentTime - lastShakeTime > 1000) {
                lastShakeTime = currentTime
                onShakeDetected(acceleration)
            }
        }
    }

    private fun onShakeDetected(intensity: Float) {
        // Notify ViewModel about shake detection
        // This would require a different architecture or event bus
        // For now, we'll use a simplified approach
        sendBroadcast(Intent("com.safeguardme.SHAKE_DETECTED").apply {
            putExtra("intensity", intensity)
        })
    }

    // Call this from Activity's onKeyDown
    fun onVolumeButtonPressed() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastVolumeButtonPress > volumeButtonResetTime) {
            volumeButtonPressCount = 1
        } else {
            volumeButtonPressCount++
        }

        lastVolumeButtonPress = currentTime

        if (volumeButtonPressCount >= 3) {
            onVolumeTriplePress()
            volumeButtonPressCount = 0
        }
    }

    fun onPowerButtonPressed() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastPowerButtonPress > powerButtonResetTime) {
            powerButtonPressCount = 1
        } else {
            powerButtonPressCount++
        }

        lastPowerButtonPress = currentTime

        if (powerButtonPressCount >= 5) {
            onPowerButtonFivePress()
            powerButtonPressCount = 0
        }
    }

    private fun onVolumeTriplePress() {
        sendBroadcast(Intent("com.safeguardme.VOLUME_TRIPLE_PRESS"))
    }

    private fun onPowerButtonFivePress() {
        sendBroadcast(Intent("com.safeguardme.POWER_FIVE_PRESS"))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}