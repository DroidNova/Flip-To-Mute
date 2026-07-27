package com.droidnova.fliptomute.ui.screens.sound_control_test

import com.droidnova.fliptomute.audio.DeviceRingerMode
import com.droidnova.fliptomute.ui.screens.home.FlipAction

enum class SoundControlTestResult {
    TEST_STARTED,
    RESTORED,
    MANUAL_CHANGE_PRESERVED,
    ALREADY_SET,
    ACCESS_REQUIRED,
    DEVICE_NOT_SUPPORTED,
    CHANGE_FAILED,
}

data class SoundControlTestUiState(
    val selectedAction: FlipAction = FlipAction.SILENT,
    val currentMode: DeviceRingerMode = DeviceRingerMode.UNKNOWN,
    val isTestRunning: Boolean = false,
    val remainingSeconds: Int = 0,
    val result: SoundControlTestResult? = null,
    val hasSoundControlAccess: Boolean = false,
    val canChangeSoundMode: Boolean = true,
)
