package com.droidnova.fliptomute.audio

import com.droidnova.fliptomute.ui.screens.home.FlipAction

class FakeRingerModeController(
    var currentMode: DeviceRingerMode = DeviceRingerMode.NORMAL,
    var applyResult: RingerModeResult = RingerModeResult.Success(
        DeviceRingerMode.SILENT,
        RingerModeSuccessType.APPLIED,
    ),
    var restoreResult: RingerModeResult = RingerModeResult.Success(
        DeviceRingerMode.NORMAL,
        RingerModeSuccessType.RESTORED,
    ),
) : RingerModeController {
    var applyCount = 0
    var restoreCount = 0
    val appliedActions = mutableListOf<FlipAction>()
    override fun getCurrentMode() = currentMode
    override fun applyTemporaryAction(action: FlipAction): RingerModeResult {
        applyCount++
        appliedActions += action
        (applyResult as? RingerModeResult.Success)?.let { currentMode = it.currentMode }
        return applyResult
    }
    override fun restorePreviousMode(): RingerModeResult {
        restoreCount++
        (restoreResult as? RingerModeResult.Success)?.let { currentMode = it.currentMode }
        return restoreResult
    }
    override fun clearTemporaryChange() = Unit
}
