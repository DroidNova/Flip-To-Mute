package com.droidnova.fliptomute.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceOrientationClassifierTest {
    private val classifier = DeviceOrientationClassifier()

    @Test fun zeroMagnitudeIsUnknown() = assertEquals(DeviceOrientation.UNKNOWN, sample(0f, 0f, 0f))
    @Test fun nanIsUnknown() = assertEquals(DeviceOrientation.UNKNOWN, sample(Float.NaN, 0f, 9.81f))
    @Test fun infinityIsUnknown() = assertEquals(DeviceOrientation.UNKNOWN, sample(0f, 0f, Float.POSITIVE_INFINITY))
    @Test fun faceUpIsRecognized() = assertEquals(DeviceOrientation.FACE_UP, sample(0f, 0f, 9.81f))
    @Test fun faceUpAllowsMinorNoise() = assertEquals(DeviceOrientation.FACE_UP, sample(0.5f, -0.4f, 9.7f))
    @Test fun strongTiltIsMoving() = assertEquals(DeviceOrientation.MOVING, sample(8f, 0f, 5f))
    @Test fun highMagnitudeIsMoving() = assertEquals(DeviceOrientation.MOVING, sample(0f, 0f, 14f))
    @Test fun sideOrientationIsMoving() = assertEquals(DeviceOrientation.MOVING, sample(9.81f, 0f, 0f))

    @Test
    fun faceDownRequiresStableDuration() {
        assertEquals(DeviceOrientation.MOVING, sample(0f, 0f, -9.81f, 0L))
        assertEquals(DeviceOrientation.MOVING, sample(0f, 0f, -9.81f, 399L))
        assertEquals(DeviceOrientation.FACE_DOWN, sample(0f, 0f, -9.81f, 400L))
    }

    @Test
    fun leavingCandidateResetsConfirmationAndReentryStartsAgain() {
        sample(0f, 0f, -9.81f, 0L)
        assertEquals(DeviceOrientation.MOVING, sample(9.81f, 0f, 0f, 300L))
        assertEquals(DeviceOrientation.MOVING, sample(0f, 0f, -9.81f, 500L))
        assertEquals(DeviceOrientation.MOVING, sample(0f, 0f, -9.81f, 899L))
        assertEquals(DeviceOrientation.FACE_DOWN, sample(0f, 0f, -9.81f, 900L))
    }

    @Test
    fun confirmedFaceDownUsesExitHysteresis() {
        sample(0f, 0f, -9.81f, 0L)
        sample(0f, 0f, -9.81f, 400L)
        assertEquals(DeviceOrientation.FACE_DOWN, sample(7f, 0f, -6f, 450L))
        assertEquals(DeviceOrientation.MOVING, sample(8f, 0f, -4f, 500L))
    }

    private fun sample(x: Float, y: Float, z: Float, millis: Long = 0L) =
        classifier.processSample(x, y, z, millis * 1_000_000L)
}
