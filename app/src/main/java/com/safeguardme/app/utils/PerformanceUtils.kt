// utils/PerformanceUtils.kt
package com.safeguardme.app.utils

import androidx.compose.runtime.*
import com.safeguardme.app.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object PerformanceUtils {

    // Debounced state for search/filtering
    @Composable
    fun <T> rememberDebouncedState(
        value: T,
        delayMillis: Long = 300L
    ): State<T> {
        val debouncedState = remember { mutableStateOf(value) }

        LaunchedEffect(value) {
            kotlinx.coroutines.delay(delayMillis)
            debouncedState.value = value
        }

        return debouncedState
    }

    // Optimized list state for large datasets
    @Composable
    fun <T> rememberOptimizedListState(
        items: List<T>,
        key: (T) -> Any = { it.hashCode() }
    ): State<List<T>> {
        return remember(items) {
            derivedStateOf {
                items.distinctBy { key(it) }
            }
        }
    }

    // Memory-efficient image loading state
    @Composable
    fun rememberImageLoadingState(): MutableState<Boolean> {
        return remember { mutableStateOf(false) }
    }

    // Throttled actions to prevent spam
    class ActionThrottler(private val delayMs: Long = 1000L) {
        private var lastActionTime = 0L

        fun execute(action: () -> Unit): Boolean {
            val currentTime = System.currentTimeMillis()
            return if (currentTime - lastActionTime >= delayMs) {
                lastActionTime = currentTime
                action()
                true
            } else {
                false
            }
        }
    }

    // Efficient Flow transformations
    fun <T> Flow<List<T>>.distinctListUntilChanged(): Flow<List<T>> {
        return this.distinctUntilChanged { old, new ->
            old.size == new.size && old.zip(new).all { (a, b) -> a == b }
        }
    }

    fun <T, R> Flow<T>.mapDistinct(transform: (T) -> R): Flow<R> {
        return this.map(transform).distinctUntilChanged()
    }

    // Memory management utilities
    fun forceGarbageCollection() {
        if (BuildConfig.DEBUG) {
            System.gc()
            Logger.d("Forced garbage collection")
        }
    }

    // Performance monitoring
    inline fun <T> measureTime(tag: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val endTime = System.currentTimeMillis()

        if (BuildConfig.DEBUG) {
            Logger.d("$tag took ${endTime - startTime}ms")
        }

        return result
    }
}
