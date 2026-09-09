package com.droidnova.fliptomute.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.service.MonitoringCommandResult
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.service.MonitoringServiceController
import com.droidnova.fliptomute.service.MonitoringStateRepository
import com.droidnova.fliptomute.service.AppRecoveryManager
import com.droidnova.fliptomute.service.MonitoringErrorRecoveryIntent
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
    private val appRecoveryManager: AppRecoveryManager,
) : ViewModel() {
    private val message = MutableStateFlow<com.droidnova.fliptomute.service.MonitoringFailure?>(null)
    private val permissionsSheet = MutableStateFlow(false)
    private var pendingAction = PendingMonitoringAction.NONE

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesRepository.preferences,
        setupAccessRepository.accessState,
        monitoringStateRepository.state,
        message,
        permissionsSheet,
    ) { preferences, access, runtime, currentMessage, showPermissions ->
        val transitional = runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Pausing ||
            runtime is MonitoringRuntimeState.Resuming || runtime is MonitoringRuntimeState.Stopping ||
            runtime is MonitoringRuntimeState.Recovering
        val pausedError = runtime is MonitoringRuntimeState.Error &&
            runtime.recoveryIntent == MonitoringErrorRecoveryIntent.RESUME
        HomeUiState(
            isSetupComplete = access.isSetupComplete,
            selectedFlipAction = preferences.selectedFlipAction,
            callActionSelection = preferences.callActionSelection,
            monitoringState = runtime,
            isMonitoringChecked = preferences.monitoringEnabled || pausedError || runtime is MonitoringRuntimeState.Active ||
                runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Resuming,
            isMonitoringSwitchEnabled = !transitional,
            message = currentMessage,
            showPermissionsSheet = showPermissions,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), HomeUiState())

    init {
        viewModelScope.launch { appRecoveryManager.recoverOnAppLaunch() }
        viewModelScope.launch {
            monitoringStateRepository.state.collect { runtime ->
                if (runtime is MonitoringRuntimeState.Error) message.value = runtime.reason
            }
        }
    }

    fun onMonitoringChanged(enabled: Boolean) {
        if (enabled) {
            if (!setupAccessRepository.accessState.value.isSetupComplete) {
                pendingAction = PendingMonitoringAction.START
                permissionsSheet.value = true
                return
            }
            pendingAction = PendingMonitoringAction.NONE
            when (val result = serviceController.startMonitoring()) {
                MonitoringCommandResult.Accepted -> Unit
                is MonitoringCommandResult.Rejected -> message.value = result.reason
            }
        } else {
            serviceController.stopMonitoring()
        }
    }

    fun onPauseMonitoring() {
        if (monitoringStateRepository.state.value is MonitoringRuntimeState.Active) {
            when (val result = serviceController.pauseMonitoring()) {
                MonitoringCommandResult.Accepted -> Unit
                is MonitoringCommandResult.Rejected -> message.value = result.reason
            }
        }
    }

    fun onResumeMonitoring() {
        if (!setupAccessRepository.accessState.value.isSetupComplete) {
            pendingAction = PendingMonitoringAction.RESUME
            permissionsSheet.value = true
            return
        }
        pendingAction = PendingMonitoringAction.NONE
        when (val result = serviceController.resumeMonitoring()) {
            MonitoringCommandResult.Accepted -> Unit
            is MonitoringCommandResult.Rejected -> message.value = result.reason
        }
    }

    fun onFlipActionSelected(action: FlipAction) {
        viewModelScope.launch { preferencesRepository.setFlipAction(action) }
    }

    fun onMuteRingtoneChanged(selected: Boolean) = updateSelection(mute = selected)
    fun onVibratePhoneChanged(selected: Boolean) = updateSelection(vibrate = selected)

    private fun updateSelection(mute: Boolean? = null, vibrate: Boolean? = null) {
        val current = uiState.value.callActionSelection
        val updated = current.copy(
            muteRingtone = mute ?: current.muteRingtone,
            vibratePhone = vibrate ?: current.vibratePhone,
        )
        if (updated.isValid) viewModelScope.launch { preferencesRepository.setCallActionSelection(updated) }
    }

    fun onMessageShown() { message.value = null }
    fun dismissPermissionsSheet() {
        pendingAction = PendingMonitoringAction.NONE
        permissionsSheet.value = false
    }
    fun refreshAccessState() {
        val access = setupAccessRepository.refreshAndGet()
        val action = pendingAction
        if (action != PendingMonitoringAction.NONE && access.isSetupComplete) {
            permissionsSheet.value = false
            pendingAction = PendingMonitoringAction.NONE
            when (action) {
                PendingMonitoringAction.START -> onMonitoringChanged(true)
                PendingMonitoringAction.RESUME -> onResumeMonitoring()
                PendingMonitoringAction.NONE -> Unit
            }
        }
    }

    private enum class PendingMonitoringAction { NONE, START, RESUME }
}
