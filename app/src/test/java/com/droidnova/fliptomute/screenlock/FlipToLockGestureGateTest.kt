package com.droidnova.fliptomute.screenlock

import com.droidnova.fliptomute.sensor.DeviceOrientation
import org.junit.Assert.assertEquals
import org.junit.Test

class FlipToLockGestureGateTest {
    private val gate = FlipToLockGestureGate()

    @Test fun oneFaceUpSampleDoesNotArm() {
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 0L))
    }

    @Test fun stableFaceUpArms() {
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 0L))
        assertEquals(FlipToLockGestureResult.Armed, sample(DeviceOrientation.FACE_UP, 1f, 700L))
    }

    @Test fun stableFaceUpThenStableFaceDownRequestsLock() {
        arm()
        assertWaiting(sample(DeviceOrientation.MOVING, 0f, 800L))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 900L))
        assertEquals(FlipToLockGestureResult.LockRequested, sample(DeviceOrientation.FACE_DOWN, -1f, 1_600L))
    }

    @Test fun oneFaceDownSampleDoesNotLock() {
        arm()
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 800L))
    }

    @Test fun startingAndRemainingFaceDownDoesNotLock() {
        repeat(20) { index -> assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, index * 200L)) }
    }

    @Test fun verticalTiltedAndMovingToFaceDownDoNotLock() {
        listOf(
            DeviceOrientation.UNKNOWN to 0f,
            DeviceOrientation.UNKNOWN to 0.7f,
            DeviceOrientation.MOVING to 0f,
        ).forEachIndexed { index, (orientation, z) ->
            gate.reset()
            assertWaiting(sample(orientation, z, index * 2_000L))
            assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, index * 2_000L + 800L))
            assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, index * 2_000L + 1_600L))
        }
    }

    @Test fun invalidGravityDoesNotArm() {
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 0L, gravity = 13f))
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 700L, gravity = 13f))
    }

    @Test fun transitionTimeoutDoesNotLock() {
        arm()
        assertWaiting(sample(DeviceOrientation.MOVING, 0f, 800L))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 3_301L))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 4_100L))
    }

    @Test fun resetClearsArmedState() {
        arm()
        gate.reset()
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 800L))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 1_500L))
    }

    @Test fun lockRequestedOccursOnce() {
        arm()
        sample(DeviceOrientation.FACE_DOWN, -1f, 800L)
        assertEquals(FlipToLockGestureResult.LockRequested, sample(DeviceOrientation.FACE_DOWN, -1f, 1_500L))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 2_200L))
    }

    private fun arm() {
        sample(DeviceOrientation.FACE_UP, 1f, 1L)
        assertEquals(FlipToLockGestureResult.Armed, sample(DeviceOrientation.FACE_UP, 1f, 701L))
    }

    private fun sample(
        orientation: DeviceOrientation,
        normalizedZ: Float,
        millis: Long,
        gravity: Float = 9.81f,
    ) = gate.onSample(orientation, normalizedZ, gravity, millis * 1_000_000L + 1L)

    private fun assertWaiting(result: FlipToLockGestureResult) {
        assertEquals(FlipToLockGestureResult.Waiting, result)
    }
}
