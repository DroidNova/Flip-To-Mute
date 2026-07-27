package com.droidnova.fliptomute.audio

import com.droidnova.fliptomute.ui.screens.home.FlipAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRingerModeControllerTest {
    @Test fun readsKnownAndUnknownModes() {
        assertEquals(DeviceRingerMode.NORMAL, controller().getCurrentMode())
        assertEquals(DeviceRingerMode.UNKNOWN, controller(mode = 99).getCurrentMode())
    }

    @Test fun appliesSilentAndRestoresNormal() {
        val platform = FakeRingerModePlatform()
        val controller = DefaultRingerModeController(platform)
        assertSuccess(controller.applyTemporaryAction(FlipAction.SILENT), DeviceRingerMode.SILENT)
        assertSuccess(controller.restorePreviousMode(), DeviceRingerMode.NORMAL)
    }

    @Test fun appliesVibrateAndRestoresNormal() {
        val controller = controller()
        assertSuccess(controller.applyTemporaryAction(FlipAction.VIBRATE), DeviceRingerMode.VIBRATE)
        assertSuccess(controller.restorePreviousMode(), DeviceRingerMode.NORMAL)
    }

    @Test fun originalModeSurvivesRepeatedAndDifferentActions() {
        val platform = FakeRingerModePlatform()
        val controller = DefaultRingerModeController(platform)
        controller.applyTemporaryAction(FlipAction.SILENT)
        controller.applyTemporaryAction(FlipAction.SILENT)
        controller.applyTemporaryAction(FlipAction.VIBRATE)
        assertSuccess(controller.restorePreviousMode(), DeviceRingerMode.NORMAL)
    }

    @Test fun alreadyTargetCreatesNoSession() {
        val controller = controller(DeviceRingerModeMapper.ANDROID_MODE_SILENT)
        val result = controller.applyTemporaryAction(FlipAction.SILENT) as RingerModeResult.Success
        assertEquals(RingerModeSuccessType.NO_CHANGE, result.type)
        assertFailure(controller.restorePreviousMode(), RingerModeFailure.NO_ACTIVE_CHANGE)
    }

    @Test fun accessFixedAndUnavailableFailuresAreTyped() {
        assertFailure(
            DefaultRingerModeController(FakeRingerModePlatform(hasNotificationPolicyAccess = false))
                .applyTemporaryAction(FlipAction.SILENT),
            RingerModeFailure.SOUND_CONTROL_ACCESS_REQUIRED,
        )
        assertFailure(
            DefaultRingerModeController(FakeRingerModePlatform(isVolumeFixed = true))
                .applyTemporaryAction(FlipAction.SILENT),
            RingerModeFailure.FIXED_VOLUME_DEVICE,
        )
        assertFailure(
            DefaultRingerModeController(FakeRingerModePlatform(isAvailable = false))
                .applyTemporaryAction(FlipAction.SILENT),
            RingerModeFailure.AUDIO_SERVICE_UNAVAILABLE,
        )
    }

    @Test fun failedVerificationIsReported() {
        val platform = FakeRingerModePlatform(applyWrites = false)
        assertFailure(
            DefaultRingerModeController(platform).applyTemporaryAction(FlipAction.SILENT),
            RingerModeFailure.CHANGE_NOT_APPLIED,
        )
    }

    @Test fun securityExceptionDoesNotCrash() {
        val platform = FakeRingerModePlatform(throwSecurity = true)
        assertFailure(
            DefaultRingerModeController(platform).applyTemporaryAction(FlipAction.SILENT),
            RingerModeFailure.SOUND_CONTROL_ACCESS_REQUIRED,
        )
    }

    @Test fun manualChangeIsPreserved() {
        val platform = FakeRingerModePlatform()
        val controller = DefaultRingerModeController(platform)
        controller.applyTemporaryAction(FlipAction.SILENT)
        platform.mode = DeviceRingerModeMapper.ANDROID_MODE_VIBRATE
        val result = controller.restorePreviousMode() as RingerModeResult.Success
        assertEquals(RingerModeSuccessType.MANUAL_CHANGE_PRESERVED, result.type)
        assertEquals(DeviceRingerMode.VIBRATE, result.currentMode)
    }

    @Test fun restoreToPreviousVibrateAndClearSession() {
        val platform = FakeRingerModePlatform(mode = DeviceRingerModeMapper.ANDROID_MODE_VIBRATE)
        val controller = DefaultRingerModeController(platform)
        controller.applyTemporaryAction(FlipAction.SILENT)
        assertSuccess(controller.restorePreviousMode(), DeviceRingerMode.VIBRATE)
        controller.applyTemporaryAction(FlipAction.SILENT)
        controller.clearTemporaryChange()
        assertFailure(controller.restorePreviousMode(), RingerModeFailure.NO_ACTIVE_CHANGE)
    }

    @Test fun revokedAccessKeepsSessionForLaterRestore() {
        val platform = FakeRingerModePlatform()
        val controller = DefaultRingerModeController(platform)
        controller.applyTemporaryAction(FlipAction.SILENT)
        platform.hasNotificationPolicyAccess = false
        assertFailure(controller.restorePreviousMode(), RingerModeFailure.SOUND_CONTROL_ACCESS_REQUIRED)
        platform.hasNotificationPolicyAccess = true
        assertSuccess(controller.restorePreviousMode(), DeviceRingerMode.NORMAL)
    }

    private fun controller(mode: Int = DeviceRingerModeMapper.ANDROID_MODE_NORMAL) =
        DefaultRingerModeController(FakeRingerModePlatform(mode = mode))

    private fun assertSuccess(result: RingerModeResult, mode: DeviceRingerMode) {
        assertTrue(result is RingerModeResult.Success)
        assertEquals(mode, (result as RingerModeResult.Success).currentMode)
    }

    private fun assertFailure(result: RingerModeResult, reason: RingerModeFailure) {
        assertEquals(reason, (result as RingerModeResult.Failure).reason)
    }
}

private class FakeRingerModePlatform(
    var mode: Int = DeviceRingerModeMapper.ANDROID_MODE_NORMAL,
    override var isAvailable: Boolean = true,
    override var isVolumeFixed: Boolean = false,
    override var hasNotificationPolicyAccess: Boolean = true,
    private val applyWrites: Boolean = true,
    private val throwSecurity: Boolean = false,
) : RingerModePlatform {
    override fun getRingerMode(): Int = mode
    override fun setRingerMode(mode: Int) {
        if (throwSecurity) throw SecurityException()
        if (applyWrites) this.mode = mode
    }
}
