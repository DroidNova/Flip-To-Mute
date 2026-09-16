package com.droidnova.fliptomute.audio

import com.droidnova.fliptomute.ui.screens.home.FlipAction

class FakeRingerModeController(
    currentMode: DeviceRingerMode = DeviceRingerMode.NORMAL,
    var applyResult: RingerModeResult = RingerModeResult.Success(
        DeviceRingerMode.SILENT,
        RingerModeSuccessType.APPLIED,
    ),
    var restoreResult: RingerModeResult = RingerModeResult.Success(
        DeviceRingerMode.NORMAL,
        RingerModeSuccessType.RESTORED,
    ),
    var recoveryResult: RingerModeRecoveryResult = RingerModeRecoveryResult.NoPendingChange,
) : RingerModeController {
    private var mode = currentMode
    var applyCount = 0
    var restoreCount = 0
    var recoverCount = 0
    val appliedActions = mutableListOf<FlipAction>()
    override fun getCurrentMode() = mode
    override suspend fun applyTemporaryAction(action: FlipAction): RingerModeResult {
        applyCount++
        appliedActions += action
        (applyResult as? RingerModeResult.Success)?.let { mode = it.currentMode }
        return applyResult
    }
    override suspend fun restorePreviousMode(): RingerModeResult {
        restoreCount++
        (restoreResult as? RingerModeResult.Success)?.let { mode = it.currentMode }
        return restoreResult
    }

    override fun restorePreviousModeImmediately(): RingerModeResult = restoreResult
    override suspend fun recoverPendingChange(): RingerModeRecoveryResult {
        recoverCount++
        return recoveryResult
    }
    override suspend fun clearTemporaryChange() = Unit

}
