package com.droidnova.fliptomute.ui.screens.call_state_test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.telephony.CellularCallMonitor
import com.droidnova.fliptomute.telephony.CellularCallMonitorState
import com.droidnova.fliptomute.telephony.CellularCallState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class CallStateTestViewModel(
    private val callMonitor: CellularCallMonitor,
    private val setupAccessRepository: SetupAccessRepository,
) : ViewModel() {
    private val recentTransitions = MutableStateFlow<List<CellularCallState>>(emptyList())

    val uiState: StateFlow<CallStateTestUiState> = combine(
        callMonitor.state,
        setupAccessRepository.accessState,
        recentTransitions,
    ) { monitorState, accessState, history ->
        val hasPermission = accessState.phoneStateStatus == SetupAccessStatus.GRANTED
        when (monitorState) {
            CellularCallMonitorState.Stopped -> CallStateTestUiState(
                isTelephonyAvailable = callMonitor.isTelephonyAvailable,
                hasPhonePermission = hasPermission,
                recentTransitions = history,
            )
            CellularCallMonitorState.PermissionRequired -> CallStateTestUiState(
                isTelephonyAvailable = callMonitor.isTelephonyAvailable,
                hasPhonePermission = false,
                recentTransitions = history,
            )
            CellularCallMonitorState.TelephonyUnavailable -> CallStateTestUiState(
                isTelephonyAvailable = false,
                hasPhonePermission = hasPermission,
                recentTransitions = history,
            )
            is CellularCallMonitorState.Listening -> CallStateTestUiState(
                isListening = true,
                isTelephonyAvailable = true,
                hasPhonePermission = hasPermission,
                currentCallState = monitorState.callState,
                monitoredSubscriptionCount = monitorState.monitoredSubscriptionCount,
                recentTransitions = history,
            )
            is CellularCallMonitorState.Error -> CallStateTestUiState(
                isTelephonyAvailable = callMonitor.isTelephonyAvailable,
                hasPhonePermission = hasPermission,
                recentTransitions = history,
                error = monitorState.reason,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = CallStateTestUiState(
            isTelephonyAvailable = callMonitor.isTelephonyAvailable,
            hasPhonePermission = setupAccessRepository.accessState.value.phoneStateStatus == SetupAccessStatus.GRANTED,
        ),
    )

    init {
        callMonitor.state.onEach { monitorState ->
            if (monitorState is CellularCallMonitorState.Listening) {
                recordTransition(monitorState.callState)
            }
        }.launchIn(viewModelScope)
        setupAccessRepository.accessState.onEach { accessState ->
            if (accessState.phoneStateStatus != SetupAccessStatus.GRANTED) callMonitor.stop()
        }.launchIn(viewModelScope)
    }

    fun startListening() {
        if (uiState.value.isListening) return
        recentTransitions.value = emptyList()
        setupAccessRepository.refresh()
        callMonitor.start()
    }

    fun stopListening() = callMonitor.stop()

    fun refreshAccessState() = setupAccessRepository.refresh()

    override fun onCleared() {
        callMonitor.stop()
        super.onCleared()
    }

    private fun recordTransition(callState: CellularCallState) {
        if (callState == CellularCallState.UNKNOWN) return
        recentTransitions.update { current ->
            if (current.firstOrNull() == callState) current else (listOf(callState) + current).take(MAX_TRANSITIONS)
        }
    }

    private companion object {
        const val MAX_TRANSITIONS = 5
    }
}
