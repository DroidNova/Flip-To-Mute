package com.droidnova.fliptomute.service

enum class MonitoringFailure {
    SETUP_REQUIRED,
    TELEPHONY_UNAVAILABLE,
    SENSOR_UNAVAILABLE,
    SERVICE_START_NOT_ALLOWED,
    CALL_MONITOR_FAILED,
    SOUND_CONTROL_FAILED,
    NOTIFICATION_UNAVAILABLE,
    UNKNOWN,
}

sealed interface MonitoringRuntimeState {
    data object Stopped : MonitoringRuntimeState
    data object Starting : MonitoringRuntimeState
    data object Active : MonitoringRuntimeState
    data object Stopping : MonitoringRuntimeState
    data class Error(val reason: MonitoringFailure) : MonitoringRuntimeState
}

sealed interface MonitoringCommandResult {
    data object Accepted : MonitoringCommandResult
    data class Rejected(val reason: MonitoringFailure) : MonitoringCommandResult
}
