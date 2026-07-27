package com.droidnova.fliptomute.ui.screens.settings

import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.data.setup.SetupAccessState

data class SettingsUiState(
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val detectionFeedbackEnabled: Boolean = true,
    val monitoringEnabled: Boolean = false,
    val accessState: SetupAccessState = SetupAccessState(),
)
