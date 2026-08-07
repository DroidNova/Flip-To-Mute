package com.droidnova.fliptomute.ui.screens.settings

import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.deviceadmin.DeviceAdminAvailability
import com.droidnova.fliptomute.ui.screens.home.FlipAction

data class SettingsUiState(
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val detectionFeedbackEnabled: Boolean = true,
    val requireFlatSurfaceBeforeFlip: Boolean = false,
    val pocketProtectionEnabled: Boolean = true,
    val isProximitySensorAvailable: Boolean = true,
    val monitoringEnabled: Boolean = false,
    val startAfterPhoneRestart: Boolean = false,
    val flipToLockEnabled: Boolean = false,
    val deviceAdminAvailability: DeviceAdminAvailability = DeviceAdminAvailability.INACTIVE,
    val accessState: SetupAccessState = SetupAccessState(),
)

enum class SettingsMessage {
    FLIP_TO_LOCK_READY,
    SCREEN_LOCK_ACCESS_REMOVED,
}

sealed interface SettingsUiEvent {
    data object ShowDeviceAdminExplanation : SettingsUiEvent
    data class ShowMessage(val message: SettingsMessage) : SettingsUiEvent
}
