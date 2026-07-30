package com.droidnova.fliptomute.sensor

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects a nearly horizontal, low-movement posture. Sensors cannot prove physical surface contact.
 */
class FlatSurfaceStabilityDetector(
    private val configuration: FaceDownDetectionConfiguration = FaceDownDetectionConfiguration(),
) {
    private var candidateStartNanos: Long? = null
    private var previous: Vector? = null

    fun processSample(x: Float, y: Float, z: Float, timestampNanos: Long): Boolean {
        val magnitude = sqrt(x * x + y * y + z * z)
        val current = Vector(x, y, z)
        val prior = previous
        previous = current
        if (!magnitude.isFinite() || magnitude !in
            configuration.minimumGravityMagnitude..configuration.maximumGravityMagnitude ||
            abs(z / magnitude) < configuration.flatOrientationThreshold || prior == null ||
            current.distanceFrom(prior) > configuration.maximumStableVectorDelta
        ) {
            candidateStartNanos = null
            return false
        }
        val startedAt = candidateStartNanos
        if (startedAt == null || timestampNanos < startedAt) {
            candidateStartNanos = timestampNanos
            return false
        }
        return timestampNanos - startedAt >=
            configuration.minimumFlatStableDurationMillis * NANOS_PER_MILLISECOND
    }

    fun reset() {
        candidateStartNanos = null
        previous = null
    }

    private data class Vector(val x: Float, val y: Float, val z: Float) {
        fun distanceFrom(other: Vector): Float = sqrt(
            (x - other.x) * (x - other.x) +
                (y - other.y) * (y - other.y) +
                (z - other.z) * (z - other.z),
        )
    }

    private companion object { const val NANOS_PER_MILLISECOND = 1_000_000L }
}
