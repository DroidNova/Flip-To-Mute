package com.droidnova.fliptomute.ui.screens.settings

import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.data.setup.SetupAccessState

data class SettingsUiState(
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val detectionFeedbackEnabled: Boolean = true,
    val requireFlatSurfaceBeforeFlip: Boolean = false,
    val pocketProtectionEnabled: Boolean = true,
    val isProximitySensorAvailable: Boolean = true,
    val monitoringEnabled: Boolean = false,
    val startAfterPhoneRestart: Boolean = false,
    val accessState: SetupAccessState = SetupAccessState(),
)
