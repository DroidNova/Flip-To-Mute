package com.droidnova.fliptomute.sensor

import kotlin.math.max

class ProximityValueClassifier {
    fun classify(value: Float, maximumRange: Float): ProximityState {
        if (!value.isFinite() || value < 0f || !maximumRange.isFinite() || maximumRange <= 0f) {
            return ProximityState.UNKNOWN
        }
        // Allow normal sensor quantization close to the advertised maximum range.
        val farBoundary = maximumRange - max(maximumRange * 0.01f, 0.001f)
        return if (value >= farBoundary) ProximityState.FAR else ProximityState.NEAR
    }
}
