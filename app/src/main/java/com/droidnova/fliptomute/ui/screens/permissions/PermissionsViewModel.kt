package com.droidnova.fliptomute.ui.screens.permissions

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
) : ViewModel() {
    val uiState: StateFlow<PermissionsUiState> = setupAccessRepository.accessState
        .map(::PermissionsUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = PermissionsUiState(setupAccessRepository.accessState.value),
        )

    fun refreshAccessState() = setupAccessRepository.refresh()

    fun onSetupFinished(onFinished: () -> Unit = {}) {
        if (uiState.value.isSetupComplete) {
            viewModelScope.launch {
                preferencesRepository.setOnboardingCompleted(true)
                onFinished()
            }
        }
    }
}
