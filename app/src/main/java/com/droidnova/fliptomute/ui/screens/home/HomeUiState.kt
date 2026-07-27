package com.droidnova.fliptomute.ui.screens.home

import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.service.MonitoringRuntimeState

enum class FlipAction { SILENT, VIBRATE }

data class HomeUiState(
    val isSetupComplete: Boolean = false,
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val monitoringState: MonitoringRuntimeState = MonitoringRuntimeState.Stopped,
    val isMonitoringChecked: Boolean = false,
    val isMonitoringSwitchEnabled: Boolean = false,
    val message: MonitoringFailure? = null,
)
