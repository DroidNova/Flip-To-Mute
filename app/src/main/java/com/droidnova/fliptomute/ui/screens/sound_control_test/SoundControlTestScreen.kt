package com.droidnova.fliptomute.ui.screens.sound_control_test

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.audio.DeviceRingerMode
import com.droidnova.fliptomute.ui.components.AppTopBar
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme

@Composable
fun SoundControlTestScreen(
    onBack: () -> Unit,
    onOpenSetup: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val viewModel: SoundControlTestViewModel = viewModel(factory = viewModelFactory)
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    RestoreSoundOnStop(
        onStop = viewModel::onScreenLeaving,
        onResume = viewModel::refreshCurrentMode,
    )
    SoundControlTestContent(
        state = state,
        onBack = onBack,
        onOpenSetup = onOpenSetup,
        onStart = viewModel::startTest,
        onRestore = viewModel::restoreNow,
    )
}

@Composable
private fun RestoreSoundOnStop(onStop: () -> Unit, onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnStop = rememberUpdatedState(onStop)
    val currentOnResume = rememberUpdatedState(onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> currentOnStop.value()
                Lifecycle.Event.ON_RESUME -> currentOnResume.value()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            currentOnStop.value()
        }
    }
}

@Composable
private fun SoundControlTestContent(
    state: SoundControlTestUiState,
    onBack: () -> Unit,
    onOpenSetup: () -> Unit,
    onStart: () -> Unit,
    onRestore: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(stringResource(R.string.test_sound_control), onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.current_sound_mode, modeText(state.currentMode)),
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.test_action), style = MaterialTheme.typography.titleMedium)
                    Text(actionText(state.selectedAction), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.change_action_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        !state.hasSoundControlAccess -> {
                            Text(
                                stringResource(R.string.sound_control_access_required),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(stringResource(R.string.sound_control_access_required_description))
                            Button(onClick = onOpenSetup, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.open_app_setup))
                            }
                        }
                        !state.canChangeSoundMode -> {
                            Text(stringResource(R.string.sound_mode_unsupported))
                        }
                        else -> {
                            Button(
                                onClick = onStart,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.isTestRunning,
                            ) {
                                Text(
                                    stringResource(
                                        if (state.selectedAction == FlipAction.SILENT) {
                                            R.string.test_silent_mode
                                        } else {
                                            R.string.test_vibrate_mode
                                        },
                                    ),
                                )
                            }
                            if (state.isTestRunning) {
                                Text(stringResource(R.string.restoring_countdown, state.remainingSeconds))
                                OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.restore_now))
                                }
                            }
                        }
                    }
                }
            }
            state.result?.let { result -> item { ResultMessage(result) } }
            item {
                Text(
                    stringResource(R.string.sound_test_note),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ResultMessage(result: SoundControlTestResult) {
    val text = when (result) {
        SoundControlTestResult.TEST_STARTED -> R.string.sound_test_started
        SoundControlTestResult.RESTORED -> R.string.sound_mode_restored
        SoundControlTestResult.MANUAL_CHANGE_PRESERVED -> R.string.manual_sound_change_preserved
        SoundControlTestResult.ALREADY_SET -> R.string.sound_mode_already_set
        SoundControlTestResult.ACCESS_REQUIRED -> R.string.sound_control_access_required_description
        SoundControlTestResult.DEVICE_NOT_SUPPORTED -> R.string.sound_mode_unsupported
        SoundControlTestResult.CHANGE_FAILED -> R.string.sound_mode_change_failed
    }
    Text(stringResource(text), color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun modeText(mode: DeviceRingerMode) = stringResource(
    when (mode) {
        DeviceRingerMode.NORMAL -> R.string.mode_normal
        DeviceRingerMode.VIBRATE -> R.string.mode_vibrate
        DeviceRingerMode.SILENT -> R.string.mode_silent
        DeviceRingerMode.UNKNOWN -> R.string.mode_unknown
    },
)

@Composable
private fun actionText(action: FlipAction) = stringResource(
    if (action == FlipAction.SILENT) R.string.silent_title else R.string.vibrate_title,
)

@Preview(showBackground = true) @Composable private fun SilentReadyPreview() = PreviewSound(SoundControlTestUiState(hasSoundControlAccess = true))
@Preview(showBackground = true) @Composable private fun VibrateReadyPreview() = PreviewSound(
    SoundControlTestUiState(selectedAction = FlipAction.VIBRATE, hasSoundControlAccess = true),
)
@Preview(showBackground = true) @Composable private fun RunningPreview() = PreviewSound(
    SoundControlTestUiState(hasSoundControlAccess = true, isTestRunning = true, remainingSeconds = 3),
)
@Preview(showBackground = true) @Composable private fun AccessPreview() = PreviewSound(SoundControlTestUiState())
@Preview(showBackground = true) @Composable private fun UnsupportedPreview() = PreviewSound(
    SoundControlTestUiState(hasSoundControlAccess = true, canChangeSoundMode = false),
)
@Preview(showBackground = true) @Composable private fun RestoredPreview() = PreviewSound(
    SoundControlTestUiState(hasSoundControlAccess = true, result = SoundControlTestResult.RESTORED),
)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable private fun ManualChangeDarkPreview() = PreviewSound(
    SoundControlTestUiState(
        hasSoundControlAccess = true,
        result = SoundControlTestResult.MANUAL_CHANGE_PRESERVED,
    ),
)

@Composable private fun PreviewSound(state: SoundControlTestUiState) {
    FlipToMuteTheme(dynamicColor = false) { SoundControlTestContent(state, {}, {}, {}, {}) }
}
