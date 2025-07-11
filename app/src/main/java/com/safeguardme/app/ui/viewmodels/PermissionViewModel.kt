// ui/viewmodels/PermissionViewModel.kt
package com.safeguardme.app.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safeguardme.app.ui.screens.PermissionStep
import com.safeguardme.app.utils.AppPermission
import com.safeguardme.app.utils.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor() : ViewModel() {

    private val _currentStep = MutableStateFlow(PermissionStep.INTRODUCTION)
    val currentStep: StateFlow<PermissionStep> = _currentStep.asStateFlow()

    private val _showRationale = MutableStateFlow<Map<AppPermission, Boolean>>(emptyMap())
    val showRationale: StateFlow<Map<AppPermission, Boolean>> = _showRationale.asStateFlow()

    private lateinit var permissionManager: PermissionManager
    private lateinit var context: Context

    fun initialize(permissionManager: PermissionManager, context: Context) {
        this.permissionManager = permissionManager
        this.context = context
    }

    fun nextStep() {
        _currentStep.value = when (_currentStep.value) {
            PermissionStep.INTRODUCTION -> PermissionStep.ESSENTIAL_PERMISSIONS
            PermissionStep.ESSENTIAL_PERMISSIONS -> PermissionStep.ENHANCED_PERMISSIONS
            PermissionStep.ENHANCED_PERMISSIONS -> PermissionStep.ENHANCED_PERMISSIONS // Stay on last step
        }
    }

    fun requestPermission(permission: AppPermission, onComplete: () -> Unit) {
        viewModelScope.launch {
            // Show rationale first
            _showRationale.value = _showRationale.value.toMutableMap().apply {
                put(permission, true)
            }

            // Request permission
            permissionManager.requestPermissions(listOf(permission)) { result ->
                // Hide rationale
                _showRationale.value = _showRationale.value.toMutableMap().apply {
                    put(permission, false)
                }
                onComplete()
            }
        }
    }
}