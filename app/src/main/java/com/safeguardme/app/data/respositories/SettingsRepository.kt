// data/repositories/SettingsRepository.kt
package com.safeguardme.app.data.repositories

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SettingsRepository"

        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val ENABLE_SOUNDS_KEY = booleanPreferencesKey("enable_sounds")
        private val ALLOW_OFFLINE_MODE_KEY = booleanPreferencesKey("allow_offline_mode")
        private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
        private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        private val AUTO_BACKUP_ENABLED_KEY = booleanPreferencesKey("auto_backup_enabled")
        private val EMERGENCY_CONTACTS_ONLY_KEY = booleanPreferencesKey("emergency_contacts_only")
        private val LAST_BACKUP_TIME_KEY = longPreferencesKey("last_backup_time")

        private val VOICE_DETECTION_ENABLED_KEY = booleanPreferencesKey("voice_detection_enabled")
        private val VOICE_DETECTION_KEYWORD_KEY = stringPreferencesKey("voice_detection_keyword")
        private val VOICE_DETECTION_SENSITIVITY_KEY = stringPreferencesKey("voice_detection_sensitivity")
    }

    // Dark mode setting
    val isDarkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[DARK_MODE_KEY] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    // Sounds setting
    val isSoundsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ENABLE_SOUNDS_KEY] ?: true }

    suspend fun setSoundsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_SOUNDS_KEY] = enabled
        }
    }

    suspend fun setVoiceDetectionEnabled(enabled: Boolean) {
        Log.d(TAG, "🎤 Setting voice detection enabled: $enabled")
        context.dataStore.edit { preferences ->
            preferences[VOICE_DETECTION_ENABLED_KEY] = enabled
        }
    }

    suspend fun setVoiceDetectionKeyword(keyword: String?) {
        Log.d(TAG, "🎯 Setting voice detection keyword: $keyword")
        context.dataStore.edit { preferences ->
            if (keyword != null) {
                preferences[VOICE_DETECTION_KEYWORD_KEY] = keyword
            } else {
                preferences.remove(VOICE_DETECTION_KEYWORD_KEY)
            }
        }
    }

    suspend fun setVoiceDetectionSensitivity(sensitivity: String) {
        Log.d(TAG, "🎚️ Setting voice detection sensitivity: $sensitivity")
        context.dataStore.edit { preferences ->
            preferences[VOICE_DETECTION_SENSITIVITY_KEY] = sensitivity
        }
    }

    val voiceDetectionEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOICE_DETECTION_ENABLED_KEY] ?: false
    }

    val voiceDetectionKeyword: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[VOICE_DETECTION_KEYWORD_KEY]
    }

    /*suspend fun isVoiceDetectionConfigured(): Boolean {
        return try {
            val settings = appSettings.map { it }.kotlinx.coroutines.flow.first()
            settings.voiceDetectionKeyword != null && settings.voiceDetectionKeyword.isNotBlank()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking voice detection configuration", e)
            false
        }
    }*/

    // Offline mode setting
    val isOfflineModeAllowed: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[ALLOW_OFFLINE_MODE_KEY] ?: true }

    suspend fun setOfflineModeAllowed(allowed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ALLOW_OFFLINE_MODE_KEY] = allowed
        }
    }

    // Onboarding completion
    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[HAS_SEEN_ONBOARDING_KEY] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] = completed
        }
    }

    // Biometric authentication
    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[BIOMETRIC_ENABLED_KEY] ?: false }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED_KEY] = enabled
        }
    }

    // Emergency contacts only mode
    val isEmergencyContactsOnly: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[EMERGENCY_CONTACTS_ONLY_KEY] ?: false }

    suspend fun setEmergencyContactsOnly(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EMERGENCY_CONTACTS_ONLY_KEY] = enabled
        }
    }

    // Get all settings as a combined flow
    data class AppSettings(
        val darkMode: Boolean = false,
        val soundsEnabled: Boolean = true,
        val offlineModeAllowed: Boolean = true,
        val biometricEnabled: Boolean = false,
        val emergencyContactsOnly: Boolean = false,
        val voiceDetectionEnabled: Boolean = false,
        val voiceDetectionKeyword: String? = null,
        val voiceDetectionSensitivity: String = "medium" // low, medium, high
    )

    val appSettings: Flow<AppSettings> = context.dataStore.data
        .map { preferences ->
            AppSettings(
                darkMode = preferences[DARK_MODE_KEY] ?: false,
                soundsEnabled = preferences[ENABLE_SOUNDS_KEY] ?: true,
                offlineModeAllowed = preferences[ALLOW_OFFLINE_MODE_KEY] ?: true,
                biometricEnabled = preferences[BIOMETRIC_ENABLED_KEY] ?: false,
                emergencyContactsOnly = preferences[EMERGENCY_CONTACTS_ONLY_KEY] ?: false,
                voiceDetectionEnabled = preferences[VOICE_DETECTION_ENABLED_KEY] ?: false,
                voiceDetectionKeyword = preferences[VOICE_DETECTION_KEYWORD_KEY],
                voiceDetectionSensitivity = preferences[VOICE_DETECTION_SENSITIVITY_KEY] ?: "medium"
            )
        }


}