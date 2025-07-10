// data/models/FirebaseResponse.kt
package com.safeguardme.app.data.models

sealed class FirebaseResponse<out T> {
    data class Success<T>(val data: T) : FirebaseResponse<T>()
    data class Error(val exception: Exception) : FirebaseResponse<Nothing>()
    object Loading : FirebaseResponse<Nothing>()
}

// Extension functions for Result handling
fun <T> Result<T>.toFirebaseResponse(): FirebaseResponse<T> = when {
    isSuccess -> FirebaseResponse.Success(getOrThrow())
    else -> FirebaseResponse.Error((exceptionOrNull() ?: Exception("Unknown error")) as Exception)
}