package com.droidnova.fliptomute.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

class PocketProtectionGateTest {
    private val gate = PocketProtectionGate()

    @Test fun initialFarLocksClear() {
        assertEquals(PocketProtectionDecision.CLEAR, gate.onProximityChanged(ProximityState.FAR, 1))
        assertEquals(PocketProtectionDecision.CLEAR, gate.onProximityChanged(ProximityState.NEAR, 2))
    }

    @Test fun nearStableFlatFaceDownClears() {
        gate.onProximityChanged(ProximityState.NEAR, ms(1))
        assertEquals(PocketProtectionDecision.WAITING, sample(DeviceOrientation.MOVING, -1f, 1))
        assertEquals(PocketProtectionDecision.CLEAR, sample(DeviceOrientation.FACE_DOWN, -.99f, 401))
    }

    @Test fun nearVerticalBlocksAndLocks() {
        gate.onProximityChanged(ProximityState.NEAR, ms(1))
        assertEquals(PocketProtectionDecision.BLOCKED, sample(DeviceOrientation.MOVING, 0f, 2))
        assertEquals(PocketProtectionDecision.BLOCKED, gate.onInitialTimeout())
    }

    @Test fun unavailableAndTimeoutFallBackToClear() {
        assertEquals(PocketProtectionDecision.CLEAR, gate.onProximityChanged(ProximityState.UNAVAILABLE, 1))
        gate.reset()
        assertEquals(PocketProtectionDecision.CLEAR, gate.onInitialTimeout())
        assertEquals(PocketProtectionDecision.CLEAR, gate.onProximityChanged(ProximityState.NEAR, 2))
    }

    @Test fun resetStartsIndependentDecision() {
        gate.onProximityChanged(ProximityState.NEAR, 1)
        sample(DeviceOrientation.FACE_UP, 1f, 2)
        gate.reset()
        assertEquals(PocketProtectionDecision.CLEAR, gate.onProximityChanged(ProximityState.FAR, 3))
    }

    private fun sample(orientation: DeviceOrientation, z: Float, millis: Long) =
        gate.onOrientationSample(orientation, z, 9.81f, ms(millis))

    private fun ms(value: Long) = value * 1_000_000L
}
