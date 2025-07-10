// data/repositories/SafetyRepository.kt
package com.safeguardme.app.data.repositories

import com.safeguardme.app.data.models.SafetyStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface SafetyRepository {
    fun observeSafetyStatus(): Flow<SafetyStatus>
    suspend fun updateSafetyStatus(status: SafetyStatus): Result<Unit>
    suspend fun refreshSafetyStatus(): Result<Unit>
    suspend fun getCurrentSafetyStatus(): Result<SafetyStatus>
}

@Singleton
class SafetyRepositoryImpl @Inject constructor(
    private val safetyDataSource: SafetyDataSource,
    private val userRepository: UserRepository
) : SafetyRepository {

    override fun observeSafetyStatus(): Flow<SafetyStatus> {
        return userRepository.observeCurrentUserProfile()
            .map { user -> user?.safetyStatus ?: SafetyStatus.DISABLED }
    }

    override suspend fun updateSafetyStatus(status: SafetyStatus): Result<Unit> {
        return try {
            safetyDataSource.updateSafetyStatus(status)
            userRepository.updateUserSafetyStatus(status)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshSafetyStatus(): Result<Unit> {
        return try {
            val currentStatus = safetyDataSource.getCurrentSafetyStatus()
            userRepository.updateUserSafetyStatus(currentStatus)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentSafetyStatus(): Result<SafetyStatus> {
        return try {
            val status = safetyDataSource.getCurrentSafetyStatus()
            Result.success(status)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// Data source interface
interface SafetyDataSource {
    suspend fun updateSafetyStatus(status: SafetyStatus)
    suspend fun getCurrentSafetyStatus(): SafetyStatus
}