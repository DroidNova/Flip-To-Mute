package com.droidnova.fliptomute.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import kotlinx.coroutines.launch
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileAddRequester
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileAddResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.AppTopBar
import com.droidnova.fliptomute.ui.components.FlipActionOption
import com.droidnova.fliptomute.ui.components.SectionHeader
import com.droidnova.fliptomute.ui.components.SettingsItem
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.ui.util.RefreshOnResume
import com.droidnova.fliptomute.deviceadmin.DeviceAdminAvailability

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenSetup: () -> Unit,
    onCallStateTest: () -> Unit,
    onSensorTest: () -> Unit,
    onSoundControlTest: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    RefreshOnResume {
        viewModel.refreshAccessState()
        viewModel.refreshDeviceAdminState()
    }
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val activity = LocalContext.current as Activity
    val addRequester = remember(activity) { QuickSettingsTileAddRequester(activity) }
    val snackbar = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showManualInstructions by remember { mutableStateOf(false) }
    var showAdminExplanation by remember { mutableStateOf(false) }
    var showRemoveAdminConfirmation by remember { mutableStateOf(false) }
    val adminLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onDeviceAdminActivationResult()
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsUiEvent.ShowDeviceAdminExplanation -> showAdminExplanation = true
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
    val onAddTile = {
        addRequester.request { result ->
            if (result == QuickSettingsTileAddResult.ManualInstructionsRequired) {
                showManualInstructions = true
            } else {
                val message = activity.getString(result.messageResource())
                coroutineScope.launch { snackbar.showSnackbar(message) }
            }
        }
    }
    SettingsScreen(
        state = state,
        onBack = onBack,
        onOpenSetup = onOpenSetup,
        onCallStateTest = onCallStateTest,
        onSensorTest = onSensorTest,
        onSoundControlTest = onSoundControlTest,
        onFlipActionSelected = viewModel::onFlipActionSelected,
        onDetectionFeedbackChanged = viewModel::onDetectionFeedbackChanged,
        onRequireFlatSurfaceBeforeFlipChanged = viewModel::onRequireFlatSurfaceBeforeFlipChanged,
        onPocketProtectionChanged = viewModel::onPocketProtectionChanged,
        onStartAfterPhoneRestartChanged = viewModel::onStartAfterPhoneRestartChanged,
        onFlipToLockChanged = viewModel::onFlipToLockChanged,
        onRemoveDeviceAdmin = { showRemoveAdminConfirmation = true },
        onAddQuickSettingsTile = onAddTile,
        snackbarHostState = snackbar,
        showManualTileInstructions = showManualInstructions,
        onDismissManualTileInstructions = { showManualInstructions = false },
        showAdminExplanation = showAdminExplanation,
        onDismissAdminExplanation = { showAdminExplanation = false },
        onContinueAdminExplanation = {
            showAdminExplanation = false
            adminLauncher.launch(viewModel.createDeviceAdminActivationIntent())
        },
        showRemoveAdminConfirmation = showRemoveAdminConfirmation,
        onDismissRemoveAdmin = { showRemoveAdminConfirmation = false },
        onConfirmRemoveAdmin = {
            showRemoveAdminConfirmation = false
            viewModel.onRemoveDeviceAdminConfirmed()
        },
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onOpenSetup: () -> Unit,
    onCallStateTest: () -> Unit,
    onSensorTest: () -> Unit,
    onSoundControlTest: () -> Unit,
    onFlipActionSelected: (FlipAction) -> Unit,
    onDetectionFeedbackChanged: (Boolean) -> Unit,
    onRequireFlatSurfaceBeforeFlipChanged: (Boolean) -> Unit,
    onPocketProtectionChanged: (Boolean) -> Unit,
    onStartAfterPhoneRestartChanged: (Boolean) -> Unit,
    onFlipToLockChanged: (Boolean) -> Unit,
    onRemoveDeviceAdmin: () -> Unit,
    onAddQuickSettingsTile: () -> Unit,
    snackbarHostState: SnackbarHostState,
    showManualTileInstructions: Boolean,
    onDismissManualTileInstructions: () -> Unit,
    showAdminExplanation: Boolean,
    onDismissAdminExplanation: () -> Unit,
    onContinueAdminExplanation: () -> Unit,
    showRemoveAdminConfirmation: Boolean,
    onDismissRemoveAdmin: () -> Unit,
    onConfirmRemoveAdmin: () -> Unit,
) {
    if (showManualTileInstructions) {
        AlertDialog(
            onDismissRequest = onDismissManualTileInstructions,
            title = { Text(stringResource(R.string.add_quick_settings_tile_title)) },
            text = { Text(stringResource(R.string.add_quick_settings_tile_instructions)) },
            confirmButton = {
                TextButton(onClick = onDismissManualTileInstructions) { Text(stringResource(R.string.got_it)) }
            },
        )
    }
    if (showAdminExplanation) {
        AlertDialog(
            onDismissRequest = onDismissAdminExplanation,
            title = { Text(stringResource(R.string.allow_screen_locking_title)) },
            text = { Text(stringResource(R.string.allow_screen_locking_message)) },
            confirmButton = {
                TextButton(onClick = onContinueAdminExplanation) { Text(stringResource(R.string.continue_action)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissAdminExplanation) { Text(stringResource(R.string.cancel_action)) }
            },
        )
    }
    if (showRemoveAdminConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissRemoveAdmin,
            title = { Text(stringResource(R.string.remove_screen_lock_access_title)) },
            text = { Text(stringResource(R.string.remove_screen_lock_access_message)) },
            confirmButton = {
                TextButton(onClick = onConfirmRemoveAdmin) { Text(stringResource(R.string.remove_action)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissRemoveAdmin) { Text(stringResource(R.string.cancel_action)) }
            },
        )
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(stringResource(R.string.settings_title), onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionHeader(stringResource(R.string.behaviour_section)) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onRequireFlatSurfaceBeforeFlipChanged(!state.requireFlatSurfaceBeforeFlip)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.only_when_lying_flat))
                        Text(stringResource(R.string.only_when_lying_flat_description))
                    }
                    Checkbox(
                        checked = state.requireFlatSurfaceBeforeFlip,
                        onCheckedChange = null,
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = state.isProximitySensorAvailable) {
                            onPocketProtectionChanged(!state.pocketProtectionEnabled)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.pocket_protection))
                        Text(stringResource(if (state.isProximitySensorAvailable) {
                            R.string.pocket_protection_description
                        } else R.string.pocket_protection_unavailable))
                    }
                    Switch(
                        checked = state.pocketProtectionEnabled && state.isProximitySensorAvailable,
                        enabled = state.isProximitySensorAvailable,
                        onCheckedChange = null,
                    )
                }
            }
            item {
                FlipActionOption(
                    title = stringResource(R.string.silent_title),
                    description = stringResource(R.string.silent_description),
                    icon = Icons.Default.VolumeOff,
                    selected = state.selectedFlipAction == FlipAction.SILENT,
                    onClick = { onFlipActionSelected(FlipAction.SILENT) },
                )
            }
            item {
                FlipActionOption(
                    title = stringResource(R.string.vibrate_title),
                    description = stringResource(R.string.vibrate_description),
                    icon = Icons.Default.Vibration,
                    selected = state.selectedFlipAction == FlipAction.VIBRATE,
                    onClick = { onFlipActionSelected(FlipAction.VIBRATE) },
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.detection_feedback))
                        Text(stringResource(R.string.detection_feedback_description))
                    }
                    Switch(
                        checked = state.detectionFeedbackEnabled,
                        onCheckedChange = onDetectionFeedbackChanged,
                    )
                }
            }
            item { SectionHeader(stringResource(R.string.extra_gestures_section)) }
            item {
                val supported = state.deviceAdminAvailability != DeviceAdminAvailability.UNSUPPORTED
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = supported) { onFlipToLockChanged(!state.flipToLockEnabled) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.flip_to_lock_title))
                        Text(stringResource(R.string.flip_to_lock_supporting_text))
                        Text(stringResource(R.string.flip_to_lock_secondary_text))
                        when {
                            !supported -> Text(stringResource(R.string.screen_locking_unavailable))
                            state.flipToLockEnabled && state.deviceAdminAvailability == DeviceAdminAvailability.ACTIVE ->
                                Text(stringResource(R.string.screen_lock_access_allowed))
                        }
                        if (!state.flipToLockEnabled && state.deviceAdminAvailability == DeviceAdminAvailability.ACTIVE) {
                            TextButton(onClick = onRemoveDeviceAdmin) {
                                Text(stringResource(R.string.remove_screen_lock_access))
                            }
                        }
                    }
                    Switch(
                        checked = state.flipToLockEnabled,
                        enabled = supported,
                        onCheckedChange = null,
                    )
                }
            }
            item { SectionHeader(stringResource(R.string.convenience_section)) }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStartAfterPhoneRestartChanged(!state.startAfterPhoneRestart) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.start_after_phone_restart))
                        Text(stringResource(R.string.start_after_phone_restart_description))
                    }
                    Switch(checked = state.startAfterPhoneRestart, onCheckedChange = null)
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onAddQuickSettingsTile).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.quick_settings_tile_title))
                        Text(stringResource(R.string.quick_settings_tile_description))
                    }
                    TextButton(onClick = onAddQuickSettingsTile) { Text(stringResource(R.string.add_tile)) }
                }
            }
            item { SectionHeader(stringResource(R.string.monitoring_section)) }
            item {
                SettingsItem(
                    stringResource(R.string.monitoring_status),
                    stringResource(
                        if (state.monitoringEnabled) R.string.setting_enabled else R.string.setting_disabled,
                    ),
                )
            }
            item { SectionHeader(stringResource(R.string.setup_section)) }
            item {
                SettingsItem(
                    stringResource(R.string.phone_access_title),
                    accessStatusText(state.accessState.phoneStateStatus),
                )
            }
            item {
                SettingsItem(
                    stringResource(R.string.sound_access_title),
                    accessStatusText(state.accessState.soundControlStatus),
                )
            }
            item {
                SettingsItem(
                    stringResource(R.string.notifications_title),
                    accessStatusText(state.accessState.notificationStatus),
                )
            }
            item {
                Button(onClick = onOpenSetup, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.open_app_setup))
                }
            }
            item {
                SettingsItem(
                    stringResource(R.string.notifications_title),
                    stringResource(R.string.monitoring_notification_description),
                )
            }
            item { SectionHeader(stringResource(R.string.diagnostics_section)) }
            item {
                Button(onClick = onSensorTest, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.test_flip_title))
                }
                Text(stringResource(R.string.test_flip_description))
            }
            item {
                Button(onClick = onCallStateTest, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.test_call_detection))
                }
                Text(stringResource(R.string.test_call_detection_description))
            }
            item {
                Button(onClick = onSoundControlTest, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.test_sound_control))
                }
                Text(stringResource(R.string.test_sound_control_description))
            }
            item { SectionHeader(stringResource(R.string.about_section)) }
            item { SettingsItem(stringResource(R.string.privacy_policy), stringResource(R.string.privacy_policy_unavailable)) }
            item { SettingsItem(stringResource(R.string.app_version), stringResource(R.string.app_version_value)) }
            item { SettingsItem(stringResource(R.string.about_app), stringResource(R.string.about_app_description)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    FlipToMuteTheme(dynamicColor = false) {
        SettingsScreen(
            state = SettingsUiState(),
            onBack = {},
            onOpenSetup = {},
            onCallStateTest = {},
            onSensorTest = {},
            onSoundControlTest = {},
            onFlipActionSelected = {},
            onDetectionFeedbackChanged = {},
            onRequireFlatSurfaceBeforeFlipChanged = {},
            onPocketProtectionChanged = {},
            onStartAfterPhoneRestartChanged = {},
            onFlipToLockChanged = {},
            onRemoveDeviceAdmin = {},
            onAddQuickSettingsTile = {},
            snackbarHostState = remember { SnackbarHostState() },
            showManualTileInstructions = false,
            onDismissManualTileInstructions = {},
            showAdminExplanation = false,
            onDismissAdminExplanation = {},
            onContinueAdminExplanation = {},
            showRemoveAdminConfirmation = false,
            onDismissRemoveAdmin = {},
            onConfirmRemoveAdmin = {},
        )
    }
}

private fun QuickSettingsTileAddResult.messageResource() = when (this) {
    QuickSettingsTileAddResult.Added -> R.string.quick_settings_tile_added
    QuickSettingsTileAddResult.AlreadyAdded -> R.string.quick_settings_tile_already_added
    QuickSettingsTileAddResult.NotAdded -> R.string.quick_settings_tile_not_added
    QuickSettingsTileAddResult.RequestInProgress -> R.string.quick_settings_tile_request_in_progress
    QuickSettingsTileAddResult.Failed -> R.string.quick_settings_tile_add_failed
    QuickSettingsTileAddResult.ManualInstructionsRequired -> R.string.quick_settings_tile_add_failed
}

@Composable
private fun accessStatusText(status: SetupAccessStatus) = stringResource(
    when (status) {
        SetupAccessStatus.GRANTED -> R.string.allowed
        SetupAccessStatus.NOT_GRANTED -> R.string.not_allowed
        SetupAccessStatus.NOT_SUPPORTED -> R.string.not_supported
    },
)
