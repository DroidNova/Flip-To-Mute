package com.droidnova.fliptomute.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

class FlatSurfaceFlipGateTest {
    private val gate = FlatSurfaceFlipGate()

    @Test fun initialFaceDownRequiresContinuousConfirmationAndAllowsOnlyOnce() {
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 0))
        assertEquals(FlatSurfaceFlipResult.Allowed, sample(DeviceOrientation.FACE_DOWN, -1f, 400))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 800))
    }

    @Test fun initialTiltOrMovementDisablesAlreadyFaceDownShortcut() {
        assertWaiting(sample(DeviceOrientation.MOVING, -0.4f, 0))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 100))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 600))
    }

    @Test fun invalidValuesDoNotArmOrTrigger() {
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 0, 20f))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 500))
        gate.reset()
        assertWaiting(sample(DeviceOrientation.FACE_UP, Float.NaN, 1_000))
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 1_600, 6f))
    }

    @Test fun stableFaceUpArmsAndStableFaceDownAllows() {
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 0))
        assertEquals(FlatSurfaceFlipResult.Armed, sample(DeviceOrientation.FACE_UP, .99f, 500))
        assertWaiting(sample(DeviceOrientation.MOVING, 0f, 600))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -.99f, 900))
        assertEquals(FlatSurfaceFlipResult.Allowed, sample(DeviceOrientation.FACE_DOWN, -.98f, 1_300))
    }

    @Test fun interruptedFaceUpAndVerticalPhoneDoNotArm() {
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 0))
        assertWaiting(sample(DeviceOrientation.MOVING, 0f, 300))
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 400))
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 800))
    }

    @Test fun transitionTimeoutRequiresACompleteNewSequence() {
        arm(0)
        assertWaiting(sample(DeviceOrientation.MOVING, 0f, 600))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 3_601))
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 4_100))
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 4_200))
        assertEquals(FlatSurfaceFlipResult.Armed, sample(DeviceOrientation.FACE_UP, 1f, 4_700))
    }

    @Test fun resetClearsCandidatesArmedAndTriggeredAndRestoresInitialShortcut() {
        arm(0)
        gate.reset()
        assertWaiting(sample(DeviceOrientation.FACE_DOWN, -1f, 1_000))
        assertEquals(FlatSurfaceFlipResult.Allowed, sample(DeviceOrientation.FACE_DOWN, -1f, 1_400))
        gate.reset()
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 2_000))
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, 2_400))
    }

    private fun arm(startMillis: Long) {
        assertWaiting(sample(DeviceOrientation.FACE_UP, 1f, startMillis))
        assertEquals(FlatSurfaceFlipResult.Armed, sample(DeviceOrientation.FACE_UP, 1f, startMillis + 500))
    }

    private fun sample(orientation: DeviceOrientation, z: Float, millis: Long, magnitude: Float = 9.81f) =
        gate.onSample(orientation, z, magnitude, (millis + 1) * 1_000_000L)

    private fun assertWaiting(result: FlatSurfaceFlipResult) =
        assertEquals(FlatSurfaceFlipResult.Waiting, result)
}
