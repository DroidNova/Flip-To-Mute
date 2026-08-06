package com.droidnova.fliptomute.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.droidnova.fliptomute.app.FlipToMuteApplication
import com.droidnova.fliptomute.audio.RingerModeRecoveryResult
import com.droidnova.fliptomute.notification.MonitoringNotificationManager
import com.droidnova.fliptomute.util.MonitoringLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FlipMonitoringService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val container by lazy { (application as FlipToMuteApplication).container }
    private val notificationHelper by lazy { MonitoringNotificationManager(this) }
    private var coordinator: FlipMonitoringCoordinator? = null
    private var commandJob: Job? = null
    private var foregroundStarted = false
    private var generation = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        debugLog("Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        when (MonitoringServiceCommandClassifier.classify(intent != null, intent?.action)) {
            MonitoringServiceCommand.START -> {
                startMonitoring(
                    isRestart = false,
                    isResume = container.monitoringStateRepository.state.value is MonitoringRuntimeState.Paused,
                    startId = startId,
                )
                START_STICKY
            }
            MonitoringServiceCommand.PAUSE -> {
                pauseMonitoring(startId)
                START_NOT_STICKY
            }
            MonitoringServiceCommand.RESUME -> {
                startMonitoring(isRestart = false, isResume = true, startId = startId)
                START_STICKY
            }
            MonitoringServiceCommand.STOP -> {
                stopMonitoring(startId)
                START_NOT_STICKY
            }
            MonitoringServiceCommand.RESTART -> {
                startMonitoring(isRestart = true, isResume = false, startId = startId)
                START_STICKY
            }
            MonitoringServiceCommand.UNKNOWN -> {
                stopMonitoring(startId)
                START_NOT_STICKY
            }
        }

    private fun startMonitoring(isRestart: Boolean, isResume: Boolean, startId: Int) {
        debugLog("Start command received")
        val runtime = container.monitoringStateRepository.state.value
        if (runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Resuming ||
            runtime is MonitoringRuntimeState.Active
        ) return
        val token = ++generation
        notificationHelper.cancelPausedNotification()
        publishMonitoringState(if (isResume) MonitoringRuntimeState.Resuming else MonitoringRuntimeState.Starting)
        if (!promoteToForeground()) {
            commandJob = serviceScope.launch {
                if (isResume) failResume(MonitoringFailure.NOTIFICATION_UNAVAILABLE, startId)
                else failStart(MonitoringFailure.NOTIFICATION_UNAVAILABLE, startId)
            }
            return
        }
        debugLog("Foreground notification started")
        commandJob?.cancel()
        commandJob = serviceScope.launch {
            val storedPreferences = container.appPreferencesRepository.preferences.first()
            val recoveryController = container.ringerModeControllerFactory.create()
            val recovery = recoveryController.recoverPendingChange()
            if (recovery is RingerModeRecoveryResult.Failure) {
                if (isResume || storedPreferences.monitoringPaused) {
                    failResume(MonitoringFailure.SOUND_CONTROL_FAILED, startId)
                } else {
                    failStart(MonitoringFailure.SOUND_CONTROL_FAILED, startId)
                }
                return@launch
            }
            if (isRestart && storedPreferences.monitoringEnabled && storedPreferences.monitoringPaused) {
                finishPaused(startId)
                return@launch
            }
            val storedMonitoring = storedPreferences.monitoringEnabled
            val setup = container.setupAccessRepository.refreshAndGet()
            debugLog("Setup state refreshed")
            debugLog("Phone access status: ${setup.phoneStateStatus.name}")
            debugLog("Sound control access status: ${setup.soundControlStatus.name}")
            debugLog("Notification access status: ${setup.notificationStatus.name}")
            debugLog("Setup complete: ${setup.isSetupComplete}")
            if (isRestart) when (
                val decision = StickyRestartPolicy.decide(
                    storedMonitoring,
                    setup.isSetupComplete,
                )
            ) {
                StickyRestartDecision.StopDisabled -> {
                    finishStopped(startId, writePreference = false)
                    return@launch
                }
                is StickyRestartDecision.StopFailure -> {
                    failStart(decision.reason, startId)
                    return@launch
                }
                StickyRestartDecision.Continue -> Unit
            }
            if (!setup.isSetupComplete || token != generation) {
                if (isResume) failResume(MonitoringFailure.SETUP_REQUIRED, startId)
                else failStart(MonitoringFailure.SETUP_REQUIRED, startId)
                return@launch
            }
            coordinator?.stop()
            coordinator = FlipMonitoringCoordinator(
                container.appPreferencesRepository,
                container.cellularCallMonitorFactory.create(),
                container.deviceOrientationMonitorFactory.create(),
                container.ringerModeControllerFactory.create(),
                container.incomingCallVibrationControllerFactory.create(),
                serviceScope,
                onFailure = { reason ->
                    serviceScope.launch {
                        if (isResume) failResume(reason, startId) else failStart(reason, startId)
                    }
                },
            )
            debugLog("Starting cellular call monitor")
            when (val result = coordinator?.startAndAwaitReady()) {
                MonitoringCoordinatorStartResult.Started -> {
                    debugLog("Coordinator startup result: Started")
                    if (token != generation) return@launch
                    try {
                        container.appPreferencesRepository.setMonitoringPaused(false)
                        container.appPreferencesRepository.setMonitoringEnabled(true)
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        MonitoringLog.failure(this@FlipMonitoringService, "Saving monitoring preference failed", error)
                        failStart(MonitoringFailure.UNKNOWN, startId)
                        return@launch
                    }
                    if (token != generation) return@launch
                    publishMonitoringState(MonitoringRuntimeState.Active)
                    debugLog("Runtime state changed to Active")
                }
                is MonitoringCoordinatorStartResult.Failed -> {
                    debugLog("Coordinator startup result: Failed(${result.reason.name})")
                    if (isResume) failResume(result.reason, startId) else failStart(result.reason, startId)
                }
                null -> if (isResume) failResume(MonitoringFailure.CALL_MONITOR_FAILED, startId)
                else failStart(MonitoringFailure.CALL_MONITOR_FAILED, startId)
            }
        }
    }

    private fun promoteToForeground(): Boolean = try {
        if (!foregroundStarted) {
            notificationHelper.createChannel()
            debugLog("Foreground notification channel created")
            ServiceCompat.startForeground(
                this,
                MonitoringNotificationManager.NOTIFICATION_ID,
                notificationHelper.buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else 0,
            )
            foregroundStarted = true
            debugLog("Foreground notification posted")
        }
        true
    } catch (error: SecurityException) {
        MonitoringLog.failure(this, "Foreground promotion failed", error)
        false
    } catch (error: IllegalArgumentException) {
        MonitoringLog.failure(this, "Foreground promotion failed", error)
        false
    } catch (error: IllegalStateException) {
        MonitoringLog.failure(this, "Foreground promotion failed", error)
        false
    }

    private fun stopMonitoring(startId: Int) {
        ++generation
        commandJob?.cancel()
        coordinator?.beginStopping()
        publishMonitoringState(MonitoringRuntimeState.Stopping)
        notificationHelper.cancelPausedNotification()
        commandJob = serviceScope.launch { finishStopped(startId, writePreference = true) }
    }

    private fun pauseMonitoring(startId: Int) {
        val runtime = container.monitoringStateRepository.state.value
        if (runtime is MonitoringRuntimeState.Paused || runtime is MonitoringRuntimeState.Pausing) return
        ++generation
        commandJob?.cancel()
        publishMonitoringState(MonitoringRuntimeState.Pausing)
        coordinator?.beginStopping()
        commandJob = serviceScope.launch { finishPaused(startId) }
    }

    private suspend fun finishPaused(startId: Int) {
        coordinator?.stop()
        coordinator = null
        container.appPreferencesRepository.setMonitoringEnabled(true)
        container.appPreferencesRepository.setMonitoringPaused(true)
        if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        publishMonitoringState(MonitoringRuntimeState.Paused)
        notificationHelper.showPausedNotification()
        stopSelfResult(startId)
    }

    private suspend fun failResume(reason: MonitoringFailure, startId: Int) {
        debugLog("Monitoring resume failure reason: ${reason.name}")
        coordinator?.stop()
        coordinator = null
        container.appPreferencesRepository.setMonitoringEnabled(true)
        container.appPreferencesRepository.setMonitoringPaused(true)
        if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        publishMonitoringState(MonitoringRuntimeState.Error(reason))
        notificationHelper.showPausedNotification()
        stopSelfResult(startId)
    }

    private suspend fun failStart(reason: MonitoringFailure, startId: Int) {
        debugLog("Monitoring runtime failure reason: ${reason.name}")
        publishMonitoringState(MonitoringRuntimeState.Error(reason))
        debugLog("Runtime state changed to Error")
        finishStopped(startId, writePreference = true, preserveError = true)
    }

    private suspend fun finishStopped(
        startId: Int,
        writePreference: Boolean,
        preserveError: Boolean = false,
    ) {
        debugLog("Service cleanup started")
        coordinator?.stop()
        coordinator = null
        if (writePreference) {
            try {
                container.appPreferencesRepository.setMonitoringEnabled(false)
                container.appPreferencesRepository.setMonitoringPaused(false)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                MonitoringLog.failure(this, "Clearing monitoring preference failed", error)
            }
        }
        if (!preserveError) publishMonitoringState(MonitoringRuntimeState.Stopped)
        if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
        notificationHelper.cancelPausedNotification()
        foregroundStarted = false
        stopSelfResult(startId)
        debugLog("Service stopped")
    }

    override fun onDestroy() {
        ++generation
        coordinator?.beginStopping()
        coordinator = null
        commandJob?.cancel()
        val runtime = container.monitoringStateRepository.state.value
        if (runtime !is MonitoringRuntimeState.Error && runtime !is MonitoringRuntimeState.Paused) {
            publishMonitoringState(MonitoringRuntimeState.Stopped)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun createStartIntent(context: Context) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.START_ACTION)
        fun createStopIntent(context: Context) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.STOP_ACTION)
        fun createPauseIntent(context: Context) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.PAUSE_ACTION)
        fun createResumeIntent(context: Context) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.RESUME_ACTION)
    }

    private fun debugLog(message: String) {
        MonitoringLog.d(this, message)
    }

    private fun publishMonitoringState(state: MonitoringRuntimeState) {
        container.monitoringStateRepository.updateState(state)
        container.quickSettingsTileUpdateRequester.requestUpdate()
    }

}
