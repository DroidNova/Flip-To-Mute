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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.ui.components.AppTopBar
import com.droidnova.fliptomute.ui.components.FlipActionOption
import com.droidnova.fliptomute.ui.components.SectionHeader
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.ui.util.RefreshOnResume

@Composable
fun HomeRoute(
    onSettingsClick: () -> Unit,
    onPermissionsClick: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    RefreshOnResume(viewModel::refreshAccessState)
    HomeScreen(
        state = viewModel.uiState.collectAsStateWithLifecycle().value,
        onMonitoringChanged = viewModel::onMonitoringChanged,
        onFlipActionSelected = viewModel::onFlipActionSelected,
        onSetupClick = onPermissionsClick,
        onSettingsClick = onSettingsClick,
        onMessageShown = viewModel::onMessageShown,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onMonitoringChanged: (Boolean) -> Unit,
    onFlipActionSelected: (FlipAction) -> Unit,
    onSetupClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMessageShown: () -> Unit,
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
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(stringResource(R.string.app_name)) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, stringResource(R.string.settings_content_description))
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { MainStatusCard(state, onMonitoringChanged, onSetupClick) }
            item { SectionHeader(stringResource(R.string.when_i_flip)) }
            item {
                FlipActionOption(
                    stringResource(R.string.silent_title),
                    stringResource(R.string.mute_ringtone),
                    Icons.Default.VolumeOff,
                    state.selectedFlipAction == FlipAction.SILENT,
                    { onFlipActionSelected(FlipAction.SILENT) },
                )
            }
            item {
                FlipActionOption(
                    stringResource(R.string.vibrate_title),
                    stringResource(R.string.switch_to_vibration),
                    Icons.Default.Vibration,
                    state.selectedFlipAction == FlipAction.VIBRATE,
                    { onFlipActionSelected(FlipAction.VIBRATE) },
                )
            }
            item {
                Text(
                    stringResource(R.string.home_compact_explanation),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MainStatusCard(
    state: HomeUiState,
    onMonitoringChanged: (Boolean) -> Unit,
    onSetupClick: () -> Unit,
) {
    val runtime = state.monitoringState
    val title = when {
        !state.isSetupComplete -> R.string.home_setup_title
        runtime is MonitoringRuntimeState.Starting -> R.string.turning_on
        runtime is MonitoringRuntimeState.Active -> R.string.monitoring_notification_title
        runtime is MonitoringRuntimeState.Stopping -> R.string.turning_off
        runtime is MonitoringRuntimeState.Error -> R.string.monitoring_start_error
        else -> R.string.flip_to_mute_off
    }
    val body = when {
        !state.isSetupComplete -> R.string.home_setup_description
        runtime is MonitoringRuntimeState.Active -> R.string.monitoring_notification_text
        runtime is MonitoringRuntimeState.Starting -> R.string.preparing_monitoring
        else -> R.string.home_off_description
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), Arrangement.spacedBy(12.dp), Alignment.Start) {
            Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(body))
            if (!state.isSetupComplete) {
                Button(onClick = onSetupClick) { Text(stringResource(R.string.set_up_app)) }
            } else if (runtime is MonitoringRuntimeState.Error) {
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
                    if (runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Stopping) {
                        CircularProgressIndicator()
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
            {}, {}, {}, {}, {},
        )
    }
}
