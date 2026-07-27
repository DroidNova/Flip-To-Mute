package com.droidnova.fliptomute.ui.screens.sensor_test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.DeviceOrientationMonitor
import com.droidnova.fliptomute.sensor.FaceDownDetectionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SensorTestViewModel(
    private val orientationMonitor: DeviceOrientationMonitor,
) : ViewModel() {
    val uiState: StateFlow<SensorTestUiState> = orientationMonitor.state
        .map(::mapState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = mapState(orientationMonitor.state.value),
        )

    fun startTest() {
        if (orientationMonitor.isSensorAvailable) orientationMonitor.start()
    }

    fun stopTest() = orientationMonitor.stop()

    override fun onCleared() {
        orientationMonitor.stop()
        super.onCleared()
    }

    private fun mapState(state: FaceDownDetectionState): SensorTestUiState = when (state) {
        is FaceDownDetectionState.Idle -> SensorTestUiState(
            isSensorAvailable = state.isSensorAvailable,
            sensorSource = state.sensorSource,
        )
        is FaceDownDetectionState.Detecting -> SensorTestUiState(
            isSensorAvailable = true,
            isTesting = true,
            orientation = state.orientation,
            sensorSource = state.sensorSource,
            gravityX = state.gravityX,
            gravityY = state.gravityY,
            gravityZ = state.gravityZ,
        )
        FaceDownDetectionState.SensorUnavailable -> SensorTestUiState(isSensorAvailable = false)
        FaceDownDetectionState.Error -> SensorTestUiState(
            isSensorAvailable = orientationMonitor.isSensorAvailable,
            orientation = DeviceOrientation.UNKNOWN,
            hasError = true,
        )
    }
}
