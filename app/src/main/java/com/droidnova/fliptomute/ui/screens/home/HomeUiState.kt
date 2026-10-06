package com.droidnova.fliptomute.ui.screens.home

import androidx.annotation.StringRes
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.data.preferences.CallActionSelection
import com.droidnova.fliptomute.data.stats.FlipStats
import com.droidnova.fliptomute.service.MonitoringErrorRecoveryIntent
import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.ui.common.HomeHint
import com.droidnova.fliptomute.ui.components.FlipChoice

enum class FlipAction { SILENT, VIBRATE }

/** What Home shows (design spec 4.4, "Home states"). */
enum class HomeStatus { SETUP_NEEDED, CHECKING, OFF, TURNING_ON, ON, BUSY, PAUSED, ATTENTION }

/** The fix button on the attention card. */
enum class AttentionFix { TURN_BACK_ON, TRY_AGAIN, FIX_ACCESS, CHECK_SETUP, RESUME, ACKNOWLEDGE, NONE }

/** One row of the "Needs attention messages" table (design spec 4.4). */
data class Attention(
    val failure: MonitoringFailure,
    @StringRes val title: Int,
    @StringRes val body: Int?,
    val fix: AttentionFix,
    @StringRes val fixLabel: Int?,
)

/** Cards under the action selector, in the order they appear. */
sealed interface HomeCard {
    data class NeedsAttention(val attention: Attention) : HomeCard
    data object BatteryWarning : HomeCard
    data class Stats(val stats: FlipStats) : HomeCard
    data class Discovery(val hint: HomeHint) : HomeCard
}

/** One-time dialogs, at most one per visit, in this order (design spec 4.11). */
enum class HomeDialog { UPDATE_AVAILABLE, WHATS_NEW }

/** One-shot requests for the screen: things only the activity can do. */
enum class HomeEvent { OPEN_ACCESS, OPEN_CHECK_SETUP, OPEN_SETTINGS, OPEN_BATTERY_SETTINGS, REQUEST_TILE, OPEN_STORE }

data class HomeUiState(
    // 1.x fields, kept so earlier behaviour and its tests stay valid
    val isSetupComplete: Boolean = false,
    val selectedFlipAction: FlipAction = FlipAction.SILENT,
    val callActionSelection: CallActionSelection = CallActionSelection(),
    val monitoringState: MonitoringRuntimeState = MonitoringRuntimeState.Unresolved,
    val isMonitoringChecked: Boolean = false,
    val isMonitoringSwitchEnabled: Boolean = false,
    val message: MonitoringFailure? = null,
    val showPermissionsSheet: Boolean = false,
    // v2.0 Home
    val status: HomeStatus = HomeStatus.CHECKING,
    val pausedUntilEpochMs: Long? = null,
    val flipChoice: FlipChoice = FlipChoice.SILENCE,
    val cards: List<HomeCard> = emptyList(),
    val dialog: HomeDialog? = null,
    val showPauseSheet: Boolean = false,
    val event: HomeEvent? = null,
)

/** Maps the service state to what Home shows. Pure, so every row of the states table is tested. */
fun homeStatus(runtime: MonitoringRuntimeState, setupComplete: Boolean, monitoringChecked: Boolean): HomeStatus = when {
    runtime is MonitoringRuntimeState.Error -> HomeStatus.ATTENTION
    !setupComplete && !monitoringChecked -> HomeStatus.SETUP_NEEDED
    runtime is MonitoringRuntimeState.Unresolved || runtime is MonitoringRuntimeState.Recovering -> HomeStatus.CHECKING
    runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Resuming -> HomeStatus.TURNING_ON
    runtime is MonitoringRuntimeState.Pausing || runtime is MonitoringRuntimeState.Stopping -> HomeStatus.BUSY
    runtime is MonitoringRuntimeState.Active -> HomeStatus.ON
    runtime is MonitoringRuntimeState.Paused -> HomeStatus.PAUSED
    else -> HomeStatus.OFF
}

/** Silence or Vibrate from the stored selection: any selection with vibrate shows as Vibrate (plan 11). */
fun CallActionSelection.toFlipChoice(): FlipChoice = if (vibratePhone) FlipChoice.VIBRATE else FlipChoice.SILENCE

fun FlipChoice.toSelection(): CallActionSelection = when (this) {
    FlipChoice.SILENCE -> CallActionSelection(muteRingtone = true, vibratePhone = false)
    FlipChoice.VIBRATE -> CallActionSelection(muteRingtone = false, vibratePhone = true)
}

/** The "Needs attention messages" table, one row per failure. */
fun attentionFor(error: MonitoringRuntimeState.Error): Attention {
    val reason = error.reason
    if (error.recoveryIntent == MonitoringErrorRecoveryIntent.RESUME) {
        return Attention(reason, R.string.attention_paused_title, R.string.attention_paused_body, AttentionFix.RESUME, R.string.resume_monitoring)
    }
    return when (reason) {
        MonitoringFailure.SETUP_REQUIRED ->
            Attention(reason, R.string.attention_access_title, null, AttentionFix.FIX_ACCESS, R.string.attention_fix_access)
        MonitoringFailure.NOTIFICATION_UNAVAILABLE ->
            Attention(reason, R.string.attention_notifications_title, null, AttentionFix.FIX_ACCESS, R.string.access_allow_notifications)
        MonitoringFailure.SERVICE_START_NOT_ALLOWED ->
            Attention(reason, R.string.attention_stopped_title, R.string.attention_stopped_body, AttentionFix.TURN_BACK_ON, R.string.attention_turn_back_on)
        MonitoringFailure.CALL_MONITOR_FAILED ->
            Attention(reason, R.string.attention_calls_title, null, AttentionFix.TRY_AGAIN, R.string.try_again)
        MonitoringFailure.SOUND_CONTROL_FAILED ->
            Attention(reason, R.string.attention_sound_title, null, AttentionFix.FIX_ACCESS, R.string.attention_check_sound_access)
        MonitoringFailure.SENSOR_UNAVAILABLE ->
            Attention(reason, R.string.attention_sensor_title, null, AttentionFix.CHECK_SETUP, R.string.attention_check_setup)
        MonitoringFailure.TELEPHONY_UNAVAILABLE ->
            Attention(reason, R.string.attention_telephony_title, null, AttentionFix.NONE, null)
        MonitoringFailure.CLEANUP_FAILED ->
            Attention(reason, R.string.attention_cleanup_title, R.string.attention_cleanup_body, AttentionFix.ACKNOWLEDGE, R.string.got_it)
        MonitoringFailure.UNKNOWN ->
            Attention(reason, R.string.attention_unknown_title, null, AttentionFix.TRY_AGAIN, R.string.try_again)
    }
}

/** What decides which discovery card may show. */
data class DiscoveryInputs(
    val now: Long,
    val firstOnAt: Long?,
    val startAfterRestart: Boolean,
    val flipToLockEnabled: Boolean,
    val flipToLockSupported: Boolean,
    val tileAdded: Boolean,
    val tileRequestSupported: Boolean,
    val dismissed: Set<HomeHint>,
)

/** The one discovery card to show, or null (design spec 4.4, "Discovery cards"). */
fun nextDiscovery(input: DiscoveryInputs): HomeHint? {
    fun onForDays(days: Int) = input.firstOnAt != null && input.now - input.firstOnAt >= days * DAY_MS
    return when {
        HomeHint.START_AFTER_RESTART !in input.dismissed && !input.startAfterRestart -> HomeHint.START_AFTER_RESTART
        HomeHint.FLIP_TO_LOCK !in input.dismissed && onForDays(2) && !input.flipToLockEnabled && input.flipToLockSupported ->
            HomeHint.FLIP_TO_LOCK
        HomeHint.QUICK_SETTINGS_TILE !in input.dismissed && onForDays(4) && !input.tileAdded && input.tileRequestSupported ->
            HomeHint.QUICK_SETTINGS_TILE
        else -> null
    }
}

/**
 * Card order (design spec 4.4): attention first, then the battery warning, then stats once there is
 * at least one flip, then at most one card that asks for something. Discovery only while on.
 */
fun orderCards(
    status: HomeStatus,
    attention: Attention?,
    batteryWarning: Boolean,
    stats: FlipStats,
    discovery: HomeHint?,
): List<HomeCard> = buildList {
    if (attention != null) add(HomeCard.NeedsAttention(attention))
    if (batteryWarning && status == HomeStatus.ON) add(HomeCard.BatteryWarning)
    if (stats.total > 0) add(HomeCard.Stats(stats))
    if (discovery != null && status == HomeStatus.ON && attention == null && !batteryWarning) add(HomeCard.Discovery(discovery))
}


private const val DAY_MS = 24 * 60 * 60 * 1000L
