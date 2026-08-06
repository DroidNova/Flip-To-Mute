package com.droidnova.fliptomute.sensor

data class FlatSurfaceFlipConfiguration(
    val flatOrientationThreshold: Float = 0.85f,
    val faceUpStableDurationMillis: Long = 500L,
    val faceDownStableDurationMillis: Long = 400L,
    val maximumFlipTransitionDurationMillis: Long = 3_000L,
    val minimumStableGravityMagnitude: Float = 7.0f,
    val maximumStableGravityMagnitude: Float = 12.5f,
)

sealed interface FlatSurfaceFlipResult {
    data object Waiting : FlatSurfaceFlipResult
    data object Armed : FlatSurfaceFlipResult
    data object Allowed : FlatSurfaceFlipResult
}

/** Pure, timestamp-driven gate for a stable flat face-up to flat face-down gesture. */
class FlatSurfaceFlipGate(
    private val configuration: FlatSurfaceFlipConfiguration = FlatSurfaceFlipConfiguration(),
) {
    private enum class State { INITIAL_OBSERVATION, WAITING_FOR_FACE_UP, FACE_UP_STABILIZING, ARMED, FLIPPING, TRIGGERED }

    private var state = State.INITIAL_OBSERVATION
    private var faceUpSince = 0L
    private var faceDownSince = 0L
    private var transitionSince = 0L

    fun onSample(
        orientation: DeviceOrientation,
        normalizedZ: Float,
        gravityMagnitude: Float,
        timestampNanos: Long,
    ): FlatSurfaceFlipResult {
        if (state == State.TRIGGERED || timestampNanos <= 0L) return FlatSurfaceFlipResult.Waiting
        val valid = normalizedZ.isFinite() && gravityMagnitude.isFinite() &&
            gravityMagnitude in configuration.minimumStableGravityMagnitude..configuration.maximumStableGravityMagnitude
        val faceUp = valid && orientation == DeviceOrientation.FACE_UP &&
            normalizedZ >= configuration.flatOrientationThreshold
        // FACE_DOWN itself is emitted only after the existing classifier's stability interval.
        // Start timing from the filtered flat candidate so the two confirmations overlap.
        val flatFaceDownCandidate = valid && normalizedZ <= -configuration.flatOrientationThreshold
        val confirmedFaceDown = flatFaceDownCandidate && orientation == DeviceOrientation.FACE_DOWN

        if (state == State.INITIAL_OBSERVATION) {
            if (flatFaceDownCandidate) {
                if (faceDownSince == 0L) faceDownSince = timestampNanos
                if (confirmedFaceDown &&
                    elapsedMillis(faceDownSince, timestampNanos) >= configuration.faceDownStableDurationMillis
                ) {
                    state = State.TRIGGERED
                    return FlatSurfaceFlipResult.Allowed
                }
                return FlatSurfaceFlipResult.Waiting
            }
            // The shortcut applies only to an uninterrupted initial face-down observation.
            faceDownSince = 0L
            state = State.WAITING_FOR_FACE_UP
        }

        when (state) {
            State.WAITING_FOR_FACE_UP -> if (faceUp) {
                faceUpSince = timestampNanos
                state = State.FACE_UP_STABILIZING
            }
            State.FACE_UP_STABILIZING -> if (!faceUp) {
                faceUpSince = 0L
                state = State.WAITING_FOR_FACE_UP
            } else if (elapsedMillis(faceUpSince, timestampNanos) >= configuration.faceUpStableDurationMillis) {
                state = State.ARMED
                return FlatSurfaceFlipResult.Armed
            }
            State.ARMED -> if (!faceUp) {
                transitionSince = timestampNanos
                faceDownSince = if (flatFaceDownCandidate) timestampNanos else 0L
                state = State.FLIPPING
            }
            State.FLIPPING -> {
                if (elapsedMillis(transitionSince, timestampNanos) > configuration.maximumFlipTransitionDurationMillis) {
                    resetForFaceUp(faceUp, timestampNanos)
                } else if (flatFaceDownCandidate) {
                    if (faceDownSince == 0L) faceDownSince = timestampNanos
                    if (confirmedFaceDown &&
                        elapsedMillis(faceDownSince, timestampNanos) >= configuration.faceDownStableDurationMillis
                    ) {
                        state = State.TRIGGERED
                        return FlatSurfaceFlipResult.Allowed
                    }
                } else {
                    faceDownSince = 0L
                }
            }
            State.INITIAL_OBSERVATION, State.TRIGGERED -> Unit
        }
        return FlatSurfaceFlipResult.Waiting
    }

    fun reset() {
        state = State.INITIAL_OBSERVATION
        faceUpSince = 0L
        faceDownSince = 0L
        transitionSince = 0L
    }

    private fun resetForFaceUp(faceUp: Boolean, timestampNanos: Long) {
        faceDownSince = 0L
        transitionSince = 0L
        faceUpSince = if (faceUp) timestampNanos else 0L
        state = if (faceUp) State.FACE_UP_STABILIZING else State.WAITING_FOR_FACE_UP
    }

    private fun elapsedMillis(start: Long, now: Long): Long =
        if (now >= start) (now - start) / NANOS_PER_MILLISECOND else 0L

    private companion object { const val NANOS_PER_MILLISECOND = 1_000_000L }
}
