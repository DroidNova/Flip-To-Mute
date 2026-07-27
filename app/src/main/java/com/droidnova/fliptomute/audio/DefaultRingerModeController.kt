package com.droidnova.fliptomute.audio

import com.droidnova.fliptomute.data.recovery.RingerRecoveryRepository
import com.droidnova.fliptomute.data.recovery.RingerRecoverySession
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultRingerModeController(
    private val platform: RingerModePlatform,
    private val recoveryRepository: RingerRecoveryRepository,
) : RingerModeController {
    private val mutex = Mutex()

    override fun getCurrentMode(): DeviceRingerMode = try {
        if (!platform.isAvailable) DeviceRingerMode.UNKNOWN
        else DeviceRingerModeMapper.fromAndroidMode(platform.getRingerMode())
    } catch (_: SecurityException) {
        DeviceRingerMode.UNKNOWN
    } catch (_: IllegalStateException) {
        DeviceRingerMode.UNKNOWN
    }

    override suspend fun applyTemporaryAction(action: FlipAction): RingerModeResult = mutex.withLock {
        readinessFailure()?.let { return@withLock RingerModeResult.Failure(it) }
        val current = getCurrentMode()
        val target = if (action == FlipAction.SILENT) DeviceRingerMode.SILENT else DeviceRingerMode.VIBRATE
        if (current == DeviceRingerMode.UNKNOWN) return@withLock RingerModeResult.Failure(RingerModeFailure.UNKNOWN)

        val existing = try {
            recoveryRepository.getRecoverySession()
        } catch (_: IOException) {
            return@withLock RingerModeResult.Failure(RingerModeFailure.RECOVERY_STATE_PERSISTENCE_FAILED)
        }
        val recovery = existing ?: if (current == target) {
            return@withLock RingerModeResult.Success(current, RingerModeSuccessType.NO_CHANGE)
        } else RingerRecoverySession(current, target)
        val updatedRecovery = if (existing == null) recovery else recovery.copy(appliedMode = target)
        if (!persist(updatedRecovery)) {
            return@withLock RingerModeResult.Failure(RingerModeFailure.RECOVERY_STATE_PERSISTENCE_FAILED)
        }
        if (current == target) return@withLock RingerModeResult.Success(current, RingerModeSuccessType.APPLIED)

        val androidTarget = DeviceRingerModeMapper.toAndroidMode(target)
            ?: return@withLock RingerModeResult.Failure(RingerModeFailure.UNKNOWN)
        writeAndVerify(androidTarget, target)?.let { return@withLock RingerModeResult.Failure(it) }
        RingerModeResult.Success(target, RingerModeSuccessType.APPLIED)
    }

    override suspend fun restorePreviousMode(): RingerModeResult = mutex.withLock {
        val session = try {
            recoveryRepository.getRecoverySession()
        } catch (_: IOException) {
            return@withLock RingerModeResult.Failure(RingerModeFailure.RECOVERY_STATE_PERSISTENCE_FAILED)
        } ?: return@withLock RingerModeResult.Failure(RingerModeFailure.NO_ACTIVE_CHANGE)
        val current = getCurrentMode()
        if (current == DeviceRingerMode.UNKNOWN) {
            return@withLock RingerModeResult.Failure(RingerModeFailure.AUDIO_SERVICE_UNAVAILABLE)
        }
        if (current != session.appliedMode) {
            if (!clearRecovery()) {
                return@withLock RingerModeResult.Failure(RingerModeFailure.RECOVERY_STATE_PERSISTENCE_FAILED)
            }
            return@withLock RingerModeResult.Success(current, RingerModeSuccessType.MANUAL_CHANGE_PRESERVED)
        }
        readinessFailure()?.let { return@withLock RingerModeResult.Failure(it) }
        val target = DeviceRingerModeMapper.toAndroidMode(session.previousMode)
            ?: return@withLock RingerModeResult.Failure(RingerModeFailure.UNKNOWN)
        writeAndVerify(target, session.previousMode)?.let { return@withLock RingerModeResult.Failure(it) }
        if (!clearRecovery()) {
            return@withLock RingerModeResult.Failure(RingerModeFailure.RECOVERY_STATE_PERSISTENCE_FAILED)
        }
        RingerModeResult.Success(session.previousMode, RingerModeSuccessType.RESTORED)
    }

    override suspend fun recoverPendingChange(): RingerModeRecoveryResult = mutex.withLock {
        val session = try {
            recoveryRepository.getRecoverySession()
        } catch (_: IOException) {
            return@withLock RingerModeRecoveryResult.Failure(RingerModeFailure.RECOVERY_STATE_PERSISTENCE_FAILED)
        } ?: return@withLock RingerModeRecoveryResult.NoPendingChange
        val current = getCurrentMode()
        if (current == DeviceRingerMode.UNKNOWN) {
            return@withLock RingerModeRecoveryResult.Failure(RingerModeFailure.AUDIO_SERVICE_UNAVAILABLE)
        }
        if (current != session.appliedMode) {
            return@withLock if (clearRecovery()) {
                RingerModeRecoveryResult.CurrentModePreserved(current)
            } else {
                RingerModeRecoveryResult.Failure(RingerModeFailure.RECOVERY_STATE_PERSISTENCE_FAILED)
            }
        }
        readinessFailure()?.let { return@withLock RingerModeRecoveryResult.Failure(it) }
        val target = DeviceRingerModeMapper.toAndroidMode(session.previousMode)
            ?: return@withLock RingerModeRecoveryResult.Failure(RingerModeFailure.UNKNOWN)
        writeAndVerify(target, session.previousMode)?.let { return@withLock RingerModeRecoveryResult.Failure(it) }
        if (!clearRecovery()) {
            return@withLock RingerModeRecoveryResult.Failure(RingerModeFailure.RECOVERY_STATE_PERSISTENCE_FAILED)
        }
        RingerModeRecoveryResult.Restored(session.previousMode)
    }

    override suspend fun clearTemporaryChange() = mutex.withLock { recoveryRepository.clearRecoverySession() }

    private suspend fun persist(session: RingerRecoverySession): Boolean = try {
        recoveryRepository.saveRecoverySession(session)
        true
    } catch (_: IOException) {
        false
    }

    private suspend fun clearRecovery(): Boolean = try {
        recoveryRepository.clearRecoverySession()
        true
    } catch (_: IOException) {
        false
    }

    private fun writeAndVerify(androidMode: Int, expected: DeviceRingerMode): RingerModeFailure? = try {
        platform.setRingerMode(androidMode)
        if (getCurrentMode() == expected) null else RingerModeFailure.CHANGE_NOT_APPLIED
    } catch (_: SecurityException) {
        RingerModeFailure.SOUND_CONTROL_ACCESS_REQUIRED
    } catch (_: IllegalArgumentException) {
        RingerModeFailure.CHANGE_NOT_APPLIED
    } catch (_: IllegalStateException) {
        RingerModeFailure.CHANGE_NOT_APPLIED
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
