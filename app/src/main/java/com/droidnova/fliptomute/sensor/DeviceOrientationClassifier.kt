package com.droidnova.fliptomute.sensor

import kotlin.math.sqrt

class DeviceOrientationClassifier(
    private val configuration: FaceDownDetectionConfiguration = FaceDownDetectionConfiguration(),
) {
    private var faceDownCandidateTimestampNanos: Long? = null
    private var orientation = DeviceOrientation.UNKNOWN

    fun processSample(x: Float, y: Float, z: Float, timestampNanos: Long): DeviceOrientation {
        if (!x.isFinite() || !y.isFinite() || !z.isFinite() || timestampNanos < 0L) {
            resetCandidate()
            return DeviceOrientation.UNKNOWN.also { orientation = it }
        }
        val magnitude = sqrt(x * x + y * y + z * z)
        if (!magnitude.isFinite() || magnitude < MINIMUM_VALID_MAGNITUDE) {
            resetCandidate()
            return DeviceOrientation.UNKNOWN.also { orientation = it }
        }
        if (magnitude !in configuration.minimumGravityMagnitude..configuration.maximumGravityMagnitude) {
            resetCandidate()
            return DeviceOrientation.MOVING.also { orientation = it }
        }

        val normalizedZ = z / magnitude
        if (orientation == DeviceOrientation.FACE_DOWN && normalizedZ <= configuration.faceDownExitThreshold) {
            return DeviceOrientation.FACE_DOWN
        }
        if (normalizedZ <= configuration.faceDownEnterThreshold) {
            val startedAt = faceDownCandidateTimestampNanos
            if (startedAt == null || timestampNanos < startedAt) {
                faceDownCandidateTimestampNanos = timestampNanos
                return DeviceOrientation.MOVING.also { orientation = it }
            }
            val stableNanos = configuration.minimumStableDurationMillis * NANOS_PER_MILLISECOND
            return if (timestampNanos - startedAt >= stableNanos) {
                DeviceOrientation.FACE_DOWN.also { orientation = it }
            } else {
                DeviceOrientation.MOVING.also { orientation = it }
            }
        }

        resetCandidate()
        return if (normalizedZ >= configuration.faceUpThreshold) {
            DeviceOrientation.FACE_UP.also { orientation = it }
        } else {
            DeviceOrientation.MOVING.also { orientation = it }
        }
    }

    fun reset() {
        orientation = DeviceOrientation.UNKNOWN
        resetCandidate()
    }

    private fun resetCandidate() {
        faceDownCandidateTimestampNanos = null
    }

    private companion object {
        const val MINIMUM_VALID_MAGNITUDE = 0.001f
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
