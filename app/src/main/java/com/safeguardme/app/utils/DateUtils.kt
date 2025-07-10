// utils/DateUtils.kt
package com.safeguardme.app.utils

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {

    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayDateTimeFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    private val fullDateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Format date for display
     */
    fun formatDisplayDate(date: Date): String {
        return displayDateFormat.format(date)
    }

    /**
     * Format time for display
     */
    fun formatDisplayTime(date: Date): String {
        return displayTimeFormat.format(date)
    }

    /**
     * Format date and time for display
     */
    fun formatDisplayDateTime(date: Date): String {
        return displayDateTimeFormat.format(date)
    }

    /**
     * Format date for database storage
     */
    fun formatStorageDateTime(date: Date): String {
        return fullDateTimeFormat.format(date)
    }

    /**
     * Get relative time string (e.g., "2 hours ago")
     */
    fun getRelativeTimeString(date: Date): String {
        val now = Date()
        val diffInMillis = now.time - date.time

        return when {
            diffInMillis < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diffInMillis < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
                "$minutes minute${if (minutes != 1L) "s" else ""} ago"
            }
            diffInMillis < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
                "$hours hour${if (hours != 1L) "s" else ""} ago"
            }
            diffInMillis < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                "$days day${if (days != 1L) "s" else ""} ago"
            }
            else -> formatDisplayDate(date)
        }
    }

    /**
     * Check if date is today
     */
    fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val dateToCheck = Calendar.getInstance().apply { time = date }

        return today.get(Calendar.YEAR) == dateToCheck.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == dateToCheck.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Check if date is within last 24 hours
     */
    fun isWithinLast24Hours(date: Date): Boolean {
        val now = Date()
        val diffInMillis = now.time - date.time
        return diffInMillis <= TimeUnit.DAYS.toMillis(1)
    }

    /**
     * Get start of day for date
     */
    fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    /**
     * Get end of day for date
     */
    fun getEndOfDay(date: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.time
    }
}
