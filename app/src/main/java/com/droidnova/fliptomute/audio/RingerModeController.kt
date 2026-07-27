package com.droidnova.fliptomute.audio

import com.droidnova.fliptomute.ui.screens.home.FlipAction

interface RingerModeController {
    fun getCurrentMode(): DeviceRingerMode
    fun applyTemporaryAction(action: FlipAction): RingerModeResult
    fun restorePreviousMode(): RingerModeResult
    fun clearTemporaryChange()
}

fun interface RingerModeControllerFactory {
    fun create(): RingerModeController
}

internal interface RingerModePlatform {
    val isAvailable: Boolean
    val isVolumeFixed: Boolean
    val hasNotificationPolicyAccess: Boolean
    fun getRingerMode(): Int
    fun setRingerMode(mode: Int)
}
