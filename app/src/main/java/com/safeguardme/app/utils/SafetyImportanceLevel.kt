// utils/SafetyImportanceLevel.kt
package com.safeguardme.app.utils

enum class SafetyImportanceLevel {
    CRITICAL,  // App cannot function safely without this
    HIGH,      // Major safety features compromised without this
    MEDIUM,    // Some safety features unavailable without this
    LOW        // Minor convenience features affected
}