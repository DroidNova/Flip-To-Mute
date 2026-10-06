package com.droidnova.fliptomute.ui.screens.settings

import com.droidnova.fliptomute.data.preferences.FlipSchedule
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.deviceadmin.DeviceAdminAvailability
import com.droidnova.fliptomute.sensor.FlipSensitivity
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.utils.AppTheme
import com.droidnova.fliptomute.utils.ThemeMode
import com.droidnova.fliptomute.utils.about_utils.OtherAppItem

data class SettingsUiState(
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val detectionFeedbackEnabled: Boolean = true,
    val requireFlatSurfaceBeforeFlip: Boolean = false,
    val pocketProtectionEnabled: Boolean = true,
    val isProximitySensorAvailable: Boolean = true,
    val monitoringEnabled: Boolean = false,
    val startAfterPhoneRestart: Boolean = true,
    val flipToLockEnabled: Boolean = false,
    val flipNotificationEnabled: Boolean = true,
    val weeklyRecapEnabled: Boolean = true,
    val callbackReminderEnabled: Boolean = true,
    val schedule: FlipSchedule = FlipSchedule(),
    val sensitivity: FlipSensitivity = FlipSensitivity.NORMAL,
    val ringAgainWhenFaceUp: Boolean = false,
    val flipToPauseMediaEnabled: Boolean = false,
    val deviceAdminAvailability: DeviceAdminAvailability = DeviceAdminAvailability.INACTIVE,
    val accessState: SetupAccessState = SetupAccessState(),
    /** True when Android may stop Flip to Mute to save battery; null when the phone cannot tell. */
    val batteryRestricted: Boolean? = null,
    val tileAdded: Boolean = false,
    val appTheme: AppTheme = AppTheme.BLUE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Earned themes that are not open yet (future features F36). */
    val lockedThemes: Set<AppTheme> = emptySet(),
    val totalFlips: Int = 0,
    /** Set by the activity from the consent status (M7-02). */
    val privacyOptionsRequired: Boolean = false,
    /** Two other DroidNova apps for the foot of the list; picked by the route each time Settings opens. */
    val otherApps: List<OtherAppItem> = emptyList(),
    /** Flip to Mute Premium (future features F19): set by the route from the activity's billing state. */
    /** False while the purchase is switched off (`AppConstants.PREMIUM_ENABLED`): nothing mentions Premium. */
    val premiumAvailable: Boolean = false,
    val premiumActive: Boolean = false,
)

enum class SettingsMessage {
    FLIP_TO_LOCK_READY,
    SCREEN_LOCK_ACCESS_REMOVED,
}

sealed interface SettingsUiEvent {
    data object ShowDeviceAdminExplanation : SettingsUiEvent
    data class ShowMessage(val message: SettingsMessage) : SettingsUiEvent
    data class ShowThemeLocked(val theme: AppTheme) : SettingsUiEvent
}
