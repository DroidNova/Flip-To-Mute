package com.droidnova.fliptomute.ui.screens.home

import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.data.setup.SetupAccessType

enum class MonitoringStatus { DISABLED, SETUP_REQUIRED, ACTIVE }

enum class FlipAction { SILENT, VIBRATE }

data class SetupItemUiModel(
    val type: SetupAccessType,
    val status: SetupAccessStatus,
)

data class HomeUiState(
    val monitoringStatus: MonitoringStatus = MonitoringStatus.SETUP_REQUIRED,
    val isMonitoringEnabled: Boolean = false,
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val setupItems: List<SetupItemUiModel> = SetupAccessState().toSetupItems(),
)

fun SetupAccessState.toSetupItems() = SetupAccessType.entries.map { type ->
    SetupItemUiModel(type = type, status = statusFor(type))
}
