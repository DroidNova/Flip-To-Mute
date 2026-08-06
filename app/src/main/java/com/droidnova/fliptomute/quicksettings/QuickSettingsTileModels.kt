package com.droidnova.fliptomute.quicksettings

import com.droidnova.fliptomute.service.MonitoringRuntimeState

enum class QuickSettingsTileStatus { ON, OFF, STARTING, PAUSING, PAUSED, RESUMING, STOPPING, SETUP_REQUIRED, ERROR }

sealed interface QuickSettingsTileClickAction {
    data object StartMonitoring : QuickSettingsTileClickAction
    data object PauseMonitoring : QuickSettingsTileClickAction
    data object ResumeMonitoring : QuickSettingsTileClickAction
    data object OpenSetupAndEnable : QuickSettingsTileClickAction
    data object OpenSetupAndResume : QuickSettingsTileClickAction
    data object Ignore : QuickSettingsTileClickAction
}

class QuickSettingsTileStateResolver {
    fun resolve(state: MonitoringRuntimeState, setupComplete: Boolean): QuickSettingsTileStatus = when (state) {
        MonitoringRuntimeState.Active -> QuickSettingsTileStatus.ON
        MonitoringRuntimeState.Starting -> QuickSettingsTileStatus.STARTING
        MonitoringRuntimeState.Pausing -> QuickSettingsTileStatus.PAUSING
        MonitoringRuntimeState.Paused -> QuickSettingsTileStatus.PAUSED
        MonitoringRuntimeState.Resuming -> QuickSettingsTileStatus.RESUMING
        MonitoringRuntimeState.Stopping -> QuickSettingsTileStatus.STOPPING
        MonitoringRuntimeState.Stopped -> if (setupComplete) QuickSettingsTileStatus.OFF else QuickSettingsTileStatus.SETUP_REQUIRED
        is MonitoringRuntimeState.Error -> QuickSettingsTileStatus.ERROR
    }
}

class QuickSettingsTileClickResolver {
    fun resolve(state: MonitoringRuntimeState, setupComplete: Boolean): QuickSettingsTileClickAction = when (state) {
        MonitoringRuntimeState.Active -> QuickSettingsTileClickAction.PauseMonitoring
        MonitoringRuntimeState.Paused -> if (setupComplete) {
            QuickSettingsTileClickAction.ResumeMonitoring
        } else {
            QuickSettingsTileClickAction.OpenSetupAndResume
        }
        MonitoringRuntimeState.Starting, MonitoringRuntimeState.Pausing,
        MonitoringRuntimeState.Resuming, MonitoringRuntimeState.Stopping,
        -> QuickSettingsTileClickAction.Ignore
        MonitoringRuntimeState.Stopped, is MonitoringRuntimeState.Error -> if (setupComplete) {
            QuickSettingsTileClickAction.StartMonitoring
        } else {
            QuickSettingsTileClickAction.OpenSetupAndEnable
        }
    }
}
