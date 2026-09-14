package com.droidnova.fliptomute.ui.screens.home

import androidx.lifecycle.SavedStateHandle
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
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val message = MutableStateFlow<com.droidnova.fliptomute.service.MonitoringFailure?>(null)
    private val pendingAction = savedStateHandle.getStateFlow(
        PENDING_MONITORING_ACTION_KEY,
        PendingMonitoringAction.NONE.name,
    )
    private var accessCheckInProgress = false

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesRepository.preferences,
        setupAccessRepository.accessState,
        monitoringStateRepository.state,
        message,
        pendingAction,
    ) { preferences, access, runtime, currentMessage, pendingActionName ->
        val transitional = runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Pausing ||
            runtime is MonitoringRuntimeState.Resuming || runtime is MonitoringRuntimeState.Stopping ||
            runtime is MonitoringRuntimeState.Recovering || runtime is MonitoringRuntimeState.Unresolved
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
            showPermissionsSheet = pendingActionName != PendingMonitoringAction.NONE.name,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), HomeUiState())

    init {
        viewModelScope.launch {
            appRecoveryManager.reconcileMonitoringState(requestActiveReconstruction = true)
        }
        viewModelScope.launch {
            monitoringStateRepository.state.collect { runtime ->
                if (runtime is MonitoringRuntimeState.Error) message.value = runtime.reason
            }
        }
        if (currentPendingAction() != PendingMonitoringAction.NONE) refreshAccessState()
    }

    fun onMonitoringChanged(enabled: Boolean) {
        if (enabled) {
            validateAndDispatch(PendingMonitoringAction.START)
        } else {
            setPendingAction(PendingMonitoringAction.NONE)
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
        validateAndDispatch(PendingMonitoringAction.RESUME)
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
        setPendingAction(PendingMonitoringAction.NONE)
    }

    fun refreshAccessState() {
        val access = setupAccessRepository.refreshAndGet()
        val action = currentPendingAction()
        if (action != PendingMonitoringAction.NONE && access.isSetupComplete) {
            dispatchPendingAction(action)
        }
    }

    private fun validateAndDispatch(action: PendingMonitoringAction) {
        if (accessCheckInProgress) return
        accessCheckInProgress = true
        try {
            val access = setupAccessRepository.refreshAndGet()
            if (access.isSetupComplete) {
                dispatchPendingAction(action)
            } else {
                setPendingAction(action)
            }
        } finally {
            accessCheckInProgress = false
        }
    }

    private fun dispatchPendingAction(action: PendingMonitoringAction) {
        setPendingAction(PendingMonitoringAction.NONE)
        val result = when (action) {
            PendingMonitoringAction.START -> serviceController.startMonitoring()
            PendingMonitoringAction.RESUME -> serviceController.resumeMonitoring()
            PendingMonitoringAction.NONE -> return
        }
        if (result is MonitoringCommandResult.Rejected) message.value = result.reason
    }

    private fun currentPendingAction(): PendingMonitoringAction =
        runCatching { PendingMonitoringAction.valueOf(pendingAction.value) }
            .getOrDefault(PendingMonitoringAction.NONE)

    private fun setPendingAction(action: PendingMonitoringAction) {
        savedStateHandle[PENDING_MONITORING_ACTION_KEY] = action.name
    }

    private enum class PendingMonitoringAction { NONE, START, RESUME }

    private companion object {
        const val PENDING_MONITORING_ACTION_KEY = "pending_monitoring_action"
    }
}
