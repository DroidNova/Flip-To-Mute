package com.droidnova.fliptomute.audio

import com.droidnova.fliptomute.ui.screens.home.FlipAction

interface RingerModeController {
    fun getCurrentMode(): DeviceRingerMode
    suspend fun applyTemporaryAction(action: FlipAction): RingerModeResult
    suspend fun restorePreviousMode(): RingerModeResult
    suspend fun recoverPendingChange(): RingerModeRecoveryResult
    suspend fun clearTemporaryChange()
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
