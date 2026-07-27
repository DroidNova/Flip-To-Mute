package com.droidnova.fliptomute.telephony

enum class CellularCallState { UNKNOWN, IDLE, RINGING, ACTIVE }

enum class CellularCallMonitorError {
    PERMISSION_REVOKED,
    REGISTRATION_FAILED,
    TELEPHONY_SERVICE_UNAVAILABLE,
    UNKNOWN,
}

sealed interface CellularCallMonitorState {
    data object Stopped : CellularCallMonitorState
    data object PermissionRequired : CellularCallMonitorState
    data object TelephonyUnavailable : CellularCallMonitorState
    data class Listening(
        val callState: CellularCallState,
        val monitoredSubscriptionCount: Int,
    ) : CellularCallMonitorState
    data class Error(val reason: CellularCallMonitorError) : CellularCallMonitorState
}
