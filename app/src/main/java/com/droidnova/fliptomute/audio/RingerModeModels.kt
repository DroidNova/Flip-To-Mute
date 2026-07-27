package com.droidnova.fliptomute.audio

enum class DeviceRingerMode { NORMAL, VIBRATE, SILENT, UNKNOWN }

enum class RingerModeFailure {
    SOUND_CONTROL_ACCESS_REQUIRED,
    FIXED_VOLUME_DEVICE,
    AUDIO_SERVICE_UNAVAILABLE,
    CHANGE_NOT_APPLIED,
    NO_ACTIVE_CHANGE,
    UNKNOWN,
    RECOVERY_STATE_PERSISTENCE_FAILED,
}

enum class RingerModeSuccessType { NO_CHANGE, APPLIED, RESTORED, MANUAL_CHANGE_PRESERVED }

sealed interface RingerModeResult {
    data class Success(
        val currentMode: DeviceRingerMode,
        val type: RingerModeSuccessType,
    ) : RingerModeResult
    data class Failure(val reason: RingerModeFailure) : RingerModeResult
}

sealed interface RingerModeRecoveryResult {
    data class Restored(val restoredMode: DeviceRingerMode) : RingerModeRecoveryResult
    data class CurrentModePreserved(val currentMode: DeviceRingerMode) : RingerModeRecoveryResult
    data object NoPendingChange : RingerModeRecoveryResult
    data class Failure(val reason: RingerModeFailure) : RingerModeRecoveryResult
}
