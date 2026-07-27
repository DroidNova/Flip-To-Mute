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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
        commandJob?.cancel()
        commandJob = serviceScope.launch {
            val recoveryController = container.ringerModeControllerFactory.create()
            val recovery = recoveryController.recoverPendingChange()
            if (recovery is RingerModeRecoveryResult.Failure) {
                failStart(MonitoringFailure.SOUND_CONTROL_FAILED, startId)
                return@launch
            }
            val storedMonitoring = container.appPreferencesRepository.preferences.first().monitoringEnabled
            container.setupAccessRepository.refresh()
            if (isRestart) when (
                val decision = StickyRestartPolicy.decide(
                    storedMonitoring,
                    container.setupAccessRepository.accessState.value.isSetupComplete,
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
            if (!container.setupAccessRepository.accessState.value.isSetupComplete || token != generation) {
                failStart(MonitoringFailure.SETUP_REQUIRED, startId)
                return@launch
            }
            coordinator?.stop()
            coordinator = FlipMonitoringCoordinator(
                container.appPreferencesRepository,
                container.cellularCallMonitorFactory.create(),
                container.deviceOrientationMonitorFactory.create(),
                container.ringerModeControllerFactory.create(),
                serviceScope,
                onReady = {
                    if (token == generation) {
                        container.monitoringStateRepository.updateState(MonitoringRuntimeState.Active)
                        serviceScope.launch {
                            if (token == generation) container.appPreferencesRepository.setMonitoringEnabled(true)
                        }
                    }
                },
                onFailure = { reason -> serviceScope.launch { failStart(reason, startId) } },
            ).also { it.start() }
        }
    }

    private fun promoteToForeground(): Boolean = try {
        if (!foregroundStarted) {
            notificationHelper.createChannel()
            ServiceCompat.startForeground(
                this,
                MonitoringNotificationManager.NOTIFICATION_ID,
                notificationHelper.buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else 0,
            )
            foregroundStarted = true
        }
        true
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalStateException) {
        false
    }

    private fun stopMonitoring(startId: Int) {
        ++generation
        commandJob?.cancel()
        coordinator?.beginStopping()
        commandJob = serviceScope.launch { finishStopped(startId, writePreference = true) }
    }

    private suspend fun failStart(reason: MonitoringFailure, startId: Int) {
        container.monitoringStateRepository.updateState(MonitoringRuntimeState.Error(reason))
        finishStopped(startId, writePreference = true, preserveError = true)
    }

    private suspend fun finishStopped(
        startId: Int,
        writePreference: Boolean,
        preserveError: Boolean = false,
    ) {
        coordinator?.stop()
        coordinator = null
        if (writePreference) container.appPreferencesRepository.setMonitoringEnabled(false)
        if (!preserveError) container.monitoringStateRepository.updateState(MonitoringRuntimeState.Stopped)
        if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        stopSelfResult(startId)
    }

    override fun onDestroy() {
        ++generation
        coordinator?.beginStopping()
        coordinator = null
        commandJob?.cancel()
        container.monitoringStateRepository.updateState(MonitoringRuntimeState.Stopped)
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun createStartIntent(context: Context) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.START_ACTION)
        fun createStopIntent(context: Context) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.STOP_ACTION)
    }
}
