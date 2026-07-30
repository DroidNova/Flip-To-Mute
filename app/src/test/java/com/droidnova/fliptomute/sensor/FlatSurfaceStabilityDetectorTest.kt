package com.droidnova.fliptomute.sensor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlatSurfaceStabilityDetectorTest {
    private val detector = FlatSurfaceStabilityDetector()

    @Test fun nearlyHorizontalLowMovementBecomesStable() {
        assertFalse(sample(0f, 0f, 9.81f, 0L))
        assertFalse(sample(0.05f, 0f, 9.8f, 1L))
        assertTrue(sample(0.04f, 0.02f, 9.8f, 601L))
    }

    @Test fun tiltedPhoneDoesNotBecomeStable() {
        assertFalse(sample(6f, 0f, 7.5f, 0L))
        assertFalse(sample(6f, 0f, 7.5f, 1_000L))
    }

    @Test fun movementResetsStableCandidate() {
        sample(0f, 0f, -9.81f, 0L)
        sample(0f, 0f, -9.81f, 1L)
        assertFalse(sample(2f, 0f, -9.5f, 500L))
        assertFalse(sample(0f, 0f, -9.81f, 1_200L))
    }

    private fun sample(x: Float, y: Float, z: Float, millis: Long) =
        detector.processSample(x, y, z, millis * 1_000_000L)
}
