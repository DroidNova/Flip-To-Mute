package com.droidnova.fliptomute.ui.screens.home

import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.data.preferences.CallActionSelection

enum class FlipAction { SILENT, VIBRATE }

data class HomeUiState(
    val isSetupComplete: Boolean = false,
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val callActionSelection: CallActionSelection = CallActionSelection(),
    val monitoringState: MonitoringRuntimeState = MonitoringRuntimeState.Stopped,
    val isMonitoringChecked: Boolean = false,
    val isMonitoringSwitchEnabled: Boolean = false,
    val message: MonitoringFailure? = null,
    val showPermissionsSheet: Boolean = false,
)
