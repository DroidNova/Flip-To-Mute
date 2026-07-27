package com.droidnova.fliptomute.sensor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeDeviceOrientationMonitor(
    override val isSensorAvailable: Boolean = true,
) : DeviceOrientationMonitor {
    private val mutableState = MutableStateFlow<FaceDownDetectionState>(
        if (isSensorAvailable) {
            FaceDownDetectionState.Idle(true, OrientationSensorSource.GRAVITY)
        } else {
            FaceDownDetectionState.SensorUnavailable
        },
    )
    override val state = mutableState.asStateFlow()
    var startCount = 0
    var stopCount = 0

    override fun start() {
        if (!isSensorAvailable || state.value is FaceDownDetectionState.Detecting) return
        startCount++
        emit(DeviceOrientation.UNKNOWN)
    }

    override fun stop() {
        stopCount++
        mutableState.value = if (isSensorAvailable) {
            FaceDownDetectionState.Idle(true, OrientationSensorSource.GRAVITY)
        } else {
            FaceDownDetectionState.SensorUnavailable
        }
    }

    fun emit(orientation: DeviceOrientation) {
        mutableState.value = FaceDownDetectionState.Detecting(
            orientation, OrientationSensorSource.GRAVITY, 0f, 0f, 9.81f,
        )
    }

    fun emitError() {
        mutableState.value = FaceDownDetectionState.Error
    }
}
