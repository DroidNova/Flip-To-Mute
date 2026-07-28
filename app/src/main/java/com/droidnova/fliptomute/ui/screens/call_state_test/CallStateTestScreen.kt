package com.droidnova.fliptomute.ui.screens.call_state_test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.telephony.CellularCallMonitorError
import com.droidnova.fliptomute.telephony.CellularCallState
import com.droidnova.fliptomute.ui.components.AppTopBar
import com.droidnova.fliptomute.ui.components.SectionHeader
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme
import com.droidnova.fliptomute.ui.util.RefreshOnResume

@Composable
fun CallStateTestScreen(
    onBack: () -> Unit,
    onOpenSetup: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val viewModel: CallStateTestViewModel = viewModel(factory = viewModelFactory)
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    RefreshOnResume(viewModel::refreshAccessState)
    DisposableEffect(viewModel) { onDispose { viewModel.stopListening() } }
    CallStateTestContent(
        state = state,
        onBack = onBack,
        onOpenSetup = onOpenSetup,
        onStart = viewModel::startListening,
        onStop = viewModel::stopListening,
    )
}

@Composable
private fun CallStateTestContent(
    state: CallStateTestUiState,
    onBack: () -> Unit,
    onOpenSetup: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(stringResource(R.string.test_call_detection), onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { CallStatusCard(state) }
            item {
                when {
                    !state.isTelephonyAvailable -> Unit
                    !state.hasPhonePermission -> FullWidthButton(R.string.open_app_setup, onOpenSetup)
                    state.isListening -> FullWidthButton(R.string.stop_listening, onStop)
                    state.error != null -> FullWidthButton(R.string.try_again, onStart)
                    else -> FullWidthButton(R.string.start_listening, onStart)
                }
            }
            if (state.isListening) {
                item {
                    Text(
                        pluralStringResource(
                            R.plurals.monitored_subscriptions,
                            state.monitoredSubscriptionCount,
                            state.monitoredSubscriptionCount,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (state.recentTransitions.isNotEmpty()) {
                item { RecentTransitions(state.recentTransitions) }
            }
            item { CallTestInstructions() }
        }
    }
}

@Composable
private fun CallStatusCard(state: CallStateTestUiState) {
    val presentation = callPresentation(state)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(presentation.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(presentation.title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(presentation.description), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private data class CallPresentation(val title: Int, val description: Int, val icon: ImageVector)

private fun callPresentation(state: CallStateTestUiState): CallPresentation = when {
    !state.isTelephonyAvailable -> CallPresentation(
        R.string.cellular_unavailable, R.string.cellular_unavailable_description, Icons.Default.Warning,
    )
    !state.hasPhonePermission -> CallPresentation(
        R.string.phone_access_required, R.string.phone_access_required_description, Icons.Default.Warning,
    )
    state.error != null -> CallPresentation(
        R.string.call_test_error, errorDescription(state.error), Icons.Default.Warning,
    )
    !state.isListening -> CallPresentation(
        R.string.ready_to_test, R.string.call_test_ready_description, Icons.Default.Call,
    )
    state.currentCallState == CellularCallState.RINGING -> CallPresentation(
        R.string.incoming_call_detected, R.string.ringing_description, Icons.Default.PhoneInTalk,
    )
    state.currentCallState == CellularCallState.ACTIVE -> CallPresentation(
        R.string.call_active, R.string.call_active_description, Icons.Default.PhoneInTalk,
    )
    else -> CallPresentation(
        R.string.no_active_call, R.string.call_test_listening_description, Icons.Default.Call,
    )
}

private fun errorDescription(error: CellularCallMonitorError): Int = when (error) {
    CellularCallMonitorError.PERMISSION_REVOKED -> R.string.phone_access_required_description
    CellularCallMonitorError.REGISTRATION_FAILED,
    CellularCallMonitorError.TELEPHONY_SERVICE_UNAVAILABLE,
    CellularCallMonitorError.UNKNOWN,
    -> R.string.call_test_error_description
}

@Composable
private fun FullWidthButton(text: Int, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(stringResource(text)) }
}

@Composable
private fun RecentTransitions(transitions: List<CellularCallState>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader(stringResource(R.string.recent_transitions))
        transitions.forEachIndexed { index, state ->
            val priorCallObserved = transitions.drop(index + 1).any {
                it == CellularCallState.RINGING || it == CellularCallState.ACTIVE
            }
            Text(
                stringResource(
                    when (state) {
                        CellularCallState.RINGING -> R.string.incoming_call_detected
                        CellularCallState.ACTIVE -> R.string.call_active
                        CellularCallState.IDLE -> if (priorCallObserved) R.string.call_ended else R.string.no_active_call
                        CellularCallState.UNKNOWN -> R.string.no_active_call
                    },
                ),
            )
        }
    }
}

@Composable
private fun CallTestInstructions() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader(stringResource(R.string.call_test_instructions))
        listOf(
            R.string.call_test_step_one,
            R.string.call_test_step_two,
            R.string.call_test_step_three,
            R.string.call_test_step_four,
            R.string.call_test_step_five,
        ).forEach { Text(stringResource(it)) }
        Text(stringResource(R.string.cellular_calls_only_note), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.listening_stops_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true) @Composable private fun ReadyPreview() =
    PreviewCall(CallStateTestUiState(hasPhonePermission = true))
@Preview(showBackground = true) @Composable private fun PermissionPreview() = PreviewCall(CallStateTestUiState())
@Preview(showBackground = true) @Composable private fun UnavailablePreview() =
    PreviewCall(CallStateTestUiState(isTelephonyAvailable = false, hasPhonePermission = true))
@Preview(showBackground = true) @Composable private fun RingingPreview() =
    PreviewCall(listeningState(CellularCallState.RINGING))
@Preview(showBackground = true) @Composable private fun ListeningIdlePreview() = PreviewCall(
    listeningState(CellularCallState.IDLE).copy(
        recentTransitions = listOf(CellularCallState.IDLE, CellularCallState.RINGING),
    ),
)
@Preview(showBackground = true) @Composable private fun ActivePreview() =
    PreviewCall(listeningState(CellularCallState.ACTIVE))
@Preview(showBackground = true) @Composable private fun ErrorPreview() = PreviewCall(
    CallStateTestUiState(hasPhonePermission = true, error = CellularCallMonitorError.REGISTRATION_FAILED),
)

@Composable private fun PreviewCall(state: CallStateTestUiState) {
    FlipToMuteTheme(dynamicColor = false) { CallStateTestContent(state, {}, {}, {}, {}) }
}

private fun listeningState(state: CellularCallState) = CallStateTestUiState(
    isListening = true,
    hasPhonePermission = true,
    currentCallState = state,
    monitoredSubscriptionCount = 2,
    recentTransitions = listOf(state, CellularCallState.IDLE),
)
