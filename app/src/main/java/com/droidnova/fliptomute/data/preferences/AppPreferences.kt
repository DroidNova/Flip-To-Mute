package com.droidnova.fliptomute.data.preferences

import com.droidnova.fliptomute.ui.screens.home.FlipAction

data class AppPreferences(
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val callActionSelection: CallActionSelection = CallActionSelection(),
    val monitoringEnabled: Boolean = false,
    val monitoringPaused: Boolean = false,
    // On unless the user turned it off (v2.0 plan, decision D5): a restart must not silently stop Flip to Mute
    val startAfterPhoneRestart: Boolean = true,
    val detectionFeedbackEnabled: Boolean = true,
    val requireFlatSurfaceBeforeFlip: Boolean = false,
    val pocketProtectionEnabled: Boolean = true,
    val flipToLockEnabled: Boolean = false,
    val onboardingCompleted: Boolean = false,
    /** End of a timed pause (epoch ms), or null. The service stays on and ignores calls until then (M5-06). */
    val pauseUntilEpochMs: Long? = null,
    /** A quiet "Call silenced" notification after each flip (future features F31). */
    val flipNotificationEnabled: Boolean = true,
    /** "You silenced 9 calls this week", only in weeks with a flip (future features F10). */
    val weeklyRecapEnabled: Boolean = true,
)

data class CallActionSelection(
    val muteRingtone: Boolean = true,
    val vibratePhone: Boolean = false,
) {
    val isValid: Boolean get() = muteRingtone || vibratePhone
}
