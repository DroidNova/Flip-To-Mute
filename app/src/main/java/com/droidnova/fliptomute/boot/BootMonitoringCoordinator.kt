package com.droidnova.fliptomute.boot

import com.droidnova.fliptomute.audio.RingerModeController
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.notification.PausedNotificationController
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileUpdateRequester
import com.droidnova.fliptomute.service.MonitoringCommandResult
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.service.MonitoringServiceController
import com.droidnova.fliptomute.service.MonitoringStateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface BootMonitoringCoordinator {
    suspend fun handleBootCompleted(): BootMonitoringResult
}

sealed interface BootMonitoringResult {
    data object MonitoringStartRequested : BootMonitoringResult
    data object PausedStateRestored : BootMonitoringResult
    data object StayedOff : BootMonitoringResult
    data class Failed(val reason: BootMonitoringFailure) : BootMonitoringResult
}

enum class BootMonitoringFailure { PREFERENCES_UNAVAILABLE, SETUP_INCOMPLETE, SERVICE_START_NOT_ALLOWED, UNKNOWN }

class DefaultBootMonitoringCoordinator(
    private val preferencesRepository: AppPreferencesRepository,
    private val setupAccessRepository: SetupAccessRepository,
    private val monitoringStateRepository: MonitoringStateRepository,
    private val serviceController: MonitoringServiceController,
    private val pausedNotificationController: PausedNotificationController,
    private val tileUpdateRequester: QuickSettingsTileUpdateRequester,
    private val recoveryController: RingerModeController,
    private val resolver: BootMonitoringActionResolver = BootMonitoringActionResolver(),
    private val log: (String) -> Unit = {},
) : BootMonitoringCoordinator {
    private val mutex = Mutex()
    private var completedResult: BootMonitoringResult? = null

    override suspend fun handleBootCompleted(): BootMonitoringResult = mutex.withLock {
        completedResult?.let { return@withLock it }
        val result = try {
            val preferences = preferencesRepository.preferences.first()
            log("Boot preferences loaded")
            log("Start after restart enabled: ${preferences.startAfterPhoneRestart}")
            log("Monitoring enabled: ${preferences.monitoringEnabled}")
            log("Monitoring paused: ${preferences.monitoringPaused}")
            val action = resolver.resolve(
                preferences.startAfterPhoneRestart,
                preferences.monitoringEnabled,
                preferences.monitoringPaused,
            )
            log("Boot action resolved: ${action.javaClass.simpleName}")
            when (action) {
                BootMonitoringAction.StayOff -> stayOff()
                BootMonitoringAction.RestorePausedState -> restorePaused()
                BootMonitoringAction.StartMonitoring -> startMonitoring()
            }
        } catch (_: java.io.IOException) {
            BootMonitoringResult.Failed(BootMonitoringFailure.PREFERENCES_UNAVAILABLE)
        } catch (_: IllegalStateException) {
            BootMonitoringResult.Failed(BootMonitoringFailure.UNKNOWN)
        }
        tileUpdateRequester.requestUpdate()
        log("Tile update requested")
        completedResult = result
        result
    }

    private suspend fun stayOff(): BootMonitoringResult {
        preferencesRepository.setMonitoringEnabled(false)
        pausedNotificationController.cancelPausedNotification()
        monitoringStateRepository.updateState(MonitoringRuntimeState.Stopped)
        log("Stayed Off")
        return BootMonitoringResult.StayedOff
    }

    private suspend fun restorePaused(): BootMonitoringResult {
        recoveryController.recoverPendingChange()
        monitoringStateRepository.updateState(MonitoringRuntimeState.Paused)
        pausedNotificationController.showPausedNotification()
        log("Paused state restored")
        return BootMonitoringResult.PausedStateRestored
    }

    private suspend fun startMonitoring(): BootMonitoringResult {
        val setupComplete = setupAccessRepository.refreshAndGet().isSetupComplete
        log("Setup validation complete: $setupComplete")
        if (!setupComplete) {
            stayOff()
            return BootMonitoringResult.Failed(BootMonitoringFailure.SETUP_INCOMPLETE)
        }
        return when (serviceController.startMonitoring()) {
            MonitoringCommandResult.Accepted -> {
                log("Monitoring start requested from boot: accepted")
                BootMonitoringResult.MonitoringStartRequested
            }
            is MonitoringCommandResult.Rejected -> {
                log("Monitoring start requested from boot: rejected")
                stayOff()
                BootMonitoringResult.Failed(BootMonitoringFailure.SERVICE_START_NOT_ALLOWED)
            }
        }
    }
}
