package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.RingerModeController
import com.droidnova.fliptomute.audio.RingerModeFailure
import com.droidnova.fliptomute.audio.RingerModeRecoveryResult
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileUpdateRequester
import com.droidnova.fliptomute.notification.PausedNotificationController

sealed interface AppRecoveryResult {
    data object Complete : AppRecoveryResult
    data object StaleMonitoringCleared : AppRecoveryResult
    data class SoundRecoveryFailed(val reason: RingerModeFailure) : AppRecoveryResult
}

interface AppRecoveryManager {
    suspend fun recoverOnAppLaunch(): AppRecoveryResult
}

class DefaultAppRecoveryManager(
    private val preferencesRepository: AppPreferencesRepository,
    private val monitoringStateRepository: MonitoringStateRepository,
    private val ringerModeController: RingerModeController,
    private val tileUpdateRequester: QuickSettingsTileUpdateRequester = QuickSettingsTileUpdateRequester {},
    private val pausedNotificationController: PausedNotificationController = object : PausedNotificationController {
        override fun showPausedNotification() = Unit
        override fun cancelPausedNotification() = Unit
    },
) : AppRecoveryManager {
    private val mutex = Mutex()
    private var completed = false

    override suspend fun recoverOnAppLaunch(): AppRecoveryResult = mutex.withLock {
        if (completed) return@withLock AppRecoveryResult.Complete
        val preferences = preferencesRepository.preferences.first()
        val recovery = ringerModeController.recoverPendingChange()
        if (recovery is RingerModeRecoveryResult.Failure) {
            completed = true
            if (preferences.monitoringEnabled && preferences.monitoringPaused) {
                monitoringStateRepository.updateState(MonitoringRuntimeState.Paused)
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
            preferencesRepository.setMonitoringPaused(false)
            monitoringStateRepository.updateState(MonitoringRuntimeState.Stopped)
            pausedNotificationController.cancelPausedNotification()
            tileUpdateRequester.requestUpdate()
            completed = true
            return@withLock AppRecoveryResult.Complete
        }
        if (preferences.monitoringPaused) {
            monitoringStateRepository.updateState(MonitoringRuntimeState.Paused)
            pausedNotificationController.showPausedNotification()
            tileUpdateRequester.requestUpdate()
            completed = true
            return@withLock AppRecoveryResult.Complete
        }
        if (monitoringStateRepository.state.value is MonitoringRuntimeState.Stopped) {
            delay(STICKY_RESTART_GRACE_MILLIS)
            if (monitoringStateRepository.state.value is MonitoringRuntimeState.Stopped) {
                preferencesRepository.setMonitoringEnabled(false)
                tileUpdateRequester.requestUpdate()
                completed = true
                return@withLock AppRecoveryResult.StaleMonitoringCleared
            }
        }
        completed = true
        AppRecoveryResult.Complete
    }

    private companion object { const val STICKY_RESTART_GRACE_MILLIS = 1_000L }
}
