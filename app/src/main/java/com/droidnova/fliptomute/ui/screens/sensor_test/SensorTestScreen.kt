package com.droidnova.fliptomute.ui.screens.sensor_test

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.OrientationSensorSource
import com.droidnova.fliptomute.ui.components.AppTopBar
import com.droidnova.fliptomute.ui.components.SectionHeader
import com.droidnova.fliptomute.ui.theme.FlipToMuteTheme

@Composable
fun SensorTestScreen(
    onBack: () -> Unit,
    viewModelFactory: ViewModelProvider.Factory,
) {
    val viewModel: SensorTestViewModel = viewModel(factory = viewModelFactory)
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    StopTestOnLifecycleStop(viewModel::stopTest)
    SensorTestContent(
        state = state,
        onBack = onBack,
        onStart = viewModel::startTest,
        onStop = viewModel::stopTest,
    )
}

@Composable
private fun StopTestOnLifecycleStop(stopTest: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentStopTest = rememberUpdatedState(stopTest)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) currentStopTest.value()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            currentStopTest.value()
        }
    }
}

@Composable
private fun SensorTestContent(
    state: SensorTestUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { AppTopBar(stringResource(R.string.test_flip_title), onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { OrientationStatusCard(state) }
            if (state.isSensorAvailable) {
                item {
                    Button(
                        onClick = if (state.isTesting) onStop else onStart,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(if (state.isTesting) R.string.stop_test else R.string.start_test))
                    }
                }
            }
            item { TestInstructions() }
            if (state.sensorSource != null) {
                item { TechnicalValues(state) }
            }
        }
    }
}

@Composable
private fun OrientationStatusCard(state: SensorTestUiState) {
    val presentation = statusPresentation(state)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = presentation.icon,
                contentDescription = null,
                tint = if (state.orientation == DeviceOrientation.FACE_DOWN) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(stringResource(presentation.title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(presentation.description), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private data class StatusPresentation(val title: Int, val description: Int, val icon: ImageVector)

private fun statusPresentation(state: SensorTestUiState): StatusPresentation = when {
    !state.isSensorAvailable -> StatusPresentation(
        R.string.sensor_unavailable,
        R.string.sensor_unavailable_description,
        Icons.Default.Warning,
    )
    state.hasError -> StatusPresentation(
        R.string.sensor_error,
        R.string.sensor_error_description,
        Icons.Default.Warning,
    )
    !state.isTesting -> StatusPresentation(
        R.string.ready_to_test,
        R.string.ready_to_test_description,
        Icons.Default.ScreenRotation,
    )
    state.orientation == DeviceOrientation.FACE_UP -> StatusPresentation(
        R.string.face_up,
        R.string.face_up_description,
        Icons.Default.ScreenRotation,
    )
    state.orientation == DeviceOrientation.MOVING -> StatusPresentation(
        R.string.moving,
        R.string.moving_description,
        Icons.Default.ScreenRotation,
    )
    state.orientation == DeviceOrientation.FACE_DOWN -> StatusPresentation(
        R.string.face_down_detected,
        R.string.face_down_detected_description,
        Icons.Default.CheckCircle,
    )
    else -> StatusPresentation(
        R.string.waiting_for_sensor,
        R.string.waiting_for_sensor_description,
        Icons.Default.ScreenRotation,
    )
}

@Composable
private fun TestInstructions() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(R.string.test_instructions))
        Text(stringResource(R.string.sensor_test_step_one))
        Text(stringResource(R.string.sensor_test_step_two))
        Text(stringResource(R.string.sensor_test_step_three))
        Text(stringResource(R.string.sensor_test_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TechnicalValues(state: SensorTestUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(
                R.string.sensor_source_value,
                stringResource(
                    if (state.sensorSource == OrientationSensorSource.GRAVITY) {
                        R.string.gravity_sensor
                    } else {
                        R.string.accelerometer_fallback
                    },
                ),
            ),
        )
        if (state.gravityX != null && state.gravityY != null && state.gravityZ != null) {
            Text(stringResource(R.string.gravity_values, state.gravityX, state.gravityY, state.gravityZ))
        }
    }
}

@Preview(showBackground = true) @Composable private fun IdlePreview() = PreviewContent(SensorTestUiState())
@Preview(showBackground = true) @Composable private fun FaceUpPreview() =
    PreviewContent(testingState(DeviceOrientation.FACE_UP))
@Preview(showBackground = true) @Composable private fun MovingPreview() =
    PreviewContent(testingState(DeviceOrientation.MOVING))
@Preview(showBackground = true) @Composable private fun FaceDownPreview() =
    PreviewContent(testingState(DeviceOrientation.FACE_DOWN))
@Preview(showBackground = true) @Composable private fun UnavailablePreview() =
    PreviewContent(SensorTestUiState(isSensorAvailable = false))

@Composable
private fun PreviewContent(state: SensorTestUiState) {
    FlipToMuteTheme(dynamicColor = false) { SensorTestContent(state, {}, {}, {}) }
}

private fun testingState(orientation: DeviceOrientation) = SensorTestUiState(
    isTesting = true,
    orientation = orientation,
    sensorSource = OrientationSensorSource.GRAVITY,
    gravityX = 0f,
    gravityY = 0f,
    gravityZ = if (orientation == DeviceOrientation.FACE_DOWN) -9.81f else 9.81f,
)
