package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.RingerModeController
import com.droidnova.fliptomute.audio.RingerModeFailure
import com.droidnova.fliptomute.audio.RingerModeRecoveryResult
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileUpdateRequester
import com.droidnova.fliptomute.notification.PausedNotificationController

sealed interface AppRecoveryResult {
    data object Complete : AppRecoveryResult
    data class SoundRecoveryFailed(val reason: RingerModeFailure) : AppRecoveryResult
}

interface AppRecoveryManager {
    suspend fun reconcileMonitoringState(requestActiveReconstruction: Boolean): AppRecoveryResult
}

class DefaultAppRecoveryManager(
    private val preferencesRepository: AppPreferencesRepository,
    private val monitoringStateRepository: MonitoringStateRepository,
    private val ringerModeController: RingerModeController,
    private val serviceController: MonitoringServiceController,
    private val setupAccessRepository: SetupAccessRepository,
    private val tileUpdateRequester: QuickSettingsTileUpdateRequester = QuickSettingsTileUpdateRequester {},
    private val pausedNotificationController: PausedNotificationController = object : PausedNotificationController {
        override fun showPausedNotification() = Unit
        override fun cancelPausedNotification() = Unit
    },
) : AppRecoveryManager {
    private val mutex = Mutex()
    private var activeReconstructionRequested = false

    override suspend fun reconcileMonitoringState(
        requestActiveReconstruction: Boolean,
    ): AppRecoveryResult = mutex.withLock {
        val preferences = preferencesRepository.preferences.first()
        val recovery = if (requestActiveReconstruction) {
            ringerModeController.recoverPendingChange()
        } else {
            RingerModeRecoveryResult.NoPendingChange
        }
        if (recovery is RingerModeRecoveryResult.Failure) {
            if (preferences.monitoringEnabled && preferences.monitoringPaused) {
                monitoringStateRepository.updateState(
                    MonitoringRuntimeState.Error(
                        MonitoringFailure.SOUND_CONTROL_FAILED,
                        MonitoringErrorRecoveryIntent.RESUME,
                    ),
                )
                pausedNotificationController.showPausedNotification()
            } else {
                monitoringStateRepository.updateState(
                    MonitoringRuntimeState.Error(MonitoringFailure.SOUND_CONTROL_FAILED),
                )
            }
            tileUpdateRequester.requestUpdate()
            return@withLock AppRecoveryResult.SoundRecoveryFailed(recovery.reason)
        }
        if (!preferences.monitoringEnabled) {
            activeReconstructionRequested = false
            monitoringStateRepository.updateState(MonitoringRuntimeState.Stopped)
            pausedNotificationController.cancelPausedNotification()
            tileUpdateRequester.requestUpdate()
            return@withLock AppRecoveryResult.Complete
        }
        if (preferences.monitoringPaused) {
            activeReconstructionRequested = false
            monitoringStateRepository.updateState(MonitoringRuntimeState.Paused)
            pausedNotificationController.showPausedNotification()
            tileUpdateRequester.requestUpdate()
            return@withLock AppRecoveryResult.Complete
        }
        val reconstructionAccessComplete = if (requestActiveReconstruction) {
            try {
                setupAccessRepository.refreshAndGet().isSetupComplete
            } catch (_: RuntimeException) {
                false
            }
        } else true
        if (!reconstructionAccessComplete) {
            activeReconstructionRequested = false
            val failure = try {
                preferencesRepository.setMonitoringEnabled(false)
                preferencesRepository.setMonitoringPaused(false)
                MonitoringFailure.SETUP_REQUIRED
            } catch (_: Exception) {
                MonitoringFailure.CLEANUP_FAILED
            }
            monitoringStateRepository.updateState(MonitoringRuntimeState.Error(failure))
            pausedNotificationController.cancelPausedNotification()
            tileUpdateRequester.requestUpdate()
            return@withLock AppRecoveryResult.Complete
        }
        if (monitoringStateRepository.state.value is MonitoringRuntimeState.Unresolved ||
            monitoringStateRepository.state.value is MonitoringRuntimeState.Stopped
        ) {
            activeReconstructionRequested = false
            monitoringStateRepository.updateState(MonitoringRuntimeState.Recovering)
        }
        val runtime = monitoringStateRepository.state.value
        if (runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Resuming ||
            runtime is MonitoringRuntimeState.Active
        ) {
            activeReconstructionRequested = true
        }
        if (requestActiveReconstruction && runtime is MonitoringRuntimeState.Recovering &&
            !activeReconstructionRequested
        ) {
            when (val result = serviceController.startMonitoring()) {
                MonitoringCommandResult.Accepted -> activeReconstructionRequested = true
                is MonitoringCommandResult.Rejected -> {
                    preferencesRepository.setMonitoringEnabled(false)
                    monitoringStateRepository.updateState(MonitoringRuntimeState.Error(result.reason))
                }
            }
            tileUpdateRequester.requestUpdate()
        }
        AppRecoveryResult.Complete
    }
}
