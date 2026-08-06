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
    private var timestampNanos = 1_000_000_000L

    override fun start() {
        if (!isSensorAvailable || state.value is FaceDownDetectionState.Detecting) return
        startCount++
        mutableState.value = FaceDownDetectionState.Detecting(
            DeviceOrientation.UNKNOWN, OrientationSensorSource.GRAVITY, 0f, 0f, 0f,
        )
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
        normalizedZ: Float = when (orientation) {
            DeviceOrientation.FACE_UP -> 1f
            DeviceOrientation.FACE_DOWN -> -1f
            else -> 0f
        },
        gravityMagnitude: Float = 9.81f,
        timestampNanos: Long = this.timestampNanos.also { this.timestampNanos += 100_000_000L },
    ) {
        mutableState.value = FaceDownDetectionState.Detecting(
            orientation, OrientationSensorSource.GRAVITY, 0f, 0f, 9.81f,
            normalizedZ, gravityMagnitude, timestampNanos,
        )
    }

    fun emitError() {
        mutableState.value = FaceDownDetectionState.Error
    }
}
