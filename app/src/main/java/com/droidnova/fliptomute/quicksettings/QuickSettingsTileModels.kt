package com.droidnova.fliptomute.quicksettings

import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.service.MonitoringErrorRecoveryIntent

enum class QuickSettingsTileStatus { ON, OFF, RECOVERING, STARTING, PAUSING, PAUSED, RESUMING, STOPPING, SETUP_REQUIRED, ERROR }

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
        MonitoringRuntimeState.Recovering -> QuickSettingsTileStatus.RECOVERING
        MonitoringRuntimeState.Starting -> QuickSettingsTileStatus.STARTING
        MonitoringRuntimeState.Pausing -> QuickSettingsTileStatus.PAUSING
        MonitoringRuntimeState.Paused -> QuickSettingsTileStatus.PAUSED
        MonitoringRuntimeState.Resuming -> QuickSettingsTileStatus.RESUMING
        MonitoringRuntimeState.Stopping -> QuickSettingsTileStatus.STOPPING
        MonitoringRuntimeState.Stopped -> if (setupComplete) QuickSettingsTileStatus.OFF else QuickSettingsTileStatus.SETUP_REQUIRED
        is MonitoringRuntimeState.Error -> if (state.recoveryIntent == MonitoringErrorRecoveryIntent.RESUME) {
            QuickSettingsTileStatus.PAUSED
        } else QuickSettingsTileStatus.ERROR
    }
}

class QuickSettingsTileClickResolver {
    fun resolve(state: MonitoringRuntimeState, setupComplete: Boolean): QuickSettingsTileClickAction = when (state) {
        MonitoringRuntimeState.Active -> QuickSettingsTileClickAction.PauseMonitoring
        MonitoringRuntimeState.Recovering -> QuickSettingsTileClickAction.Ignore
        MonitoringRuntimeState.Paused -> if (setupComplete) {
            QuickSettingsTileClickAction.ResumeMonitoring
        } else {
            QuickSettingsTileClickAction.OpenSetupAndResume
        }
        MonitoringRuntimeState.Starting, MonitoringRuntimeState.Pausing,
        MonitoringRuntimeState.Resuming, MonitoringRuntimeState.Stopping,
        -> QuickSettingsTileClickAction.Ignore
        is MonitoringRuntimeState.Error -> if (state.recoveryIntent == MonitoringErrorRecoveryIntent.RESUME) {
            if (setupComplete) QuickSettingsTileClickAction.ResumeMonitoring
            else QuickSettingsTileClickAction.OpenSetupAndResume
        } else if (setupComplete) {
            QuickSettingsTileClickAction.StartMonitoring
        } else {
            QuickSettingsTileClickAction.OpenSetupAndEnable
        }
        MonitoringRuntimeState.Stopped -> if (setupComplete) {
            QuickSettingsTileClickAction.StartMonitoring
        } else {
            QuickSettingsTileClickAction.OpenSetupAndEnable
        }
    }
}
