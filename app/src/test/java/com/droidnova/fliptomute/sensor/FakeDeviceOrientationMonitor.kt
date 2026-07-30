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

    fun emit(
        orientation: DeviceOrientation,
        isFlatAndStable: Boolean = false,
        isNearlyHorizontal: Boolean = isFlatAndStable,
    ) {
        val z = if (orientation == DeviceOrientation.FACE_DOWN) -9.81f else 9.81f
        mutableState.value = FaceDownDetectionState.Detecting(
            orientation,
            OrientationSensorSource.GRAVITY,
            0f,
            0f,
            z,
            isFlatAndStable,
            isNearlyHorizontal,
        )
    }

    fun emitError() {
        mutableState.value = FaceDownDetectionState.Error
    }
}
