package com.droidnova.fliptomute.ui.screens.home

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchEvent
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequest
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileAddRequester
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.ui.common.HomeHint
import com.droidnova.fliptomute.ui.components.ActionSelector
import com.droidnova.fliptomute.ui.components.AttentionCard
import com.droidnova.fliptomute.ui.components.FlipChoice
import com.droidnova.fliptomute.ui.components.FlipPowerControl
import com.droidnova.fliptomute.ui.components.IconBadge
import com.droidnova.fliptomute.ui.components.NovaCard
import com.droidnova.fliptomute.ui.components.NovaTopBar
import com.droidnova.fliptomute.ui.components.PowerControlState
import com.droidnova.fliptomute.ui.components.SectionLabel
import com.droidnova.fliptomute.ui.components.SheetAction
import com.droidnova.fliptomute.ui.components.SheetHeader
import com.droidnova.fliptomute.ui.components.StatsCard
import com.droidnova.fliptomute.ui.components.appearIn
import com.droidnova.fliptomute.ui.util.RefreshOnResume
import com.droidnova.fliptomute.utils.findActivity
import com.droidnova.fliptomute.utils.openBatteryOptimizationSettings
import java.util.Date

/** Everything Home can ask for, as Secret Calculator's screens take an actions interface. */
interface HomeActions {
    fun powerControl()
    fun showPauseSheet()
    fun pauseFor(minutes: Int?)
    fun dismissPauseSheet()
    fun resume()
    fun turnOff()
    fun selectFlipChoice(choice: FlipChoice)
    fun attentionFix(fix: AttentionFix)
    fun batteryFix()
    fun batteryLater()
    fun discoveryAccepted(hint: HomeHint)
    fun discoveryDismissed(hint: HomeHint)
    fun dialogConfirmed()
    fun dialogDismissed()
    fun permissionsSheetContinue()
    fun permissionsSheetDismissed()
    fun openSettings()
    fun openActivity()
    fun messageShown()
}

@Composable
fun HomeRoute(
    onSettingsClick: () -> Unit,
    onOpenAccess: () -> Unit,
    onCheckSetup: () -> Unit,
    /** The stats card leads to the activity screen (future features F13). */
    onOpenActivity: () -> Unit = {},
    /** Home is on with nothing to fix and no dialog: a good moment for the review prompt (M6-07). */
    onCalm: () -> Unit = {},
    /** The Update button: Play's in-app update, or the store page where that is not possible (M7-09). */
    onStartUpdate: () -> Unit = {},
    externalMonitoringRequest: MainActivityLaunchEvent = MainActivityLaunchEvent(),
    onExternalMonitoringRequestConsumed: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refreshAccessState)
    LaunchedEffect(externalMonitoringRequest.sequence) {
        when (externalMonitoringRequest.request) {
            MainActivityLaunchRequest.OpenSetupAndEnableMonitoring -> viewModel.onMonitoringChanged(true)
            MainActivityLaunchRequest.OpenSetupAndResumeMonitoring -> viewModel.onResumeMonitoring()
            // The nav host shows the activity screen itself
            is MainActivityLaunchRequest.OpenActivity, MainActivityLaunchRequest.None -> return@LaunchedEffect
        }
        onExternalMonitoringRequestConsumed()
    }
    val calm = state.status == HomeStatus.ON && state.dialog == null && state.cards.none { it is HomeCard.NeedsAttention }
    LaunchedEffect(calm) {
        if (calm) onCalm()
    }
    // One-shot requests that need the activity
    LaunchedEffect(state.event) {
        when (state.event) {
            HomeEvent.OPEN_ACCESS -> onOpenAccess()
            HomeEvent.OPEN_CHECK_SETUP -> onCheckSetup()
            HomeEvent.OPEN_SETTINGS -> onSettingsClick()
            HomeEvent.OPEN_BATTERY_SETTINGS -> context.openBatteryOptimizationSettings()
            HomeEvent.REQUEST_TILE -> context.findActivity()?.let { QuickSettingsTileAddRequester(it).request { } }
            HomeEvent.OPEN_STORE -> onStartUpdate()
            null -> return@LaunchedEffect
        }
        viewModel.onEventHandled()
    }
    HomeScreen(
        state = state,
        actions = object : HomeActions {
            override fun powerControl() = viewModel.onPowerControlTapped()
            override fun showPauseSheet() = viewModel.showPauseSheet()
            override fun pauseFor(minutes: Int?) = viewModel.onPauseFor(minutes)
            override fun dismissPauseSheet() = viewModel.dismissPauseSheet()
            override fun resume() = viewModel.onResumeMonitoring()
            override fun turnOff() = viewModel.onMonitoringChanged(false)
            override fun selectFlipChoice(choice: FlipChoice) = viewModel.onFlipChoiceSelected(choice)
            override fun attentionFix(fix: AttentionFix) = viewModel.onAttentionFix(fix)
            override fun batteryFix() = viewModel.onBatteryFix()
            override fun batteryLater() = viewModel.onBatteryLater()
            override fun discoveryAccepted(hint: HomeHint) = viewModel.onDiscoveryAccepted(hint)
            override fun discoveryDismissed(hint: HomeHint) = viewModel.onDiscoveryDismissed(hint)
            override fun dialogConfirmed() = viewModel.onDialogConfirmed()
            override fun dialogDismissed() = viewModel.onDialogDismissed()
            override fun permissionsSheetContinue() = viewModel.onPermissionsSheetContinue()
            override fun permissionsSheetDismissed() = viewModel.dismissPermissionsSheet()
            override fun openSettings() = onSettingsClick()
            override fun openActivity() = onOpenActivity()
            override fun messageShown() = viewModel.onMessageShown()
        },
    )
}

@Composable
fun HomeScreen(state: HomeUiState, actions: HomeActions) {
    val snackbarHostState = remember { SnackbarHostState() }
    val messageText = state.message?.let { stringResource(attentionFor(MonitoringRuntimeState.Error(it)).title) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            actions.messageShown()
        }
    }
    Scaffold(
        topBar = {
            NovaTopBar(title = stringResource(R.string.app_name), onBack = null) {
                IconButton(onClick = actions::openSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        // MainActivity pads for the system bars once
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item(key = "status") { StatusBlock(state, actions) }
            item(key = "selector") {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.home_when_i_flip), Modifier.padding(start = 6.dp))
                    ActionSelector(state.flipChoice, actions::selectFlipChoice)
                }
            }
            items(state.cards, key = { it.key() }) { card ->
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().appearIn(0)) { HomeCardView(card, actions) }
            }
        }
    }
    if (state.showPauseSheet) PauseSheet(actions)
    if (state.showPermissionsSheet) PermissionsSheet(actions)
    state.dialog?.let { HomeDialogView(it, actions) }
}

@Composable
private fun StatusBlock(state: HomeUiState, actions: HomeActions) {
    val compact = LocalConfiguration.current.screenHeightDp < 600
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlipPowerControl(
            state = state.status.toPowerControl(),
            onToggle = { actions.powerControl() },
            size = if (compact) 120.dp else 168.dp,
        )
        Text(
            headline(state),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        supportingLine(state)?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusButtons(state, actions)
    }
}

@Composable
private fun StatusButtons(state: HomeUiState, actions: HomeActions) {
    when (state.status) {
        HomeStatus.SETUP_NEEDED -> Button(onClick = actions::powerControl, shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.home_continue_setup))
        }
        HomeStatus.ON -> OutlinedButton(onClick = actions::showPauseSheet, shape = RoundedCornerShape(50), modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Filled.Pause, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.home_pause))
        }
        HomeStatus.PAUSED -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = actions::resume, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(if (state.pausedUntilEpochMs != null) R.string.home_resume_now else R.string.resume_monitoring))
            }
            TextButton(onClick = actions::turnOff, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.turn_off))
            }
        }
        else -> Unit
    }
}

@Composable
private fun headline(state: HomeUiState): String = when (state.status) {
    HomeStatus.SETUP_NEEDED -> stringResource(R.string.home_setup_needed_title)
    HomeStatus.CHECKING -> stringResource(R.string.home_checking_title)
    HomeStatus.OFF -> stringResource(R.string.home_off_title)
    HomeStatus.TURNING_ON -> stringResource(R.string.home_turning_on_title)
    HomeStatus.ON -> stringResource(R.string.home_on_title)
    HomeStatus.BUSY -> stringResource(R.string.home_busy_title)
    HomeStatus.PAUSED -> state.pausedUntilEpochMs?.let { stringResource(R.string.paused_until_title, formatTime(it)) }
        ?: stringResource(R.string.home_paused_title)
    HomeStatus.ATTENTION -> stringResource(R.string.home_attention_title)
}

@Composable
private fun supportingLine(state: HomeUiState): String? = when (state.status) {
    HomeStatus.SETUP_NEEDED -> stringResource(R.string.home_setup_needed_body)
    HomeStatus.OFF -> stringResource(R.string.home_off_body)
    HomeStatus.ON -> stringResource(R.string.home_on_body)
    HomeStatus.PAUSED -> stringResource(if (state.pausedUntilEpochMs != null) R.string.paused_until_text else R.string.home_paused_body)
    else -> null
}

@Composable
private fun formatTime(epochMs: Long): String = DateFormat.getTimeFormat(LocalContext.current).format(Date(epochMs))

private fun HomeStatus.toPowerControl(): PowerControlState = when (this) {
    HomeStatus.ON -> PowerControlState.ON
    HomeStatus.PAUSED -> PowerControlState.PAUSED
    HomeStatus.ATTENTION -> PowerControlState.ATTENTION
    HomeStatus.CHECKING, HomeStatus.TURNING_ON, HomeStatus.BUSY -> PowerControlState.BUSY
    HomeStatus.OFF, HomeStatus.SETUP_NEEDED -> PowerControlState.OFF
}

private fun HomeCard.key(): String = when (this) {
    is HomeCard.NeedsAttention -> "attention"
    HomeCard.BatteryWarning -> "battery"
    is HomeCard.Stats -> "stats"
    is HomeCard.Discovery -> "discovery_${hint.name}"
}

@Composable
private fun HomeCardView(card: HomeCard, actions: HomeActions) {
    when (card) {
        is HomeCard.NeedsAttention -> {
            val attention = card.attention
            AttentionCard(
                title = stringResource(attention.title),
                body = attention.body?.let { stringResource(it) },
                actionLabel = attention.fixLabel?.let { stringResource(it) },
                onAction = { actions.attentionFix(attention.fix) },
            )
        }
        HomeCard.BatteryWarning -> PromptCard(
            icon = Icons.Filled.BatteryAlert,
            title = stringResource(R.string.battery_card_title),
            body = null,
            accept = stringResource(R.string.battery_card_fix),
            decline = stringResource(R.string.battery_card_later),
            onAccept = actions::batteryFix,
            onDecline = actions::batteryLater,
        )
        is HomeCard.Stats -> StatsCard(
            silencedThisMonth = card.stats.thisMonth,
            lastFlip = card.stats.lastFlipAt?.let { lastFlipText(it) },
            onClick = actions::openActivity,
        )
        is HomeCard.Discovery -> {
            val (icon, title, body, accept) = when (card.hint) {
                HomeHint.START_AFTER_RESTART -> DiscoveryText(Icons.Filled.RestartAlt, R.string.discovery_restart_title, R.string.discovery_restart_body, R.string.discovery_restart_action)
                HomeHint.FLIP_TO_LOCK -> DiscoveryText(Icons.Filled.Lock, R.string.discovery_lock_title, R.string.discovery_lock_body, R.string.discovery_lock_action)
                HomeHint.QUICK_SETTINGS_TILE -> DiscoveryText(Icons.Filled.Dashboard, R.string.discovery_tile_title, R.string.discovery_tile_body, R.string.discovery_tile_action)
                HomeHint.CHECK_SETUP -> DiscoveryText(Icons.AutoMirrored.Filled.FactCheck, R.string.discovery_check_title, R.string.discovery_check_body, R.string.attention_check_setup)
                HomeHint.FLAT_ONLY -> DiscoveryText(Icons.Filled.Layers, R.string.discovery_flat_title, R.string.discovery_flat_body, R.string.discovery_restart_action)
            }
            PromptCard(
                icon = icon,
                title = stringResource(title),
                body = stringResource(body),
                accept = stringResource(accept),
                decline = stringResource(R.string.discovery_not_now),
                onAccept = { actions.discoveryAccepted(card.hint) },
                onDecline = { actions.discoveryDismissed(card.hint) },
            )
        }
    }
}

private data class DiscoveryText(val icon: ImageVector, val title: Int, val body: Int, val accept: Int)

/** A card that asks for one thing: icon, title, optional line, then "do it" and "not now". */
@Composable
private fun PromptCard(
    icon: ImageVector,
    title: String,
    body: String?,
    accept: String,
    decline: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    NovaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (body != null) {
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(Modifier.align(Alignment.End).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDecline, modifier = Modifier.heightIn(min = 48.dp)) { Text(decline) }
            FilledTonalButton(onClick = onAccept, modifier = Modifier.heightIn(min = 48.dp)) { Text(accept) }
        }
    }
}

@Composable
private fun lastFlipText(epochMs: Long): String {
    val context = LocalContext.current
    return if (isSameDay(epochMs, System.currentTimeMillis())) {
        stringResource(R.string.stats_today_at, formatTime(epochMs))
    } else {
        DateUtils.formatDateTime(
            context, epochMs,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PauseSheet(actions: HomeActions) {
    ModalBottomSheet(
        onDismissRequest = actions::dismissPauseSheet,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SheetHeader(Icons.Filled.Pause, stringResource(R.string.pause_sheet_title), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            SheetAction(Icons.Filled.Timer, stringResource(R.string.pause_30_minutes), { actions.pauseFor(30) })
            SheetAction(Icons.Filled.Schedule, stringResource(R.string.pause_1_hour), { actions.pauseFor(60) })
            SheetAction(Icons.Filled.Schedule, stringResource(R.string.pause_2_hours), { actions.pauseFor(120) })
            SheetAction(Icons.Filled.Pause, stringResource(R.string.pause_until_turned_on), { actions.pauseFor(null) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsSheet(actions: HomeActions) {
    ModalBottomSheet(
        onDismissRequest = actions::permissionsSheetDismissed,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.permissions_sheet_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.permissions_sheet_body), color = MaterialTheme.colorScheme.onSurface)
            Button(onClick = actions::permissionsSheetContinue, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.continue_action))
            }
        }
    }
}

@Composable
private fun HomeDialogView(dialog: HomeDialog, actions: HomeActions) {
    when (dialog) {
        HomeDialog.WHATS_NEW -> AlertDialog(
            onDismissRequest = actions::dialogDismissed,
            title = { Text(stringResource(R.string.whats_new_title)) },
            text = { Text(stringResource(R.string.whats_new_body)) },
            confirmButton = { TextButton(onClick = actions::dialogConfirmed) { Text(stringResource(R.string.got_it)) } },
        )
        HomeDialog.UPDATE_AVAILABLE -> AlertDialog(
            onDismissRequest = actions::dialogDismissed,
            title = { Text(stringResource(R.string.update_available_title)) },
            text = { Text(stringResource(R.string.update_available_body)) },
            confirmButton = { TextButton(onClick = actions::dialogConfirmed) { Text(stringResource(R.string.update_available_action)) } },
            dismissButton = { TextButton(onClick = actions::dialogDismissed) { Text(stringResource(R.string.battery_card_later)) } },
        )
    }
}

internal object PreviewHomeActions : HomeActions {
    override fun powerControl() = Unit
    override fun showPauseSheet() = Unit
    override fun pauseFor(minutes: Int?) = Unit
    override fun dismissPauseSheet() = Unit
    override fun resume() = Unit
    override fun turnOff() = Unit
    override fun selectFlipChoice(choice: FlipChoice) = Unit
    override fun attentionFix(fix: AttentionFix) = Unit
    override fun batteryFix() = Unit
    override fun batteryLater() = Unit
    override fun discoveryAccepted(hint: HomeHint) = Unit
    override fun discoveryDismissed(hint: HomeHint) = Unit
    override fun dialogConfirmed() = Unit
    override fun dialogDismissed() = Unit
    override fun permissionsSheetContinue() = Unit
    override fun permissionsSheetDismissed() = Unit
    override fun openSettings() = Unit
    override fun openActivity() = Unit
    override fun messageShown() = Unit
}


/** Same calendar day in the phone's time zone; uses the same clock that recorded the flip. */
internal fun isSameDay(a: Long, b: Long): Boolean {
    val first = java.util.Calendar.getInstance().apply { timeInMillis = a }
    val second = java.util.Calendar.getInstance().apply { timeInMillis = b }
    return first.get(java.util.Calendar.YEAR) == second.get(java.util.Calendar.YEAR) &&
        first.get(java.util.Calendar.DAY_OF_YEAR) == second.get(java.util.Calendar.DAY_OF_YEAR)
}
