package com.droidnova.fliptomute.data.preferences

import com.droidnova.fliptomute.ui.screens.home.FlipAction

data class AppPreferences(
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val monitoringEnabled: Boolean = false,
    val detectionFeedbackEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
)
