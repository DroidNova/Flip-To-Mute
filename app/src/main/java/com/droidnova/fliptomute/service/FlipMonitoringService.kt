package com.droidnova.fliptomute.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.droidnova.fliptomute.app.FlipToMuteApplication
import com.droidnova.fliptomute.notification.MonitoringNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FlipMonitoringService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val container by lazy { (application as FlipToMuteApplication).container }
    private val notificationHelper by lazy { MonitoringNotificationManager(this) }
    private var coordinator: FlipMonitoringCoordinator? = null
    private var foregroundStarted = false
    private var cleanupStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
        }
        return START_NOT_STICKY
    }

    private fun startMonitoring() {
        val runtime = container.monitoringStateRepository.state.value
        if (runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Active) return
        cleanupStarted = false
        container.monitoringStateRepository.updateState(MonitoringRuntimeState.Starting)
        try {
            notificationHelper.createChannel()
            ServiceCompat.startForeground(
                this,
                MonitoringNotificationManager.NOTIFICATION_ID,
                notificationHelper.buildNotification(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                },
            )
            foregroundStarted = true
        } catch (_: SecurityException) {
            failStart(MonitoringFailure.NOTIFICATION_UNAVAILABLE)
            return
        } catch (_: IllegalStateException) {
            failStart(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
            return
        }

        container.setupAccessRepository.refresh()
        if (!container.setupAccessRepository.accessState.value.isSetupComplete) {
            failStart(MonitoringFailure.SETUP_REQUIRED)
            return
        }
        coordinator = FlipMonitoringCoordinator(
            preferencesRepository = container.appPreferencesRepository,
            callMonitor = container.cellularCallMonitorFactory.create(),
            orientationMonitor = container.deviceOrientationMonitorFactory.create(),
            ringerModeController = container.ringerModeControllerFactory.create(),
            scope = serviceScope,
            onReady = {
                container.monitoringStateRepository.updateState(MonitoringRuntimeState.Active)
                serviceScope.launch { container.appPreferencesRepository.setMonitoringEnabled(true) }
            },
            onFailure = ::handleFatalFailure,
        ).also { it.start() }
    }

    private fun handleFatalFailure(reason: MonitoringFailure) {
        container.monitoringStateRepository.updateState(MonitoringRuntimeState.Error(reason))
        serviceScope.launch {
            container.appPreferencesRepository.setMonitoringEnabled(false)
            finishService(preserveError = true)
        }
    }

    private fun failStart(reason: MonitoringFailure) {
        container.monitoringStateRepository.updateState(MonitoringRuntimeState.Error(reason))
        serviceScope.launch {
            container.appPreferencesRepository.setMonitoringEnabled(false)
            finishService(preserveError = true)
        }
    }

    private fun stopMonitoring() {
        if (cleanupStarted) return
        container.monitoringStateRepository.updateState(MonitoringRuntimeState.Stopping)
        // Stop event processing synchronously so a queued face-down sample cannot win this race.
        coordinator?.stop()
        serviceScope.launch {
            container.appPreferencesRepository.setMonitoringEnabled(false)
            finishService(preserveError = false)
        }
    }

    private fun finishService(preserveError: Boolean) {
        if (cleanupStarted) return
        cleanupStarted = true
        coordinator?.stop()
        coordinator = null
        if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        if (!preserveError) container.monitoringStateRepository.updateState(MonitoringRuntimeState.Stopped)
        stopSelf()
    }

    override fun onDestroy() {
        coordinator?.stop()
        coordinator = null
        if (container.monitoringStateRepository.state.value !is MonitoringRuntimeState.Error) {
            container.monitoringStateRepository.updateState(MonitoringRuntimeState.Stopped)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START_MONITORING = "com.droidnova.fliptomute.action.START_MONITORING"
        private const val ACTION_STOP_MONITORING = "com.droidnova.fliptomute.action.STOP_MONITORING"

        fun createStartIntent(context: Context) = Intent(context, FlipMonitoringService::class.java).setAction(ACTION_START_MONITORING)
        fun createStopIntent(context: Context) = Intent(context, FlipMonitoringService::class.java).setAction(ACTION_STOP_MONITORING)
    }
}
