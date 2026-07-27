package com.droidnova.fliptomute.audio

import com.droidnova.fliptomute.ui.screens.home.FlipAction

internal class DefaultRingerModeController(
    private val platform: RingerModePlatform,
) : RingerModeController {
    private var session: RingerModeChangeSession? = null

    override fun getCurrentMode(): DeviceRingerMode = synchronized(this) {
        try {
            if (!platform.isAvailable) return@synchronized DeviceRingerMode.UNKNOWN
            DeviceRingerModeMapper.fromAndroidMode(platform.getRingerMode())
        } catch (_: SecurityException) {
            DeviceRingerMode.UNKNOWN
        } catch (_: IllegalStateException) {
            DeviceRingerMode.UNKNOWN
        }
    }

    override fun applyTemporaryAction(action: FlipAction): RingerModeResult = synchronized(this) {
        readinessFailure()?.let { return@synchronized RingerModeResult.Failure(it) }
        val currentMode = getCurrentMode()
        val targetMode = when (action) {
            FlipAction.SILENT -> DeviceRingerMode.SILENT
            FlipAction.VIBRATE -> DeviceRingerMode.VIBRATE
        }
        if (currentMode == DeviceRingerMode.UNKNOWN) {
            return@synchronized RingerModeResult.Failure(RingerModeFailure.UNKNOWN)
        }
        if (currentMode == targetMode) {
            return@synchronized RingerModeResult.Success(
                currentMode,
                if (session == null) RingerModeSuccessType.NO_CHANGE else RingerModeSuccessType.APPLIED,
            )
        }
        val androidTarget = DeviceRingerModeMapper.toAndroidMode(targetMode)
            ?: return@synchronized RingerModeResult.Failure(RingerModeFailure.UNKNOWN)
        session = session?.copy(appliedMode = targetMode)
            ?: RingerModeChangeSession(previousMode = currentMode, appliedMode = targetMode)
        try {
            platform.setRingerMode(androidTarget)
            val verifiedMode = getCurrentMode()
            if (verifiedMode != targetMode) {
                return@synchronized RingerModeResult.Failure(RingerModeFailure.CHANGE_NOT_APPLIED)
            }
            RingerModeResult.Success(verifiedMode, RingerModeSuccessType.APPLIED)
        } catch (_: SecurityException) {
            RingerModeResult.Failure(RingerModeFailure.SOUND_CONTROL_ACCESS_REQUIRED)
        } catch (_: IllegalArgumentException) {
            RingerModeResult.Failure(RingerModeFailure.CHANGE_NOT_APPLIED)
        } catch (_: IllegalStateException) {
            RingerModeResult.Failure(RingerModeFailure.CHANGE_NOT_APPLIED)
        }
    }

    override fun restorePreviousMode(): RingerModeResult = synchronized(this) {
        val activeSession = session
            ?: return@synchronized RingerModeResult.Failure(RingerModeFailure.NO_ACTIVE_CHANGE)
        val currentMode = getCurrentMode()
        if (currentMode != activeSession.appliedMode) {
            session = null
            return@synchronized RingerModeResult.Success(
                currentMode,
                RingerModeSuccessType.MANUAL_CHANGE_PRESERVED,
            )
        }
        readinessFailure()?.let { return@synchronized RingerModeResult.Failure(it) }
        val restoreMode = DeviceRingerModeMapper.toAndroidMode(activeSession.previousMode)
            ?: return@synchronized RingerModeResult.Failure(RingerModeFailure.UNKNOWN)
        try {
            platform.setRingerMode(restoreMode)
            val verifiedMode = getCurrentMode()
            if (verifiedMode != activeSession.previousMode) {
                return@synchronized RingerModeResult.Failure(RingerModeFailure.CHANGE_NOT_APPLIED)
            }
            session = null
            RingerModeResult.Success(verifiedMode, RingerModeSuccessType.RESTORED)
        } catch (_: SecurityException) {
            RingerModeResult.Failure(RingerModeFailure.SOUND_CONTROL_ACCESS_REQUIRED)
        } catch (_: IllegalArgumentException) {
            RingerModeResult.Failure(RingerModeFailure.CHANGE_NOT_APPLIED)
        } catch (_: IllegalStateException) {
            RingerModeResult.Failure(RingerModeFailure.CHANGE_NOT_APPLIED)
        }
    }

    override fun clearTemporaryChange() = synchronized(this) {
        session = null
    }

    private fun readinessFailure(): RingerModeFailure? = try {
        when {
            !platform.isAvailable -> RingerModeFailure.AUDIO_SERVICE_UNAVAILABLE
            platform.isVolumeFixed -> RingerModeFailure.FIXED_VOLUME_DEVICE
            !platform.hasNotificationPolicyAccess -> RingerModeFailure.SOUND_CONTROL_ACCESS_REQUIRED
            else -> null
        }
    } catch (_: SecurityException) {
        RingerModeFailure.SOUND_CONTROL_ACCESS_REQUIRED
    } catch (_: IllegalStateException) {
        RingerModeFailure.AUDIO_SERVICE_UNAVAILABLE
    }
}
