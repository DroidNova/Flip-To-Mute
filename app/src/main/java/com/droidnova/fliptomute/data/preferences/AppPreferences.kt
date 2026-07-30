package com.droidnova.fliptomute.data.preferences

import com.droidnova.fliptomute.ui.screens.home.FlipAction

data class AppPreferences(
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val callActionSelection: CallActionSelection = CallActionSelection(),
    val monitoringEnabled: Boolean = false,
    val detectionFeedbackEnabled: Boolean = true,
    val requireFlatSurfaceBeforeFlip: Boolean = false,
    val onboardingCompleted: Boolean = false,
)

data class CallActionSelection(
    val muteRingtone: Boolean = true,
    val vibratePhone: Boolean = false,
) {
    val isValid: Boolean get() = muteRingtone || vibratePhone
}
