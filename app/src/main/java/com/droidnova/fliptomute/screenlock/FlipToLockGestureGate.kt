package com.droidnova.fliptomute.screenlock

import com.droidnova.fliptomute.sensor.DeviceOrientation

data class FlipToLockGestureConfiguration(
    val flatThreshold: Float = 0.88f,
    val faceUpStableDurationMillis: Long = 700L,
    val faceDownStableDurationMillis: Long = 700L,
    val maximumTransitionDurationMillis: Long = 2_500L,
    val minimumGravityMagnitude: Float = 7.0f,
    val maximumGravityMagnitude: Float = 12.5f,
)

sealed interface FlipToLockGestureResult {
    data object Waiting : FlipToLockGestureResult
    data object Armed : FlipToLockGestureResult
    data object LockRequested : FlipToLockGestureResult
}

class FlipToLockGestureGate(
    private val configuration: FlipToLockGestureConfiguration = FlipToLockGestureConfiguration(),
) {
    private enum class State { WAITING_FOR_FACE_UP, FACE_UP_STABILIZING, ARMED, TRANSITIONING, TRIGGERED }

    private var state = State.WAITING_FOR_FACE_UP
    private var faceUpSince = 0L
    private var faceDownSince = 0L
    private var transitionSince = 0L

    fun onSample(
        orientation: DeviceOrientation,
        normalizedZ: Float,
        gravityMagnitude: Float,
        timestampNanos: Long,
    ): FlipToLockGestureResult {
        if (state == State.TRIGGERED || timestampNanos <= 0L) return FlipToLockGestureResult.Waiting
        val valid = normalizedZ.isFinite() && gravityMagnitude.isFinite() &&
            gravityMagnitude in configuration.minimumGravityMagnitude..configuration.maximumGravityMagnitude
        val faceUpCandidate = valid && orientation != DeviceOrientation.MOVING &&
            orientation != DeviceOrientation.FACE_DOWN && normalizedZ >= configuration.flatThreshold
        val confirmedFaceUp = faceUpCandidate && orientation == DeviceOrientation.FACE_UP
        val faceDownCandidate = valid && orientation != DeviceOrientation.MOVING &&
            orientation != DeviceOrientation.FACE_UP && normalizedZ <= -configuration.flatThreshold
        val confirmedFaceDown = faceDownCandidate && orientation == DeviceOrientation.FACE_DOWN

        when (state) {
            State.WAITING_FOR_FACE_UP -> if (faceUpCandidate) {
                faceUpSince = timestampNanos
                state = State.FACE_UP_STABILIZING
            }
            State.FACE_UP_STABILIZING -> if (!faceUpCandidate) {
                faceUpSince = 0L
                state = State.WAITING_FOR_FACE_UP
            } else if (confirmedFaceUp &&
                elapsedMillis(faceUpSince, timestampNanos) >= configuration.faceUpStableDurationMillis
            ) {
                state = State.ARMED
                return FlipToLockGestureResult.Armed
            }
            State.ARMED -> if (!faceUpCandidate) {
                transitionSince = timestampNanos
                faceDownSince = if (faceDownCandidate) timestampNanos else 0L
                state = State.TRANSITIONING
            }
            State.TRANSITIONING -> {
                if (elapsedMillis(transitionSince, timestampNanos) > configuration.maximumTransitionDurationMillis) {
                    resetToFaceUpCandidate(faceUpCandidate, timestampNanos)
                } else if (faceDownCandidate) {
                    if (faceDownSince == 0L) faceDownSince = timestampNanos
                    if (confirmedFaceDown &&
                        elapsedMillis(faceDownSince, timestampNanos) >= configuration.faceDownStableDurationMillis
                    ) {
                        state = State.TRIGGERED
                        return FlipToLockGestureResult.LockRequested
                    }
                } else {
                    faceDownSince = 0L
                }
            }
            State.TRIGGERED -> Unit
        }
        return FlipToLockGestureResult.Waiting
    }

    fun reset() {
        state = State.WAITING_FOR_FACE_UP
        faceUpSince = 0L
        faceDownSince = 0L
        transitionSince = 0L
    }

    private fun resetToFaceUpCandidate(faceUp: Boolean, timestampNanos: Long) {
        faceDownSince = 0L
        transitionSince = 0L
        faceUpSince = if (faceUp) timestampNanos else 0L
        state = if (faceUp) State.FACE_UP_STABILIZING else State.WAITING_FOR_FACE_UP
    }

    private fun elapsedMillis(start: Long, now: Long): Long =
        if (start > 0L && now >= start) (now - start) / NANOS_PER_MILLISECOND else 0L

    private companion object { const val NANOS_PER_MILLISECOND = 1_000_000L }
}
