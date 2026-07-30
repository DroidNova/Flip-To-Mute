package com.droidnova.fliptomute.sensor

enum class DeviceOrientation { UNKNOWN, FACE_UP, MOVING, FACE_DOWN }

enum class OrientationSensorSource { GRAVITY, ACCELEROMETER }

data class FaceDownDetectionConfiguration(
    val faceDownEnterThreshold: Float = -0.75f,
    val faceDownExitThreshold: Float = -0.55f,
    val faceUpThreshold: Float = 0.75f,
    val minimumStableDurationMillis: Long = 400L,
    val minimumGravityMagnitude: Float = 7f,
    val maximumGravityMagnitude: Float = 12.5f,
    val accelerometerFilterAlpha: Float = 0.8f,
    val flatOrientationThreshold: Float = 0.9f,
    val maximumStableVectorDelta: Float = 0.35f,
    val minimumFlatStableDurationMillis: Long = 600L,
)

sealed interface FaceDownDetectionState {
    data class Idle(
        val isSensorAvailable: Boolean,
        val sensorSource: OrientationSensorSource?,
    ) : FaceDownDetectionState

    data class Detecting(
        val orientation: DeviceOrientation,
        val sensorSource: OrientationSensorSource,
        val gravityX: Float,
        val gravityY: Float,
        val gravityZ: Float,
        val isFlatAndStable: Boolean = false,
        val isNearlyHorizontal: Boolean = false,
    ) : FaceDownDetectionState

    data object SensorUnavailable : FaceDownDetectionState
    data object Error : FaceDownDetectionState
}
