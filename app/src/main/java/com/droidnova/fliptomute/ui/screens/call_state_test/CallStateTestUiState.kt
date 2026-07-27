package com.droidnova.fliptomute.ui.screens.call_state_test

import com.droidnova.fliptomute.telephony.CellularCallMonitorError
import com.droidnova.fliptomute.telephony.CellularCallState

data class CallStateTestUiState(
    val isListening: Boolean = false,
    val isTelephonyAvailable: Boolean = true,
    val hasPhonePermission: Boolean = false,
    val currentCallState: CellularCallState = CellularCallState.UNKNOWN,
    val monitoredSubscriptionCount: Int = 0,
    val recentTransitions: List<CellularCallState> = emptyList(),
    val error: CellularCallMonitorError? = null,
)
