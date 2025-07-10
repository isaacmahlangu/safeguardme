// utils/Logger.kt - Security-aware logging
package com.safeguardme.app.utils

import android.util.Log
import com.safeguardme.app.BuildConfig

object Logger {

    private const val TAG = "SafeguardMe"
    private const val MAX_LOG_LENGTH = 4000

    fun d(message: String, tag: String = TAG) {
        if (BuildConfig.DEBUG) {
            logLongMessage(Log.DEBUG, tag, message)
        }
    }

    fun i(message: String, tag: String = TAG) {
        if (BuildConfig.DEBUG) {
            logLongMessage(Log.INFO, tag, message)
        }
    }

    fun w(message: String, tag: String = TAG) {
        if (BuildConfig.DEBUG) {
            logLongMessage(Log.WARN, tag, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (BuildConfig.DEBUG) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                logLongMessage(Log.ERROR, tag, message)
            }
        }
    }

    // Security: Never log sensitive information
    fun securityEvent(event: String, tag: String = "Security") {
        if (BuildConfig.DEBUG) {
            logLongMessage(Log.INFO, tag, "SECURITY_EVENT: $event")
        }
    }

    // Security: Log authentication events (sanitized)
    fun authEvent(event: String, success: Boolean, tag: String = "Auth") {
        if (BuildConfig.DEBUG) {
            val status = if (success) "SUCCESS" else "FAILURE"
            logLongMessage(Log.INFO, tag, "AUTH_EVENT: $event - $status")
        }
    }

    // Security: Log data access events (sanitized)
    fun dataAccess(operation: String, collection: String, tag: String = "DataAccess") {
        if (BuildConfig.DEBUG) {
            logLongMessage(Log.INFO, tag, "DATA_ACCESS: $operation on $collection")
        }
    }

    private fun logLongMessage(priority: Int, tag: String, message: String) {
        val sanitizedMessage = sanitizeLogMessage(message)

        if (sanitizedMessage.length <= MAX_LOG_LENGTH) {
            Log.println(priority, tag, sanitizedMessage)
        } else {
            // Split long messages
            val chunks = sanitizedMessage.chunked(MAX_LOG_LENGTH)
            chunks.forEachIndexed { index, chunk ->
                Log.println(priority, tag, "[$index/${chunks.size}] $chunk")
            }
        }
    }

    private fun sanitizeLogMessage(message: String): String {
        // Remove potentially sensitive information from logs
        return message
            .replace(Regex("password[\"'\\s]*[:=][\"'\\s]*[^\\s,}]+", RegexOption.IGNORE_CASE), "password=***")
            .replace(Regex("email[\"'\\s]*[:=][\"'\\s]*[^\\s,}]+", RegexOption.IGNORE_CASE), "email=***")
            .replace(Regex("phone[\"'\\s]*[:=][\"'\\s]*[^\\s,}]+", RegexOption.IGNORE_CASE), "phone=***")
            .replace(Regex("token[\"'\\s]*[:=][\"'\\s]*[^\\s,}]+", RegexOption.IGNORE_CASE), "token=***")
            .replace(Regex("key[\"'\\s]*[:=][\"'\\s]*[^\\s,}]+", RegexOption.IGNORE_CASE), "key=***")
    }
}