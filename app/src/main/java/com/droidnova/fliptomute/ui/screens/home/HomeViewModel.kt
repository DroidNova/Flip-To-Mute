package com.droidnova.fliptomute.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.service.MonitoringCommandResult
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.service.MonitoringServiceController
import com.droidnova.fliptomute.service.MonitoringStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val preferencesRepository: AppPreferencesRepository,
    private val setupAccessRepository: SetupAccessRepository,
    private val monitoringStateRepository: MonitoringStateRepository,
    private val serviceController: MonitoringServiceController,
) : ViewModel() {
    private val message = MutableStateFlow<com.droidnova.fliptomute.service.MonitoringFailure?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesRepository.preferences,
        setupAccessRepository.accessState,
        monitoringStateRepository.state,
        message,
    ) { preferences, access, runtime, currentMessage ->
        val transitional = runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Stopping
        HomeUiState(
            isSetupComplete = access.isSetupComplete,
            selectedFlipAction = preferences.selectedFlipAction,
            monitoringState = runtime,
            isMonitoringChecked = runtime is MonitoringRuntimeState.Active || runtime is MonitoringRuntimeState.Starting,
            isMonitoringSwitchEnabled = access.isSetupComplete && !transitional,
            message = currentMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), HomeUiState())

    init {
        viewModelScope.launch {
            monitoringStateRepository.state.collect { runtime ->
                if (runtime is MonitoringRuntimeState.Error) message.value = runtime.reason
            }
        }
        viewModelScope.launch {
            preferencesRepository.preferences.collect { preferences ->
                if (preferences.monitoringEnabled && monitoringStateRepository.state.value is MonitoringRuntimeState.Stopped) {
                    preferencesRepository.setMonitoringEnabled(false)
                }
            }
        }
    }

    fun onMonitoringChanged(enabled: Boolean) {
        if (enabled) {
            if (!setupAccessRepository.accessState.value.isSetupComplete) return
            when (val result = serviceController.startMonitoring()) {
                MonitoringCommandResult.Accepted -> Unit
                is MonitoringCommandResult.Rejected -> message.value = result.reason
            }
        } else {
            serviceController.stopMonitoring()
        }
    }

    fun onFlipActionSelected(action: FlipAction) {
        viewModelScope.launch { preferencesRepository.setFlipAction(action) }
    }

    fun onMessageShown() { message.value = null }
    fun refreshAccessState() = setupAccessRepository.refresh()
}
