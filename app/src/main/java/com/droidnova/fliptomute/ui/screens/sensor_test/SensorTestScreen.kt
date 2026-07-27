package com.droidnova.fliptomute.ui.screens.sensor_test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.ui.components.AppTopBar
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme

enum class OrientationState { FACE_UP, MOVING, FACE_DOWN }

data class SensorTestUiState(
    val status: SensorTestStatus = SensorTestStatus.WAITING,
    val availableStates: List<OrientationState> = OrientationState.entries,
)

enum class SensorTestStatus { WAITING, FACE_UP, MOVING, FACE_DOWN }

@Composable
fun SensorTestScreen(onBack: () -> Unit, state: SensorTestUiState = SensorTestUiState()) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(stringResource(R.string.test_flip_title), onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = null)
                        Text(statusText(state.status), style = MaterialTheme.typography.headlineSmall)
                        Text(stringResource(R.string.sensor_instruction))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.availableStates.forEach { orientation ->
                        FilterChip(
                            selected = state.status.matches(orientation),
                            onClick = {},
                            enabled = false,
                            label = { Text(orientationText(orientation)) },
                        )
                    }
                }
            }
            item {
                Button(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) {
                    Text(stringResource(R.string.start_test))
                }
            }
        }
    }
}

@Composable
private fun statusText(status: SensorTestStatus) = stringResource(
    when (status) {
        SensorTestStatus.WAITING -> R.string.waiting_for_sensor
        SensorTestStatus.FACE_UP -> R.string.face_up
        SensorTestStatus.MOVING -> R.string.moving
        SensorTestStatus.FACE_DOWN -> R.string.face_down
    },
)

@Composable
private fun orientationText(state: OrientationState) = stringResource(
    when (state) {
        OrientationState.FACE_UP -> R.string.face_up
        OrientationState.MOVING -> R.string.moving
        OrientationState.FACE_DOWN -> R.string.face_down
    },
)

private fun SensorTestStatus.matches(orientation: OrientationState) = when (this) {
    SensorTestStatus.WAITING -> false
    SensorTestStatus.FACE_UP -> orientation == OrientationState.FACE_UP
    SensorTestStatus.MOVING -> orientation == OrientationState.MOVING
    SensorTestStatus.FACE_DOWN -> orientation == OrientationState.FACE_DOWN
}

@Preview(showBackground = true)
@Composable
private fun SensorTestScreenPreview() {
    FlipToMuteTheme(dynamicColor = false) { SensorTestScreen({}) }
}
