package com.droidnova.fliptomute.sensor

data class PocketProtectionConfiguration(
    val initialDecisionTimeoutMillis: Long = 600L,
    val flatFaceDownThreshold: Float = -0.85f,
    val minimumStableGravityMagnitude: Float = 7f,
    val maximumStableGravityMagnitude: Float = 12.5f,
    val flatFaceDownStableDurationMillis: Long = 400L,
)

enum class PocketProtectionDecision { WAITING, CLEAR, BLOCKED }

class PocketProtectionGate(
    private val configuration: PocketProtectionConfiguration = PocketProtectionConfiguration(),
) {
    private var decision = PocketProtectionDecision.WAITING
    private var initialProximity = ProximityState.UNKNOWN
    private var flatFaceDownSince = 0L

    fun onProximityChanged(state: ProximityState, timestampNanos: Long): PocketProtectionDecision {
        if (decision != PocketProtectionDecision.WAITING) return decision
        if (initialProximity != ProximityState.UNKNOWN) return decision
        when (state) {
            ProximityState.FAR, ProximityState.UNAVAILABLE -> decision = PocketProtectionDecision.CLEAR
            ProximityState.NEAR -> initialProximity = state
            ProximityState.UNKNOWN -> Unit
        }
        return decision
    }

    fun onOrientationSample(
        orientation: DeviceOrientation,
        normalizedZ: Float,
        gravityMagnitude: Float,
        timestampNanos: Long,
    ): PocketProtectionDecision {
        if (decision != PocketProtectionDecision.WAITING || initialProximity != ProximityState.NEAR) return decision
        val valid = timestampNanos > 0L && normalizedZ.isFinite() && gravityMagnitude.isFinite() &&
            gravityMagnitude in configuration.minimumStableGravityMagnitude..configuration.maximumStableGravityMagnitude
        val flatCandidate = valid && normalizedZ <= configuration.flatFaceDownThreshold
        if (!flatCandidate) {
            decision = PocketProtectionDecision.BLOCKED
        } else {
            if (flatFaceDownSince == 0L) flatFaceDownSince = timestampNanos
            if (orientation == DeviceOrientation.FACE_DOWN &&
                (timestampNanos - flatFaceDownSince) / 1_000_000L >= configuration.flatFaceDownStableDurationMillis
            ) decision = PocketProtectionDecision.CLEAR
        }
        return decision
    }

    fun onInitialTimeout(): PocketProtectionDecision {
        if (decision == PocketProtectionDecision.WAITING) decision = PocketProtectionDecision.CLEAR
        return decision
    }

    fun reset() {
        decision = PocketProtectionDecision.WAITING
        initialProximity = ProximityState.UNKNOWN
        flatFaceDownSince = 0L
    }
}
