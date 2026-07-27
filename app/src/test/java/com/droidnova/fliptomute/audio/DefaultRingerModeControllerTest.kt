package com.droidnova.fliptomute.audio

import com.droidnova.fliptomute.data.recovery.FakeRingerRecoveryRepository
import com.droidnova.fliptomute.data.recovery.RingerRecoverySession
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultRingerModeControllerTest {
    @Test fun recoveryIsSavedBeforeSoundWrite() = runTest {
        val recovery = FakeRingerRecoveryRepository()
        val platform = FakeRingerModePlatform(events = recovery.events)
        val controller = DefaultRingerModeController(platform, recovery)
        controller.applyTemporaryAction(FlipAction.SILENT)
        assertEquals(listOf("save", "write"), recovery.events)
        assertEquals(RingerRecoverySession(DeviceRingerMode.NORMAL, DeviceRingerMode.SILENT), recovery.getRecoverySession())
    }

    @Test fun persistenceFailurePreventsSoundWrite() = runTest {
        val recovery = FakeRingerRecoveryRepository().apply { failWrites = true }
        val platform = FakeRingerModePlatform()
        val result = DefaultRingerModeController(platform, recovery).applyTemporaryAction(FlipAction.SILENT)
        assertFailure(result, RingerModeFailure.RECOVERY_STATE_PERSISTENCE_FAILED)
        assertEquals(DeviceRingerModeMapper.ANDROID_MODE_NORMAL, platform.mode)
    }

    @Test fun secondActionPreservesOriginalRestoreMode() = runTest {
        val recovery = FakeRingerRecoveryRepository()
        val platform = FakeRingerModePlatform()
        val controller = DefaultRingerModeController(platform, recovery)
        controller.applyTemporaryAction(FlipAction.SILENT)
        controller.applyTemporaryAction(FlipAction.VIBRATE)
        assertEquals(DeviceRingerMode.NORMAL, recovery.getRecoverySession()?.previousMode)
        controller.restorePreviousMode()
        assertEquals(DeviceRingerModeMapper.ANDROID_MODE_NORMAL, platform.mode)
    }

    @Test fun alreadyTargetCreatesNoRecovery() = runTest {
        val recovery = FakeRingerRecoveryRepository()
        val controller = DefaultRingerModeController(
            FakeRingerModePlatform(DeviceRingerModeMapper.ANDROID_MODE_SILENT), recovery,
        )
        val result = controller.applyTemporaryAction(FlipAction.SILENT) as RingerModeResult.Success
        assertEquals(RingerModeSuccessType.NO_CHANGE, result.type)
        assertEquals(null, recovery.getRecoverySession())
    }

    @Test fun restoreMatchingModeClearsRecovery() = runTest {
        val recovery = FakeRingerRecoveryRepository(
            RingerRecoverySession(DeviceRingerMode.NORMAL, DeviceRingerMode.SILENT),
        )
        val platform = FakeRingerModePlatform(DeviceRingerModeMapper.ANDROID_MODE_SILENT)
        val result = DefaultRingerModeController(platform, recovery).restorePreviousMode()
        assertTrue(result is RingerModeResult.Success)
        assertEquals(null, recovery.getRecoverySession())
        assertEquals(DeviceRingerModeMapper.ANDROID_MODE_NORMAL, platform.mode)
    }

    @Test fun manualModeChangeIsPreservedAndCleared() = runTest {
        val recovery = FakeRingerRecoveryRepository(
            RingerRecoverySession(DeviceRingerMode.NORMAL, DeviceRingerMode.SILENT),
        )
        val platform = FakeRingerModePlatform(DeviceRingerModeMapper.ANDROID_MODE_VIBRATE)
        val result = DefaultRingerModeController(platform, recovery).restorePreviousMode() as RingerModeResult.Success
        assertEquals(RingerModeSuccessType.MANUAL_CHANGE_PRESERVED, result.type)
        assertEquals(DeviceRingerModeMapper.ANDROID_MODE_VIBRATE, platform.mode)
        assertEquals(null, recovery.getRecoverySession())
    }

    @Test fun processRecoveryRestoresAndIsIdempotent() = runTest {
        val recovery = FakeRingerRecoveryRepository(
            RingerRecoverySession(DeviceRingerMode.VIBRATE, DeviceRingerMode.SILENT),
        )
        val platform = FakeRingerModePlatform(DeviceRingerModeMapper.ANDROID_MODE_SILENT)
        val controller = DefaultRingerModeController(platform, recovery)
        assertTrue(controller.recoverPendingChange() is RingerModeRecoveryResult.Restored)
        assertEquals(DeviceRingerModeMapper.ANDROID_MODE_VIBRATE, platform.mode)
        assertEquals(RingerModeRecoveryResult.NoPendingChange, controller.recoverPendingChange())
    }

    @Test fun persistedBeforeWriteAndManualCurrentModesArePreserved() = runTest {
        for (current in listOf(DeviceRingerMode.NORMAL, DeviceRingerMode.VIBRATE)) {
            val recovery = FakeRingerRecoveryRepository(
                RingerRecoverySession(DeviceRingerMode.NORMAL, DeviceRingerMode.SILENT),
            )
            val platform = FakeRingerModePlatform(DeviceRingerModeMapper.toAndroidMode(current)!!)
            val result = DefaultRingerModeController(platform, recovery).recoverPendingChange()
            assertTrue(result is RingerModeRecoveryResult.CurrentModePreserved)
            assertEquals(DeviceRingerModeMapper.toAndroidMode(current), platform.mode)
        }
    }

    private fun assertFailure(result: RingerModeResult, reason: RingerModeFailure) {
        assertEquals(reason, (result as RingerModeResult.Failure).reason)
    }
}

private class FakeRingerModePlatform(
    var mode: Int = DeviceRingerModeMapper.ANDROID_MODE_NORMAL,
    private val events: MutableList<String> = mutableListOf(),
    override var isAvailable: Boolean = true,
    override var isVolumeFixed: Boolean = false,
    override var hasNotificationPolicyAccess: Boolean = true,
) : RingerModePlatform {
    override fun getRingerMode() = mode
    override fun setRingerMode(mode: Int) { events += "write"; this.mode = mode }
}
