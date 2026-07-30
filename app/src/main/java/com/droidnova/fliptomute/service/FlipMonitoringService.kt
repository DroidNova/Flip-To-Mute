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
                startMonitoring(isRestart = false, startId = startId)
                START_STICKY
            }
            MonitoringServiceCommand.STOP -> {
                stopMonitoring(startId)
                START_NOT_STICKY
            }
            MonitoringServiceCommand.RESTART -> {
                startMonitoring(isRestart = true, startId = startId)
                START_STICKY
            }
            MonitoringServiceCommand.UNKNOWN -> {
                stopMonitoring(startId)
                START_NOT_STICKY
            }
        }

    private fun startMonitoring(isRestart: Boolean, startId: Int) {
        debugLog("Start command received")
        val runtime = container.monitoringStateRepository.state.value
        if (runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Active) return
        val token = ++generation
        container.monitoringStateRepository.updateState(MonitoringRuntimeState.Starting)
        if (!promoteToForeground()) {
            commandJob = serviceScope.launch {
                failStart(MonitoringFailure.NOTIFICATION_UNAVAILABLE, startId)
            }
            return
        }
        debugLog("Foreground notification started")
        commandJob?.cancel()
        commandJob = serviceScope.launch {
            val recoveryController = container.ringerModeControllerFactory.create()
            val recovery = recoveryController.recoverPendingChange()
            if (recovery is RingerModeRecoveryResult.Failure) {
                failStart(MonitoringFailure.SOUND_CONTROL_FAILED, startId)
                return@launch
            }
            val storedMonitoring = container.appPreferencesRepository.preferences.first().monitoringEnabled
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
                failStart(MonitoringFailure.SETUP_REQUIRED, startId)
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
                onFailure = { reason -> serviceScope.launch { failStart(reason, startId) } },
            )
            debugLog("Starting cellular call monitor")
            when (val result = coordinator?.startAndAwaitReady()) {
                MonitoringCoordinatorStartResult.Started -> {
                    debugLog("Coordinator startup result: Started")
                    if (token != generation) return@launch
                    try {
                        container.appPreferencesRepository.setMonitoringEnabled(true)
                    } catch (error: Exception) {
                        if (error is CancellationException) throw error
                        MonitoringLog.failure(this@FlipMonitoringService, "Saving monitoring preference failed", error)
                        failStart(MonitoringFailure.UNKNOWN, startId)
                        return@launch
                    }
                    if (token != generation) return@launch
                    container.monitoringStateRepository.updateState(MonitoringRuntimeState.Active)
                    debugLog("Runtime state changed to Active")
                }
                is MonitoringCoordinatorStartResult.Failed -> {
                    debugLog("Coordinator startup result: Failed(${result.reason.name})")
                    failStart(result.reason, startId)
                }
                null -> failStart(MonitoringFailure.CALL_MONITOR_FAILED, startId)
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
        commandJob = serviceScope.launch { finishStopped(startId, writePreference = true) }
    }

    private suspend fun failStart(reason: MonitoringFailure, startId: Int) {
        debugLog("Monitoring runtime failure reason: ${reason.name}")
        container.monitoringStateRepository.updateState(MonitoringRuntimeState.Error(reason))
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
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                MonitoringLog.failure(this, "Clearing monitoring preference failed", error)
            }
        }
        if (!preserveError) container.monitoringStateRepository.updateState(MonitoringRuntimeState.Stopped)
        if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        stopSelfResult(startId)
        debugLog("Service stopped")
    }

    override fun onDestroy() {
        ++generation
        coordinator?.beginStopping()
        coordinator = null
        commandJob?.cancel()
        if (container.monitoringStateRepository.state.value !is MonitoringRuntimeState.Error) {
            container.monitoringStateRepository.updateState(MonitoringRuntimeState.Stopped)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun createStartIntent(context: Context) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.START_ACTION)
        fun createStopIntent(context: Context) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.STOP_ACTION)
    }

    private fun debugLog(message: String) {
        MonitoringLog.d(this, message)
    }

}
