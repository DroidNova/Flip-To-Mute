package com.droidnova.fliptomute.ui.screens.permissions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PermissionsViewModel(
    private val setupAccessRepository: SetupAccessRepository,
    private val preferencesRepository: AppPreferencesRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    val uiState: StateFlow<PermissionsUiState> = setupAccessRepository.accessState
        .map(::PermissionsUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = PermissionsUiState(setupAccessRepository.accessState.value),
        )

    fun refreshAccessState() = setupAccessRepository.refresh()

    fun permissionAction(
        permission: RuntimeSetupPermission,
        granted: Boolean,
        shouldShowRationale: Boolean,
    ): RuntimePermissionAction = resolveRuntimePermissionAction(
        granted = granted,
        shouldShowRationale = shouldShowRationale,
        requestedBefore = savedStateHandle[permission.savedStateKey] ?: false,
    )

    fun markPermissionRequested(permission: RuntimeSetupPermission) {
        savedStateHandle[permission.savedStateKey] = true
    }

    fun onSetupFinished(onFinished: () -> Unit = {}) {
        if (uiState.value.isSetupComplete) {
            viewModelScope.launch {
                preferencesRepository.setOnboardingCompleted(true)
                onFinished()
            }
        }
    }
}

enum class RuntimeSetupPermission(internal val savedStateKey: String) {
    PHONE("phone_permission_requested"),
    NOTIFICATIONS("notification_permission_requested"),
}

enum class RuntimePermissionAction { REFRESH, SHOW_EXPLANATION, OPEN_SETTINGS }

internal fun resolveRuntimePermissionAction(
    granted: Boolean,
    shouldShowRationale: Boolean,
    requestedBefore: Boolean,
): RuntimePermissionAction = when {
    granted -> RuntimePermissionAction.REFRESH
    requestedBefore && !shouldShowRationale -> RuntimePermissionAction.OPEN_SETTINGS
    else -> RuntimePermissionAction.SHOW_EXPLANATION
}
