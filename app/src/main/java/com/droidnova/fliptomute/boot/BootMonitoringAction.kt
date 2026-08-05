package com.droidnova.fliptomute.boot

sealed interface BootMonitoringAction {
    data object StartMonitoring : BootMonitoringAction
    data object RestorePausedState : BootMonitoringAction
    data object StayOff : BootMonitoringAction
}

class BootMonitoringActionResolver {
    fun resolve(
        startAfterPhoneRestart: Boolean,
        monitoringEnabled: Boolean,
        monitoringPaused: Boolean,
    ): BootMonitoringAction = when {
        !startAfterPhoneRestart || !monitoringEnabled -> BootMonitoringAction.StayOff
        monitoringPaused -> BootMonitoringAction.RestorePausedState
        else -> BootMonitoringAction.StartMonitoring
    }
}
