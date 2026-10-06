package com.droidnova.fliptomute.boot

import com.droidnova.fliptomute.service.MonitoringStartSource

/** What woke the app up without the user opening it. */
enum class AutoStartTrigger(val startSource: MonitoringStartSource) {
    /** BOOT_COMPLETED. Follows the "Start after phone restart" setting. */
    BOOT(MonitoringStartSource.BOOT),
    /** MY_PACKAGE_REPLACED. An update is not a restart, so the user's choice is simply restored. */
    PACKAGE_REPLACED(MonitoringStartSource.PACKAGE_REPLACED),
}

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
    ): BootMonitoringAction = resolve(AutoStartTrigger.BOOT, startAfterPhoneRestart, monitoringEnabled, monitoringPaused)

    fun resolve(
        trigger: AutoStartTrigger,
        startAfterPhoneRestart: Boolean,
        monitoringEnabled: Boolean,
        monitoringPaused: Boolean,
    ): BootMonitoringAction = when {
        !monitoringEnabled -> BootMonitoringAction.StayOff
        trigger == AutoStartTrigger.BOOT && !startAfterPhoneRestart -> BootMonitoringAction.StayOff
        monitoringPaused -> BootMonitoringAction.RestorePausedState
        else -> BootMonitoringAction.StartMonitoring
    }
}
