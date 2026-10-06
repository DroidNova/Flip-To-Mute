package com.droidnova.fliptomute.ui.screens.check_setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.telephony.CellularCallState
import com.droidnova.fliptomute.ui.components.IconBadge
import com.droidnova.fliptomute.ui.components.NovaCard
import com.droidnova.fliptomute.ui.components.NovaTopBar
import com.droidnova.fliptomute.ui.screens.call_state_test.CallStateTestViewModel
import com.droidnova.fliptomute.ui.screens.sensor_test.SensorTestViewModel
import com.droidnova.fliptomute.ui.screens.sound_control_test.SoundControlTestViewModel
import com.droidnova.fliptomute.ui.theme.stateColors
import com.droidnova.fliptomute.ui.util.RefreshOnResume

/** What the guided flow can ask for. */
interface CheckSetupActions {
    fun back()
    fun start(step: CheckStep)
    fun skip(step: CheckStep)
    fun fix(step: CheckStep)
    fun sendReport()
}

@Composable
fun CheckSetupRoute(
    onBack: () -> Unit,
    onOpenAccess: () -> Unit,
    onOpenKeepRunning: () -> Unit,
    onSendReport: (String) -> Unit,
    viewModel: CheckSetupViewModel = hiltViewModel(),
    sensorViewModel: SensorTestViewModel = hiltViewModel(),
    soundViewModel: SoundControlTestViewModel = hiltViewModel(),
    callViewModel: CallStateTestViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sensor by sensorViewModel.uiState.collectAsStateWithLifecycle()
    val sound by soundViewModel.uiState.collectAsStateWithLifecycle()
    val calls by callViewModel.uiState.collectAsStateWithLifecycle()
    RefreshOnResume(viewModel::refreshAutomaticSteps)

    // The three test view models report into the guided flow
    LaunchedEffect(sensor.orientation, sensor.isSensorAvailable, state[CheckStep.FLIP_SENSOR]) {
        if (state[CheckStep.FLIP_SENSOR] == StepResult.RUNNING) {
            viewModel.onSensorResult(sensor.orientation == DeviceOrientation.FACE_DOWN, sensor.isSensorAvailable && !sensor.hasError)
            if (sensor.orientation == DeviceOrientation.FACE_DOWN) sensorViewModel.stopTest()
        }
    }
    LaunchedEffect(sound.result, state[CheckStep.SOUND]) {
        if (state[CheckStep.SOUND] == StepResult.RUNNING) viewModel.onSoundResult(sound.result)
    }
    LaunchedEffect(calls.currentCallState, calls.recentTransitions, state[CheckStep.CALLS]) {
        if (state[CheckStep.CALLS] == StepResult.RUNNING) {
            when {
                !calls.isTelephonyAvailable -> viewModel.onCallsUnavailable()
                calls.currentCallState == CellularCallState.RINGING || CellularCallState.RINGING in calls.recentTransitions -> {
                    viewModel.onCallRinging()
                    callViewModel.stopListening()
                }
            }
        }
    }
    // Leaving puts everything back: sensor off, sound mode restored, call listener stopped
    DisposableEffect(Unit) {
        onDispose {
            sensorViewModel.stopTest()
            soundViewModel.onScreenLeaving()
            callViewModel.stopListening()
        }
    }

    CheckSetupScreen(
        state = state,
        actions = object : CheckSetupActions {
            override fun back() = onBack()
            override fun start(step: CheckStep) {
                viewModel.onStepStarted(step)
                when (step) {
                    CheckStep.FLIP_SENSOR -> sensorViewModel.startTest()
                    CheckStep.SOUND -> soundViewModel.startTest()
                    CheckStep.CALLS -> callViewModel.startListening()
                    CheckStep.ACCESS, CheckStep.BATTERY -> viewModel.refreshAutomaticSteps()
                }
            }
            override fun skip(step: CheckStep) {
                if (step == CheckStep.CALLS) callViewModel.stopListening()
                viewModel.onStepSkipped(step)
            }
            override fun fix(step: CheckStep) {
                when (step) {
                    CheckStep.ACCESS, CheckStep.SOUND -> onOpenAccess()
                    CheckStep.BATTERY -> onOpenKeepRunning()
                    CheckStep.FLIP_SENSOR, CheckStep.CALLS -> start(step)
                }
            }
            override fun sendReport() = onSendReport(viewModel.reportText())
        },
    )
}

@Composable
fun CheckSetupScreen(state: CheckSetupUiState, actions: CheckSetupActions) {
    Scaffold(
        topBar = { NovaTopBar(stringResource(R.string.attention_check_setup), onBack = actions::back) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.finished) item(key = "summary") { Summary(state, actions) }
            items(CheckStep.entries, key = { it.name }) { step ->
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) { StepCard(step, state[step], actions) }
            }
        }
    }
}

@Composable
private fun Summary(state: CheckSetupUiState, actions: CheckSetupActions) {
    val clean = state.problems == 0
    val color = if (clean) stateColors.on else stateColors.attention
    NovaCard(Modifier.widthIn(max = 560.dp), color = color.container) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (clean) Icons.Filled.CheckCircle else Icons.Filled.Error, tint = color.accent, container = color.accent.copy(alpha = 0.14f))
            Spacer(Modifier.width(12.dp))
            Text(
                if (clean) stringResource(R.string.check_all_good) else pluralStringResource(R.plurals.check_problems, state.problems, state.problems),
                style = MaterialTheme.typography.titleMedium,
                color = color.onContainer,
                modifier = Modifier.weight(1f),
            )
        }
        if (!clean) {
            FilledTonalButton(onClick = actions::sendReport, modifier = Modifier.align(Alignment.End).padding(top = 8.dp).heightIn(min = 48.dp)) {
                Text(stringResource(R.string.check_send_report))
            }
        }
    }
}

@Composable
private fun StepCard(step: CheckStep, result: StepResult, actions: CheckSetupActions) {
    val text = step.text()
    NovaCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(text.icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(text.title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    stringResource(if (result == StepResult.FAIL || result == StepResult.WARNING) text.problem else text.description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ResultIcon(result)
        }
        StepButtons(step, result, actions)
    }
}

@Composable
private fun StepButtons(step: CheckStep, result: StepResult, actions: CheckSetupActions) {
    val automatic = step == CheckStep.ACCESS || step == CheckStep.BATTERY
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
        when {
            result == StepResult.FAIL || result == StepResult.WARNING -> {
                FilledTonalButton(onClick = { actions.fix(step) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(step.text().fix))
                }
            }
            !automatic && (result == StepResult.NOT_CHECKED || result == StepResult.SKIPPED) -> {
                if (step == CheckStep.CALLS) {
                    TextButton(onClick = { actions.skip(step) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.onboarding_skip)) }
                }
                Button(onClick = { actions.start(step) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(if (step == CheckStep.SOUND) R.string.check_test else R.string.check_start))
                }
            }
            result == StepResult.RUNNING && step == CheckStep.CALLS -> {
                TextButton(onClick = { actions.skip(step) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.onboarding_skip)) }
            }
        }
    }
}

@Composable
private fun ResultIcon(result: StepResult) {
    val description = stringResource(
        when (result) {
            StepResult.PASS -> R.string.check_result_pass
            StepResult.WARNING -> R.string.check_result_warning
            StepResult.FAIL -> R.string.check_result_fail
            StepResult.SKIPPED -> R.string.check_result_skipped
            StepResult.RUNNING -> R.string.check_result_running
            StepResult.NOT_CHECKED -> R.string.check_result_not_checked
        },
    )
    when (result) {
        StepResult.RUNNING -> CircularProgressIndicator(Modifier.padding(4.dp).width(24.dp), strokeWidth = 3.dp)
        else -> Icon(
            when (result) {
                StepResult.PASS -> Icons.Filled.CheckCircle
                StepResult.WARNING -> Icons.Filled.WarningAmber
                StepResult.FAIL -> Icons.Filled.Error
                StepResult.SKIPPED -> Icons.Outlined.RemoveCircleOutline
                else -> Icons.Outlined.RadioButtonUnchecked
            },
            contentDescription = description,
            tint = when (result) {
                StepResult.PASS -> stateColors.on.accent
                StepResult.WARNING -> stateColors.paused.accent
                StepResult.FAIL -> stateColors.attention.accent
                else -> MaterialTheme.colorScheme.outline
            },
        )
    }
}

private data class StepText(val icon: ImageVector, val title: Int, val description: Int, val problem: Int, val fix: Int)

private fun CheckStep.text(): StepText = when (this) {
    CheckStep.ACCESS -> StepText(Icons.Filled.Security, R.string.check_access_title, R.string.check_access_body, R.string.check_access_problem, R.string.attention_fix_access)
    CheckStep.BATTERY -> StepText(Icons.Filled.BatteryChargingFull, R.string.check_battery_title, R.string.check_battery_body, R.string.check_battery_problem, R.string.battery_card_fix)
    CheckStep.FLIP_SENSOR -> StepText(Icons.Filled.ScreenRotation, R.string.check_sensor_title, R.string.check_sensor_body, R.string.check_sensor_problem, R.string.try_again)
    CheckStep.SOUND -> StepText(Icons.Filled.VolumeOff, R.string.check_sound_title, R.string.check_sound_body, R.string.check_sound_problem, R.string.attention_check_sound_access)
    CheckStep.CALLS -> StepText(Icons.Filled.Call, R.string.check_calls_title, R.string.check_calls_body, R.string.check_calls_problem, R.string.try_again)
}

internal object PreviewCheckSetupActions : CheckSetupActions {
    override fun back() = Unit
    override fun start(step: CheckStep) = Unit
    override fun skip(step: CheckStep) = Unit
    override fun fix(step: CheckStep) = Unit
    override fun sendReport() = Unit
}
