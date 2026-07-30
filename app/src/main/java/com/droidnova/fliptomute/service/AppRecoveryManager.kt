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
) : AppRecoveryManager {
    private val mutex = Mutex()
    private var completed = false

    override suspend fun recoverOnAppLaunch(): AppRecoveryResult = mutex.withLock {
        if (completed) return@withLock AppRecoveryResult.Complete
        val recovery = ringerModeController.recoverPendingChange()
        if (recovery is RingerModeRecoveryResult.Failure) {
            completed = true
            monitoringStateRepository.updateState(
                MonitoringRuntimeState.Error(MonitoringFailure.SOUND_CONTROL_FAILED),
            )
            tileUpdateRequester.requestUpdate()
            return@withLock AppRecoveryResult.SoundRecoveryFailed(recovery.reason)
        }
        val stored = preferencesRepository.preferences.first().monitoringEnabled
        if (stored && monitoringStateRepository.state.value is MonitoringRuntimeState.Stopped) {
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
