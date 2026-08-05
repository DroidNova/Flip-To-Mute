package com.droidnova.fliptomute.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: AppPreferencesRepository,
    private val setupAccessRepository: SetupAccessRepository,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        setupAccessRepository.accessState,
    ) { preferences, accessState ->
        SettingsUiState(
            selectedFlipAction = preferences.selectedFlipAction,
            detectionFeedbackEnabled = preferences.detectionFeedbackEnabled,
            requireFlatSurfaceBeforeFlip = preferences.requireFlatSurfaceBeforeFlip,
            monitoringEnabled = preferences.monitoringEnabled,
            startAfterPhoneRestart = preferences.startAfterPhoneRestart,
            accessState = accessState,
        )
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(),
        )

    fun onFlipActionSelected(action: FlipAction) {
        viewModelScope.launch { preferencesRepository.setFlipAction(action) }
    }

    fun onDetectionFeedbackChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDetectionFeedbackEnabled(enabled) }
    }

    fun onRequireFlatSurfaceBeforeFlipChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setRequireFlatSurfaceBeforeFlip(enabled) }
    }

    fun onStartAfterPhoneRestartChanged(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setStartAfterPhoneRestart(enabled) }
    }

    fun refreshAccessState() = setupAccessRepository.refresh()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
