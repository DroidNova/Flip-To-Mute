package com.droidnova.fliptomute.quicksettings

import com.droidnova.fliptomute.service.MonitoringRuntimeState

enum class QuickSettingsTileStatus { ON, OFF, STARTING, STOPPING, SETUP_REQUIRED, ERROR }

sealed interface QuickSettingsTileClickAction {
    data object StartMonitoring : QuickSettingsTileClickAction
    data object StopMonitoring : QuickSettingsTileClickAction
    data object OpenSetupAndEnable : QuickSettingsTileClickAction
    data object Ignore : QuickSettingsTileClickAction
}

class QuickSettingsTileStateResolver {
    fun resolve(state: MonitoringRuntimeState, setupComplete: Boolean): QuickSettingsTileStatus = when (state) {
        MonitoringRuntimeState.Active -> QuickSettingsTileStatus.ON
        MonitoringRuntimeState.Starting -> QuickSettingsTileStatus.STARTING
        MonitoringRuntimeState.Stopping -> QuickSettingsTileStatus.STOPPING
        MonitoringRuntimeState.Stopped -> if (setupComplete) QuickSettingsTileStatus.OFF else QuickSettingsTileStatus.SETUP_REQUIRED
        is MonitoringRuntimeState.Error -> QuickSettingsTileStatus.ERROR
    }
}

class QuickSettingsTileClickResolver {
    fun resolve(state: MonitoringRuntimeState, setupComplete: Boolean): QuickSettingsTileClickAction = when (state) {
        MonitoringRuntimeState.Active -> QuickSettingsTileClickAction.StopMonitoring
        MonitoringRuntimeState.Starting, MonitoringRuntimeState.Stopping -> QuickSettingsTileClickAction.Ignore
        MonitoringRuntimeState.Stopped, is MonitoringRuntimeState.Error -> if (setupComplete) {
            QuickSettingsTileClickAction.StartMonitoring
        } else {
            QuickSettingsTileClickAction.OpenSetupAndEnable
        }
    }
}
