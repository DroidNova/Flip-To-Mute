package com.droidnova.fliptomute.ui.screens.settings

import android.text.format.DateFormat
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PhoneCallback
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.billing.LocalPremiumController
import com.droidnova.fliptomute.deviceadmin.DeviceAdminAvailability
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileAddRequester
import com.droidnova.fliptomute.sensor.FlipSensitivity
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileAddResult
import com.droidnova.fliptomute.ui.components.ActionSelector
import com.droidnova.fliptomute.ui.components.ChoicePill
import com.droidnova.fliptomute.ui.components.FlipChoice
import com.droidnova.fliptomute.ui.components.NovaTopBar
import com.droidnova.fliptomute.ui.components.SettingsGroup
import com.droidnova.fliptomute.ui.components.SettingsRow
import com.droidnova.fliptomute.ui.components.SwitchRow
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.ui.theme.BlueLightColorScheme
import com.droidnova.fliptomute.ui.theme.ForestLightColorScheme
import com.droidnova.fliptomute.ui.theme.MidnightLightColorScheme
import com.droidnova.fliptomute.ui.theme.RoseLightColorScheme
import com.droidnova.fliptomute.ui.theme.SunsetLightColorScheme
import com.droidnova.fliptomute.ui.theme.TealLightColorScheme
import com.droidnova.fliptomute.ui.theme.labelRes
import com.droidnova.fliptomute.ui.util.RefreshOnResume
import com.droidnova.fliptomute.utils.AppTheme
import com.droidnova.fliptomute.utils.ThemeMode
import com.droidnova.fliptomute.ui.screens.about.OtherAppsSection
import com.droidnova.fliptomute.utils.about_utils.AppConstants
import com.droidnova.fliptomute.utils.about_utils.IntentUtil
import com.droidnova.fliptomute.utils.about_utils.randomOtherApps
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Locale

/** Everything Settings can ask for (design spec 4.6), as Secret Calculator's SettingsActions. */
interface SettingsActions {
    fun back()
    fun selectFlipChoice(choice: FlipChoice)
    fun setFlatOnly(enabled: Boolean)
    fun setPocketProtection(enabled: Boolean)
    fun setFeedback(enabled: Boolean)
    fun setFlipToLock(enabled: Boolean)
    fun removeScreenLockAccess()
    fun setStartAfterRestart(enabled: Boolean)
    fun setFlipNotification(enabled: Boolean)
    fun setWeeklyRecap(enabled: Boolean)
    fun setCallbackReminder(enabled: Boolean)
    fun selectSensitivity(sensitivity: FlipSensitivity)
    fun setRingAgain(enabled: Boolean)
    fun setFlipToPause(enabled: Boolean)
    fun setScheduleEnabled(enabled: Boolean)
    fun toggleScheduleDay(day: DayOfWeek)
    fun setScheduleStart(minute: Int)
    fun setScheduleEnd(minute: Int)
    fun openKeepRunning()
    fun addTile()
    fun selectThemeMode(mode: ThemeMode)
    fun selectTheme(theme: AppTheme)
    fun openCheckSetup()
    fun reportProblem()
    fun openAbout()
    fun openPrivacyPolicy()
    fun openPrivacyOptions()
    fun removeAds()
    fun openOtherApp(packageName: String)
    fun openMoreApps()
}

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenKeepRunning: () -> Unit,
    onOpenCheckSetup: () -> Unit,
    onReportProblem: () -> Unit,
    onAbout: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    privacyOptionsRequired: Boolean = false,
    onOpenPrivacyOptions: () -> Unit = {},
    /** A rewarded ad can be shown to open an earned theme for a week (future features F34). */
    rewardedThemeAvailable: Boolean = false,
    onWatchAdForTheme: (onRewarded: () -> Unit, onUnavailable: () -> Unit) -> Unit = { _, _ -> },
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    RefreshOnResume {
        viewModel.refreshAccessState()
        viewModel.refreshDeviceAdminState()
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val premium = LocalPremiumController.current
    val premiumUi by premium.premiumUi.collectAsStateWithLifecycle()
    val activity = LocalActivity.current ?: return
    val otherApps = remember { randomOtherApps(activity.packageName) }
    val addRequester = remember(activity) { QuickSettingsTileAddRequester(activity) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showManualInstructions by remember { mutableStateOf(false) }
    var showAdminExplanation by remember { mutableStateOf(false) }
    var showRemoveAdminConfirmation by remember { mutableStateOf(false) }
    var lockedTheme by remember { mutableStateOf<AppTheme?>(null) }
    // The switch turns on only after Android grants screen-lock access (architecture A11)
    val adminLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onDeviceAdminActivationResult()
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsUiEvent.ShowDeviceAdminExplanation -> showAdminExplanation = true
                is SettingsUiEvent.ShowThemeLocked -> lockedTheme = event.theme
                is SettingsUiEvent.ShowMessage -> snackbar.showSnackbar(
                    activity.getString(
                        when (event.message) {
                            SettingsMessage.FLIP_TO_LOCK_READY -> R.string.flip_to_lock_ready
                            SettingsMessage.SCREEN_LOCK_ACCESS_REMOVED -> R.string.screen_lock_access_removed
                        },
                    ),
                )
            }
        }
    }
    SettingsScreen(
        state = state.copy(
            privacyOptionsRequired = privacyOptionsRequired,
            otherApps = otherApps,
            removeAdsAvailable = AppConstants.REMOVE_ADS_ENABLED,
            adsRemoved = premiumUi.isPremium,
            removeAdsPrice = premiumUi.priceLabel,
        ),
        snackbarHostState = snackbar,
        actions = object : SettingsActions {
            override fun back() = onBack()
            override fun selectFlipChoice(choice: FlipChoice) = viewModel.onFlipActionSelected(
                if (choice == FlipChoice.VIBRATE) FlipAction.VIBRATE else FlipAction.SILENT,
            )
            override fun setFlatOnly(enabled: Boolean) = viewModel.onRequireFlatSurfaceBeforeFlipChanged(enabled)
            override fun setPocketProtection(enabled: Boolean) = viewModel.onPocketProtectionChanged(enabled)
            override fun setFeedback(enabled: Boolean) = viewModel.onDetectionFeedbackChanged(enabled)
            override fun setFlipToLock(enabled: Boolean) = viewModel.onFlipToLockChanged(enabled)
            override fun removeScreenLockAccess() { showRemoveAdminConfirmation = true }
            override fun setStartAfterRestart(enabled: Boolean) = viewModel.onStartAfterPhoneRestartChanged(enabled)
            override fun setFlipNotification(enabled: Boolean) = viewModel.onFlipNotificationChanged(enabled)
            override fun setWeeklyRecap(enabled: Boolean) = viewModel.onWeeklyRecapChanged(enabled)
            override fun setCallbackReminder(enabled: Boolean) = viewModel.onCallbackReminderChanged(enabled)
            override fun selectSensitivity(sensitivity: FlipSensitivity) = viewModel.onSensitivitySelected(sensitivity)
            override fun setRingAgain(enabled: Boolean) = viewModel.onRingAgainWhenFaceUpChanged(enabled)
            override fun setFlipToPause(enabled: Boolean) = viewModel.onFlipToPauseMediaChanged(enabled)
            override fun setScheduleEnabled(enabled: Boolean) = viewModel.onScheduleEnabledChanged(enabled)
            override fun toggleScheduleDay(day: DayOfWeek) = viewModel.onScheduleDayToggled(day)
            override fun setScheduleStart(minute: Int) = viewModel.onScheduleStartChanged(minute)
            override fun setScheduleEnd(minute: Int) = viewModel.onScheduleEndChanged(minute)
            override fun openKeepRunning() = onOpenKeepRunning()
            override fun addTile() {
                addRequester.request { result ->
                    if (result == QuickSettingsTileAddResult.ManualInstructionsRequired) {
                        showManualInstructions = true
                    } else {
                        val message = activity.getString(result.messageResource())
                        scope.launch { snackbar.showSnackbar(message) }
                    }
                }
            }
            override fun selectThemeMode(mode: ThemeMode) = viewModel.selectThemeMode(mode)
            override fun selectTheme(theme: AppTheme) = viewModel.selectTheme(theme)
            override fun openCheckSetup() = onOpenCheckSetup()
            override fun reportProblem() = onReportProblem()
            override fun openAbout() = onAbout()
            override fun openPrivacyPolicy() = onOpenPrivacyPolicy()
            override fun openPrivacyOptions() = onOpenPrivacyOptions()
            override fun removeAds() = premium.launchPurchase()
            override fun openOtherApp(packageName: String) = IntentUtil.openPlayStore(activity, packageName)
            override fun openMoreApps() = IntentUtil.openDeveloperPlayConsole(activity)
        },
    )
    if (showManualInstructions) {
        AlertDialog(
            onDismissRequest = { showManualInstructions = false },
            title = { Text(stringResource(R.string.add_quick_settings_tile_title)) },
            text = { Text(stringResource(R.string.add_quick_settings_tile_instructions)) },
            confirmButton = { TextButton(onClick = { showManualInstructions = false }) { Text(stringResource(R.string.got_it)) } },
        )
    }
    // Explain before asking for special access, as Secret Calculator does (design spec 4.6)
    if (showAdminExplanation) {
        AlertDialog(
            onDismissRequest = { showAdminExplanation = false },
            title = { Text(stringResource(R.string.allow_screen_locking_title)) },
            text = { Text(stringResource(R.string.allow_screen_locking_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showAdminExplanation = false
                    adminLauncher.launch(viewModel.createDeviceAdminActivationIntent())
                }) { Text(stringResource(R.string.continue_action)) }
            },
            dismissButton = { TextButton(onClick = { showAdminExplanation = false }) { Text(stringResource(R.string.cancel_action)) } },
        )
    }
    // An earned theme that is not open yet: say how to earn it, and offer the ad where there is one
    lockedTheme?.let { theme ->
        AlertDialog(
            onDismissRequest = { lockedTheme = null },
            title = { Text(stringResource(R.string.theme_locked_title, stringResource(theme.labelRes()))) },
            text = {
                Text(
                    stringResource(
                        if (rewardedThemeAvailable) R.string.theme_locked_body_with_ad else R.string.theme_locked_body,
                        theme.unlockAt, state.totalFlips,
                    ),
                )
            },
            confirmButton = {
                if (rewardedThemeAvailable) {
                    TextButton(onClick = {
                        lockedTheme = null
                        scope.launch { snackbar.showSnackbar(activity.getString(R.string.theme_ad_loading)) }
                        onWatchAdForTheme(
                            { viewModel.onThemeUnlockedByAd(theme) },
                            { scope.launch { snackbar.showSnackbar(activity.getString(R.string.theme_ad_unavailable)) } },
                        )
                    }) { Text(stringResource(R.string.theme_watch_ad)) }
                } else {
                    TextButton(onClick = { lockedTheme = null }) { Text(stringResource(R.string.got_it)) }
                }
            },
            dismissButton = if (rewardedThemeAvailable) {
                { TextButton(onClick = { lockedTheme = null }) { Text(stringResource(R.string.discovery_not_now)) } }
            } else {
                null
            },
        )
    }
    if (showRemoveAdminConfirmation) {
        AlertDialog(
            onDismissRequest = { showRemoveAdminConfirmation = false },
            title = { Text(stringResource(R.string.remove_screen_lock_access_title)) },
            text = { Text(stringResource(R.string.remove_screen_lock_access_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveAdminConfirmation = false
                    viewModel.onRemoveDeviceAdminConfirmed()
                }) { Text(stringResource(R.string.remove_action)) }
            },
            dismissButton = { TextButton(onClick = { showRemoveAdminConfirmation = false }) { Text(stringResource(R.string.cancel_action)) } },
        )
    }
}

@Composable
fun SettingsScreen(state: SettingsUiState, actions: SettingsActions, snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }) {
    Scaffold(
        topBar = { NovaTopBar(stringResource(R.string.settings_title), onBack = actions::back) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            // Centred on tablets and in landscape (design spec 8)
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "flip") { FlipBehaviourGroup(state, actions) }
            item(key = "schedule") { ScheduleGroup(state, actions) }
            item(key = "gestures") { GesturesGroup(state, actions) }
            item(key = "keep") { KeepRunningGroup(state, actions) }
            item(key = "notifications") { NotificationsGroup(state, actions) }
            item(key = "appearance") { AppearanceGroup(state, actions) }
            item(key = "help") { HelpGroup(state, actions) }
            // Other DroidNova apps at the foot of the list, as in All File Reader
            if (state.otherApps.isNotEmpty()) {
                item(key = "other-apps") {
                    OtherAppsSection(
                        state.otherApps,
                        onOpenApp = actions::openOtherApp,
                        onMoreApps = actions::openMoreApps,
                        modifier = Modifier.widthIn(max = 560.dp).padding(start = 4.dp, end = 4.dp, top = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FlipBehaviourGroup(state: SettingsUiState, actions: SettingsActions) {
    SettingsGroup(stringResource(R.string.settings_group_flip), Modifier.widthIn(max = 560.dp)) {
        Text(
            stringResource(R.string.home_when_i_flip),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp),
        )
        ActionSelector(
            selected = if (state.selectedFlipAction == FlipAction.VIBRATE) FlipChoice.VIBRATE else FlipChoice.SILENCE,
            onSelect = actions::selectFlipChoice,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        SwitchRow(
            Icons.Filled.Layers,
            stringResource(R.string.only_when_lying_flat),
            state.requireFlatSurfaceBeforeFlip,
            actions::setFlatOnly,
            summary = stringResource(R.string.only_when_lying_flat_description),
        )
        SwitchRow(
            Icons.Filled.Sensors,
            stringResource(R.string.pocket_protection),
            state.pocketProtectionEnabled && state.isProximitySensorAvailable,
            actions::setPocketProtection,
            summary = stringResource(
                if (state.isProximitySensorAvailable) R.string.pocket_protection_description else R.string.pocket_protection_unavailable,
            ),
            enabled = state.isProximitySensorAvailable,
        )
        SwitchRow(
            Icons.Filled.Vibration,
            stringResource(R.string.detection_feedback),
            state.detectionFeedbackEnabled,
            actions::setFeedback,
            summary = stringResource(R.string.detection_feedback_description),
        )
        SwitchRow(
            Icons.Filled.Replay,
            stringResource(R.string.ring_again_setting),
            state.ringAgainWhenFaceUp,
            actions::setRingAgain,
            summary = stringResource(R.string.ring_again_setting_description),
        )
        Text(
            stringResource(R.string.sensitivity_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        )
        FlowRow(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlipSensitivity.entries.forEach { sensitivity ->
                ChoicePill(
                    label = stringResource(sensitivity.labelRes()),
                    selected = state.sensitivity == sensitivity,
                    onClick = { actions.selectSensitivity(sensitivity) },
                )
            }
        }
        Text(
            stringResource(state.sensitivity.descriptionRes()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp),
        )
    }
}

/** Active hours and days (future features F9). Outside them calls ring normally. */
@Composable
private fun ScheduleGroup(state: SettingsUiState, actions: SettingsActions) {
    val schedule = state.schedule
    // Which end of the period is being edited: true for the start, false for the end
    var editingStart by remember { mutableStateOf<Boolean?>(null) }
    SettingsGroup(stringResource(R.string.settings_group_schedule), Modifier.widthIn(max = 560.dp)) {
        SwitchRow(
            Icons.Filled.Schedule,
            stringResource(R.string.schedule_setting),
            schedule.enabled,
            actions::setScheduleEnabled,
            summary = stringResource(R.string.schedule_setting_description),
        )
        if (schedule.enabled) {
            FlowRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DayOfWeek.entries.forEach { day ->
                    ChoicePill(
                        label = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        selected = day in schedule.days,
                        onClick = { actions.toggleScheduleDay(day) },
                        showCheck = false,
                    )
                }
            }
            SettingsRow(
                Icons.Filled.LightMode,
                stringResource(R.string.schedule_from),
                onClick = { editingStart = true },
                summary = formatMinuteOfDay(schedule.startMinute),
            )
            SettingsRow(
                Icons.Filled.DarkMode,
                stringResource(R.string.schedule_until),
                onClick = { editingStart = false },
                summary = formatMinuteOfDay(schedule.endMinute),
            )
        }
    }
    editingStart?.let { start ->
        ScheduleTimeDialog(
            title = stringResource(if (start) R.string.schedule_from else R.string.schedule_until),
            initialMinute = if (start) schedule.startMinute else schedule.endMinute,
            onConfirm = { minute ->
                if (start) actions.setScheduleStart(minute) else actions.setScheduleEnd(minute)
                editingStart = null
            },
            onDismiss = { editingStart = null },
        )
    }
}

/** Typed time entry: it fits the narrowest phones, where the clock dial would not. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleTimeDialog(title: String, initialMinute: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    val picker = rememberTimePickerState(
        initialHour = initialMinute / 60,
        initialMinute = initialMinute % 60,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimeInput(picker) },
        confirmButton = { TextButton(onClick = { onConfirm(picker.hour * 60 + picker.minute) }) { Text(stringResource(android.R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) } },
    )
}

/** "9:00 AM" or "09:00", following the phone setting. */
@Composable
private fun formatMinuteOfDay(minute: Int): String {
    val time = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minute / 60)
        set(Calendar.MINUTE, minute % 60)
    }.time
    return DateFormat.getTimeFormat(LocalContext.current).format(time)
}

@Composable
private fun GesturesGroup(state: SettingsUiState, actions: SettingsActions) {
    SettingsGroup(stringResource(R.string.extra_gestures_section), Modifier.widthIn(max = 560.dp)) {
        SwitchRow(
            Icons.Filled.PauseCircle,
            stringResource(R.string.flip_to_pause_title),
            state.flipToPauseMediaEnabled,
            actions::setFlipToPause,
            summary = stringResource(R.string.flip_to_pause_description),
        )
        // Screen locking needs device admin support, which a few phones lack
        if (state.deviceAdminAvailability != DeviceAdminAvailability.UNSUPPORTED) {
            SwitchRow(
                Icons.Filled.Lock,
                stringResource(R.string.flip_to_lock_title),
                state.flipToLockEnabled,
                actions::setFlipToLock,
                summary = stringResource(R.string.flip_to_lock_supporting_text),
            )
        }
        if (state.deviceAdminAvailability == DeviceAdminAvailability.ACTIVE) {
            SettingsRow(
                Icons.Filled.LockReset,
                stringResource(R.string.remove_screen_lock_access),
                onClick = actions::removeScreenLockAccess,
                summary = stringResource(R.string.screen_lock_access_allowed),
            )
        }
    }
}

@Composable
private fun KeepRunningGroup(state: SettingsUiState, actions: SettingsActions) {
    SettingsGroup(stringResource(R.string.settings_group_keep_running), Modifier.widthIn(max = 560.dp)) {
        SwitchRow(
            Icons.Filled.RestartAlt,
            stringResource(R.string.start_after_phone_restart),
            state.startAfterPhoneRestart,
            actions::setStartAfterRestart,
            summary = stringResource(R.string.start_after_phone_restart_description),
        )
        SettingsRow(
            Icons.Filled.BatteryChargingFull,
            stringResource(R.string.battery_restrictions),
            onClick = actions::openKeepRunning,
            summary = stringResource(
                when (state.batteryRestricted) {
                    true -> R.string.battery_restricted
                    false -> R.string.battery_not_restricted
                    null -> R.string.battery_unknown
                },
            ),
        )
        SettingsRow(
            Icons.Filled.Dashboard,
            stringResource(R.string.quick_settings_tile_title),
            onClick = if (state.tileAdded) null else actions::addTile,
            summary = stringResource(if (state.tileAdded) R.string.tile_added else R.string.quick_settings_tile_description),
            trailing = if (state.tileAdded) null else ({ TextButton(onClick = actions::addTile) { Text(stringResource(R.string.add_tile)) } }),
        )
    }
}

/** What Flip to Mute tells the user about its own work (future features F10, F31). */
@Composable
private fun NotificationsGroup(state: SettingsUiState, actions: SettingsActions) {
    SettingsGroup(stringResource(R.string.settings_group_notifications), Modifier.widthIn(max = 560.dp)) {
        SwitchRow(
            Icons.Filled.NotificationsActive,
            stringResource(R.string.flip_notification_setting),
            state.flipNotificationEnabled,
            actions::setFlipNotification,
            summary = stringResource(R.string.flip_notification_setting_description),
        )
        SwitchRow(
            Icons.Filled.PhoneCallback,
            stringResource(R.string.callback_reminder_setting),
            state.callbackReminderEnabled,
            actions::setCallbackReminder,
            summary = stringResource(R.string.callback_reminder_setting_description),
        )
        SwitchRow(
            Icons.Filled.Insights,
            stringResource(R.string.weekly_recap_setting),
            state.weeklyRecapEnabled,
            actions::setWeeklyRecap,
            summary = stringResource(R.string.weekly_recap_setting_description),
        )
    }
}

@Composable
private fun AppearanceGroup(state: SettingsUiState, actions: SettingsActions) {
    SettingsGroup(stringResource(R.string.settings_section_appearance), Modifier.widthIn(max = 560.dp)) {
        Text(
            stringResource(R.string.theme_mode_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        )
        FlowRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                ChoicePill(
                    label = stringResource(mode.labelRes()),
                    selected = state.themeMode == mode,
                    onClick = { actions.selectThemeMode(mode) },
                    leadingIcon = mode.icon(),
                )
            }
        }
        Text(
            stringResource(R.string.colour_theme_label),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp),
        )
        FlowRow(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTheme.entries.forEach { theme ->
                ChoicePill(
                    label = stringResource(theme.labelRes()),
                    selected = state.appTheme == theme,
                    onClick = { actions.selectTheme(theme) },
                    dotColor = theme.dotColor(),
                    locked = theme in state.lockedThemes,
                )
            }
        }
    }
}

@Composable
private fun HelpGroup(state: SettingsUiState, actions: SettingsActions) {
    SettingsGroup(stringResource(R.string.settings_group_help), Modifier.widthIn(max = 560.dp)) {
        // One purchase, kept on the Google account (future features F19)
        if (state.removeAdsAvailable) SettingsRow(
            Icons.Filled.Block,
            stringResource(R.string.remove_ads_title),
            onClick = if (state.adsRemoved) null else actions::removeAds,
            summary = when {
                state.adsRemoved -> stringResource(R.string.ads_removed_summary)
                state.removeAdsPrice != null -> stringResource(R.string.remove_ads_summary_price, state.removeAdsPrice)
                else -> stringResource(R.string.remove_ads_summary)
            },
        )
        SettingsRow(Icons.AutoMirrored.Filled.FactCheck, stringResource(R.string.attention_check_setup), onClick = actions::openCheckSetup)
        SettingsRow(Icons.Filled.BugReport, stringResource(R.string.report_a_problem), onClick = actions::reportProblem)
        SettingsRow(Icons.Filled.Info, stringResource(R.string.about_title), onClick = actions::openAbout)
        // Shown once the privacy policy address is set (decision D7, M6-06)
        if (AppConstants.PRIVACY_POLICY_URL.isNotBlank()) {
            SettingsRow(Icons.Filled.Policy, stringResource(R.string.privacy_policy), onClick = actions::openPrivacyPolicy)
        }
        // Where consent law requires it, the ad choice can be changed at any time (M7-02)
        if (state.privacyOptionsRequired) {
            SettingsRow(Icons.Filled.PrivacyTip, stringResource(R.string.privacy_options), onClick = actions::openPrivacyOptions)
        }
    }
}

private fun FlipSensitivity.labelRes(): Int = when (this) {
    FlipSensitivity.QUICK -> R.string.sensitivity_quick
    FlipSensitivity.NORMAL -> R.string.sensitivity_normal
    FlipSensitivity.CAREFUL -> R.string.sensitivity_careful
}

private fun FlipSensitivity.descriptionRes(): Int = when (this) {
    FlipSensitivity.QUICK -> R.string.sensitivity_quick_description
    FlipSensitivity.NORMAL -> R.string.sensitivity_normal_description
    FlipSensitivity.CAREFUL -> R.string.sensitivity_careful_description
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

private fun ThemeMode.icon() = when (this) {
    ThemeMode.SYSTEM -> Icons.Filled.SettingsBrightness
    ThemeMode.LIGHT -> Icons.Filled.LightMode
    ThemeMode.DARK -> Icons.Filled.DarkMode
}

/** The light primary of each theme, as Secret Calculator shows a dot per theme. */
private fun AppTheme.dotColor(): Color = when (this) {
    AppTheme.BLUE -> BlueLightColorScheme.primary
    AppTheme.TEAL -> TealLightColorScheme.primary
    AppTheme.SUNSET -> SunsetLightColorScheme.primary
    AppTheme.FOREST -> ForestLightColorScheme.primary
    AppTheme.ROSE -> RoseLightColorScheme.primary
    AppTheme.MIDNIGHT -> MidnightLightColorScheme.primary
}

private fun QuickSettingsTileAddResult.messageResource() = when (this) {
    QuickSettingsTileAddResult.Added -> R.string.quick_settings_tile_added
    QuickSettingsTileAddResult.AlreadyAdded -> R.string.quick_settings_tile_already_added
    QuickSettingsTileAddResult.NotAdded -> R.string.quick_settings_tile_not_added
    QuickSettingsTileAddResult.RequestInProgress -> R.string.quick_settings_tile_request_in_progress
    QuickSettingsTileAddResult.Failed -> R.string.quick_settings_tile_add_failed
    QuickSettingsTileAddResult.ManualInstructionsRequired -> R.string.quick_settings_tile_add_failed
}

internal object PreviewSettingsActions : SettingsActions {
    override fun back() = Unit
    override fun selectFlipChoice(choice: FlipChoice) = Unit
    override fun setFlatOnly(enabled: Boolean) = Unit
    override fun setPocketProtection(enabled: Boolean) = Unit
    override fun setFeedback(enabled: Boolean) = Unit
    override fun setFlipToLock(enabled: Boolean) = Unit
    override fun removeScreenLockAccess() = Unit
    override fun setStartAfterRestart(enabled: Boolean) = Unit
    override fun setFlipNotification(enabled: Boolean) = Unit
    override fun setWeeklyRecap(enabled: Boolean) = Unit
    override fun setCallbackReminder(enabled: Boolean) = Unit
    override fun selectSensitivity(sensitivity: FlipSensitivity) = Unit
    override fun setRingAgain(enabled: Boolean) = Unit
    override fun setFlipToPause(enabled: Boolean) = Unit
    override fun setScheduleEnabled(enabled: Boolean) = Unit
    override fun toggleScheduleDay(day: DayOfWeek) = Unit
    override fun setScheduleStart(minute: Int) = Unit
    override fun setScheduleEnd(minute: Int) = Unit
    override fun openKeepRunning() = Unit
    override fun addTile() = Unit
    override fun selectThemeMode(mode: ThemeMode) = Unit
    override fun selectTheme(theme: AppTheme) = Unit
    override fun openCheckSetup() = Unit
    override fun reportProblem() = Unit
    override fun openAbout() = Unit
    override fun openPrivacyPolicy() = Unit
    override fun openPrivacyOptions() = Unit
    override fun removeAds() = Unit
    override fun openOtherApp(packageName: String) = Unit
    override fun openMoreApps() = Unit
}
