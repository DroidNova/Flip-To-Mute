package com.droidnova.fliptomute.ui.screens.home

enum class MonitoringStatus { DISABLED, SETUP_REQUIRED, ACTIVE }

enum class FlipAction { SILENT, VIBRATE }

enum class SetupItemType { PHONE, SOUND_CONTROL, NOTIFICATIONS }

data class SetupItemUiModel(
    val type: SetupItemType,
    val isAllowed: Boolean,
)

data class HomeUiState(
    val monitoringStatus: MonitoringStatus = MonitoringStatus.SETUP_REQUIRED,
    val isMonitoringEnabled: Boolean = false,
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val setupItems: List<SetupItemUiModel> = defaultSetupItems,
)

val defaultSetupItems = SetupItemType.entries.map { type ->
    SetupItemUiModel(type = type, isAllowed = false)
}
