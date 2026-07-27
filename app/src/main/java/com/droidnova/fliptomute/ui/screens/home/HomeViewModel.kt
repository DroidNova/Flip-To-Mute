package com.droidnova.fliptomute.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val preferencesRepository: AppPreferencesRepository,
    private val setupAccessRepository: SetupAccessRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        preferencesRepository.preferences,
        setupAccessRepository.accessState,
    ) { preferences, accessState ->
        HomeUiState(
            monitoringStatus = if (accessState.isSetupComplete) {
                MonitoringStatus.DISABLED
            } else {
                MonitoringStatus.SETUP_REQUIRED
            },
            isMonitoringEnabled = false,
            selectedFlipAction = preferences.selectedFlipAction,
            setupItems = accessState.toSetupItems(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    fun onFlipActionSelected(action: FlipAction) {
        viewModelScope.launch { preferencesRepository.setFlipAction(action) }
    }

    fun onMonitoringEnabledChanged(enabled: Boolean) {
        if (!enabled) {
            viewModelScope.launch { preferencesRepository.setMonitoringEnabled(false) }
        }
    }

    fun refreshAccessState() = setupAccessRepository.refresh()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
