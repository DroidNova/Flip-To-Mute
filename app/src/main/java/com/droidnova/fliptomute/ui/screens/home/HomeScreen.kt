package com.droidnova.fliptomute.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.service.MonitoringErrorRecoveryIntent
import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.ui.components.AppTopBar
import com.droidnova.fliptomute.ui.components.SectionHeader
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.ui.util.RefreshOnResume
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchEvent
import com.droidnova.fliptomute.quicksettings.MainActivityLaunchRequest

@Composable
fun HomeRoute(
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onPermissionsClick: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
    externalMonitoringRequest: MainActivityLaunchEvent = MainActivityLaunchEvent(),
    onExternalMonitoringRequestConsumed: () -> Unit = {},
) {
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    RefreshOnResume(viewModel::refreshAccessState)
    LaunchedEffect(externalMonitoringRequest.sequence) {
        when (externalMonitoringRequest.request) {
            MainActivityLaunchRequest.OpenSetupAndEnableMonitoring -> viewModel.onMonitoringChanged(true)
            MainActivityLaunchRequest.OpenSetupAndResumeMonitoring -> viewModel.onResumeMonitoring()
            MainActivityLaunchRequest.None -> Unit
        }
        if (externalMonitoringRequest.request != MainActivityLaunchRequest.None) {
            onExternalMonitoringRequestConsumed()
        }
    }
    HomeScreen(
        state = viewModel.uiState.collectAsStateWithLifecycle().value,
        onMonitoringChanged = viewModel::onMonitoringChanged,
        onPauseMonitoring = viewModel::onPauseMonitoring,
        onResumeMonitoring = viewModel::onResumeMonitoring,
        onMuteRingtoneChanged = viewModel::onMuteRingtoneChanged,
        onVibratePhoneChanged = viewModel::onVibratePhoneChanged,
        onSetupClick = onPermissionsClick,
        onSettingsClick = onSettingsClick,
        onAboutClick = onAboutClick,
        onMessageShown = viewModel::onMessageShown,
        onDismissPermissions = viewModel::dismissPermissionsSheet,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onMonitoringChanged: (Boolean) -> Unit,
    onPauseMonitoring: () -> Unit,
    onResumeMonitoring: () -> Unit,
    onMuteRingtoneChanged: (Boolean) -> Unit,
    onVibratePhoneChanged: (Boolean) -> Unit,
    onSetupClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onMessageShown: () -> Unit,
    onDismissPermissions: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    state.message?.let { failure ->
        val text = stringResource(when (failure) {
            MonitoringFailure.SETUP_REQUIRED -> R.string.monitoring_setup_error
            MonitoringFailure.TELEPHONY_UNAVAILABLE -> R.string.monitoring_telephony_error
            MonitoringFailure.NOTIFICATION_UNAVAILABLE -> R.string.monitoring_notification_error
            else -> R.string.monitoring_start_error
        })
        LaunchedEffect(failure) {
            snackbarHostState.showSnackbar(text)
            onMessageShown()
        }
    }
    var menuExpanded by remember { mutableStateOf(false) }
    if (state.showPermissionsSheet) {
        PermissionsSheet(onDismissPermissions, onSetupClick)
    }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(stringResource(R.string.app_name)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.more_options))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.settings_title)) }, onClick = {
                        menuExpanded = false; onSettingsClick()
                    })
                    DropdownMenuItem(text = { Text(stringResource(R.string.about_title)) }, onClick = {
                        menuExpanded = false; onAboutClick()
                    })
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { MainStatusCard(state, onMonitoringChanged, onPauseMonitoring, onResumeMonitoring, onSetupClick) }
            item { SectionHeader(stringResource(R.string.when_i_flip)) }
            item { ActionCheckbox(stringResource(R.string.mute_ringtone), state.callActionSelection.muteRingtone, onMuteRingtoneChanged) }
            item { ActionCheckbox(stringResource(R.string.vibrate_phone), state.callActionSelection.vibratePhone, onVibratePhoneChanged) }
            item {
                Text(
                    stringResource(R.string.home_compact_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsSheet(onDismiss: () -> Unit, onContinue: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.permissions_sheet_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.permissions_sheet_body))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.continue_action))
            }
        }
    }
}

@Composable
private fun ActionCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

@Composable
private fun MainStatusCard(
    state: HomeUiState,
    onMonitoringChanged: (Boolean) -> Unit,
    onPauseMonitoring: () -> Unit,
    onResumeMonitoring: () -> Unit,
    onSetupClick: () -> Unit,
) {
    val runtime = state.monitoringState
    val title = when {
        runtime is MonitoringRuntimeState.Paused -> R.string.monitoring_paused_title
        runtime is MonitoringRuntimeState.Recovering -> R.string.monitoring_recovering_title
        !state.isSetupComplete && !state.isMonitoringChecked -> R.string.home_setup_title
        runtime is MonitoringRuntimeState.Starting -> R.string.turning_on
        runtime is MonitoringRuntimeState.Active -> R.string.monitoring_notification_title
        runtime is MonitoringRuntimeState.Pausing -> R.string.monitoring_pausing_title
        runtime is MonitoringRuntimeState.Resuming -> R.string.monitoring_resuming_title
        runtime is MonitoringRuntimeState.Stopping -> R.string.turning_off
        runtime is MonitoringRuntimeState.Error -> R.string.monitoring_start_error
        else -> R.string.flip_to_mute_off
    }
    val body = when {
        runtime is MonitoringRuntimeState.Paused -> R.string.monitoring_paused_home_text
        runtime is MonitoringRuntimeState.Recovering -> R.string.monitoring_recovering_text
        !state.isSetupComplete && !state.isMonitoringChecked -> R.string.home_setup_description
        runtime is MonitoringRuntimeState.Active -> R.string.monitoring_notification_text
        runtime is MonitoringRuntimeState.Pausing -> R.string.monitoring_pausing_text
        runtime is MonitoringRuntimeState.Resuming -> R.string.preparing_monitoring
        runtime is MonitoringRuntimeState.Starting -> R.string.preparing_monitoring
        else -> R.string.home_off_description
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), Arrangement.spacedBy(12.dp), Alignment.Start) {
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(body))
            if (!state.isSetupComplete && !state.isMonitoringChecked) {
                Button(onClick = onSetupClick) { Text(stringResource(R.string.set_up_app)) }
            } else if (runtime is MonitoringRuntimeState.Error && !state.isMonitoringChecked) {
                Button(onClick = { onMonitoringChanged(true) }) {
                    Text(stringResource(R.string.try_again))
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Switch(
                        checked = state.isMonitoringChecked,
                        onCheckedChange = onMonitoringChanged,
                        enabled = state.isMonitoringSwitchEnabled,
                    )
                    if (runtime is MonitoringRuntimeState.Recovering || runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Pausing ||
                        runtime is MonitoringRuntimeState.Resuming || runtime is MonitoringRuntimeState.Stopping
                    ) {
                        CircularProgressIndicator()
                    }
                }
                if (runtime is MonitoringRuntimeState.Active) {
                    TextButton(onClick = onPauseMonitoring) { Text(stringResource(R.string.pause_monitoring)) }
                } else if (runtime is MonitoringRuntimeState.Paused ||
                    (runtime is MonitoringRuntimeState.Error &&
                        runtime.recoveryIntent == MonitoringErrorRecoveryIntent.RESUME)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onResumeMonitoring) { Text(stringResource(R.string.resume_monitoring)) }
                        TextButton(onClick = { onMonitoringChanged(false) }) { Text(stringResource(R.string.turn_off)) }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    FlipToMuteTheme(dynamicColor = false) {
        HomeScreen(
            HomeUiState(isSetupComplete = true, isMonitoringSwitchEnabled = true),
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}
