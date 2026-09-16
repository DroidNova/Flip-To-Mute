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
import com.droidnova.fliptomute.audio.RingerModeFailure
import com.droidnova.fliptomute.audio.RingerModeResult
import com.droidnova.fliptomute.data.setup.AndroidSetupAccessObserver
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
    private val commandSequencer = MonitoringCommandSequencer()
    private var accessRevalidationJob: Job? = null
    private val accessObserver by lazy { AndroidSetupAccessObserver(this, ::requestAccessRevalidation) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        accessObserver.register()
        debugLog("Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        commandSequencer.record(startId)
        return when (MonitoringServiceCommandClassifier.classify(intent != null, intent?.action)) {
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
            MonitoringServiceCommand.REVALIDATE_ACCESS -> {
                requestAccessRevalidation()
                START_STICKY
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
    }

    private fun startMonitoring(isRestart: Boolean, isResume: Boolean, startId: Int) {
        debugLog("Start command received")
        val runtime = container.monitoringStateRepository.state.value
        if (runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Resuming ||
            runtime is MonitoringRuntimeState.Active
        ) return
        val token = commandSequencer.supersede()
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
            try {
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
                        finishStopped(writePreference = false, startId = startId)
                        return@launch
                    }
                    is StickyRestartDecision.StopFailure -> {
                        failStart(decision.reason, startId)
                        return@launch
                    }
                    StickyRestartDecision.Continue -> Unit
                }
                if (!setup.isSetupComplete || !commandSequencer.isCurrent(token)) {
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
                            if (!commandSequencer.isCurrent(token)) return@launch
                            if (reason == MonitoringFailure.SETUP_REQUIRED) {
                                requestAccessRevalidation()
                            } else if (isResume) failResume(reason) else failStart(reason)
                        }
                    },
                    proximityMonitor = container.proximityMonitorFactory.create(),
                    debugLog = ::debugLog,
                    deviceAdminRepository = container.deviceAdminCapabilityRepository,
                    screenLockController = container.screenLockController,
                    screenStateRepository = container.screenStateRepository,
                    monitoringStateRepository = container.monitoringStateRepository,
                )
                debugLog("Starting cellular call monitor")
                when (val result = coordinator?.startAndAwaitReady()) {
                    MonitoringCoordinatorStartResult.Started -> {
                        debugLog("Coordinator startup result: Started")
                        if (!commandSequencer.isCurrent(token)) return@launch
                        if (!container.setupAccessRepository.refreshAndGet().isSetupComplete) {
                            commandSequencer.supersede()
                            failStart(MonitoringFailure.SETUP_REQUIRED, startId)
                            return@launch
                        }
                        try {
                            container.appPreferencesRepository.setMonitoringPaused(false)
                            container.appPreferencesRepository.setMonitoringEnabled(true)
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            MonitoringLog.failure(this@FlipMonitoringService, "Saving monitoring preference failed", error)
                            failStart(MonitoringFailure.UNKNOWN, startId)
                            return@launch
                        }
                        if (!commandSequencer.isCurrent(token)) return@launch
                        publishMonitoringState(MonitoringRuntimeState.Active)
                        debugLog("Runtime state changed to Active")
                    }
                    is MonitoringCoordinatorStartResult.Failed -> {
                        debugLog("Coordinator startup result: Failed(${result.reason.name})")
                        if (isResume) failResume(result.reason) else failStart(result.reason)
                    }
                    null -> if (isResume) failResume(MonitoringFailure.CALL_MONITOR_FAILED)
                    else failStart(MonitoringFailure.CALL_MONITOR_FAILED)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                MonitoringLog.failure(this@FlipMonitoringService, "Monitoring command failed", error)
                if (!commandSequencer.isCurrent(token)) return@launch
                if (isResume) failResume(MonitoringFailure.UNKNOWN) else failStart(MonitoringFailure.UNKNOWN)
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
        commandSequencer.supersede()
        commandJob?.cancel()
        coordinator?.beginStopping()
        publishMonitoringState(MonitoringRuntimeState.Stopping)
        notificationHelper.cancelPausedNotification()
        commandJob = serviceScope.launch { finishStopped(writePreference = true) }
    }

    private fun requestAccessRevalidation() {
        if (accessRevalidationJob?.isActive == true) return
        accessRevalidationJob = serviceScope.launch {
            val runtime = container.monitoringStateRepository.state.value
            if (!shouldShutdownForAccessLoss(runtime, setupComplete = false)) return@launch
            val accessComplete = try {
                container.setupAccessRepository.refreshAndGet().isSetupComplete
            } catch (error: RuntimeException) {
                MonitoringLog.failure(this@FlipMonitoringService, "Refreshing setup access failed", error)
                false
            }
            if (!shouldShutdownForAccessLoss(container.monitoringStateRepository.state.value, accessComplete)) {
                return@launch
            }
            commandSequencer.supersede()
            commandJob?.cancel()
            coordinator?.beginStopping()
            publishMonitoringState(MonitoringRuntimeState.Stopping)
            commandJob = serviceScope.launch {
                finishStopped(
                    writePreference = true,
                    preserveError = true,
                    failure = MonitoringFailure.SETUP_REQUIRED,
                )
            }
        }
    }

    private fun pauseMonitoring(startId: Int) {
        val runtime = container.monitoringStateRepository.state.value
        if (runtime is MonitoringRuntimeState.Paused || runtime is MonitoringRuntimeState.Pausing) return
        commandSequencer.supersede()
        commandJob?.cancel()
        publishMonitoringState(MonitoringRuntimeState.Pausing)
        coordinator?.beginStopping()
        commandJob = serviceScope.launch { finishPaused() }
    }

    private suspend fun finishPaused(startId: Int = commandSequencer.latestStartId) {
        val restoration = coordinator?.stop()
        coordinator = null
        val pausedPersisted = persistPausedIntentOrFallbackOff()
        if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        when {
            !pausedPersisted -> {
                publishMonitoringState(MonitoringRuntimeState.Error(MonitoringFailure.CLEANUP_FAILED))
                notificationHelper.cancelPausedNotification()
            }
            restoration.isActualRestorationFailure() -> {
                publishMonitoringState(
                    MonitoringRuntimeState.Error(
                        MonitoringFailure.CLEANUP_FAILED,
                        MonitoringErrorRecoveryIntent.RESUME,
                    ),
                )
                notificationHelper.showPausedNotification()
            }
            else -> {
                publishMonitoringState(MonitoringRuntimeState.Paused)
                notificationHelper.showPausedNotification()
            }
        }
        stopSelfResult(commandSequencer.latestStartId.coerceAtLeast(startId))
    }

    private suspend fun failResume(reason: MonitoringFailure, startId: Int = commandSequencer.latestStartId) {
        debugLog("Monitoring resume failure reason: ${reason.name}")
        val restoration = coordinator?.stop()
        coordinator = null
        val pausedPersisted = persistPausedIntentOrFallbackOff()
        if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        if (pausedPersisted) {
            publishMonitoringState(
                MonitoringRuntimeState.Error(
                    if (restoration.isActualRestorationFailure()) MonitoringFailure.CLEANUP_FAILED else reason,
                    MonitoringErrorRecoveryIntent.RESUME,
                ),
            )
            notificationHelper.showPausedNotification()
        } else {
            publishMonitoringState(MonitoringRuntimeState.Error(MonitoringFailure.CLEANUP_FAILED))
            notificationHelper.cancelPausedNotification()
        }
        stopSelfResult(commandSequencer.latestStartId.coerceAtLeast(startId))
    }

    private suspend fun failStart(reason: MonitoringFailure, startId: Int = commandSequencer.latestStartId) {
        debugLog("Monitoring runtime failure reason: ${reason.name}")
        publishMonitoringState(MonitoringRuntimeState.Error(reason))
        debugLog("Runtime state changed to Error")
        finishStopped(writePreference = true, preserveError = true, failure = reason, startId = startId)
    }

    private suspend fun finishStopped(
        writePreference: Boolean,
        preserveError: Boolean = false,
        failure: MonitoringFailure? = null,
        startId: Int = commandSequencer.latestStartId,
    ) {
        debugLog("Service cleanup started")
        val restoration = coordinator?.stop()
        coordinator = null
        var persistenceFailed = false
        if (writePreference) {
            try {
                container.appPreferencesRepository.setMonitoringEnabled(false)
                container.appPreferencesRepository.setMonitoringPaused(false)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                MonitoringLog.failure(this, "Clearing monitoring preference failed", error)
                persistenceFailed = true
                tryPersistOffAgain()
            }
        }
        val cleanupFailed = persistenceFailed || restoration.isActualRestorationFailure()
        if (cleanupFailed) {
            publishMonitoringState(MonitoringRuntimeState.Error(MonitoringFailure.CLEANUP_FAILED))
        } else if (!preserveError) {
            publishMonitoringState(MonitoringRuntimeState.Stopped)
        } else if (failure != null) {
            publishMonitoringState(MonitoringRuntimeState.Error(failure))
        }
        if (foregroundStarted) stopForeground(STOP_FOREGROUND_REMOVE)
        notificationHelper.cancelPausedNotification()
        foregroundStarted = false
        stopSelfResult(commandSequencer.latestStartId.coerceAtLeast(startId))
        debugLog("Service stopped")
    }

    override fun onDestroy() {
        accessObserver.unregister()
        accessRevalidationJob?.cancel()
        commandSequencer.supersede()
        coordinator?.beginStopping()
        val immediateRestoration = coordinator?.restorePreviousModeImmediately()
        if (immediateRestoration.isActualRestorationFailure()) {
            publishMonitoringState(MonitoringRuntimeState.Error(MonitoringFailure.CLEANUP_FAILED))
        }
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
        fun createRevalidateAccessIntent(context: Context) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.REVALIDATE_ACCESS_ACTION)
    }

    private fun debugLog(message: String) {
        MonitoringLog.d(this, message)
    }

    private fun publishMonitoringState(state: MonitoringRuntimeState) {
        container.monitoringStateRepository.updateState(state)
        container.quickSettingsTileUpdateRequester.requestUpdate()
    }

    private suspend fun persistPausedIntentOrFallbackOff(): Boolean = try {
        container.appPreferencesRepository.setMonitoringEnabled(true)
        container.appPreferencesRepository.setMonitoringPaused(true)
        true
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        MonitoringLog.failure(this, "Saving paused monitoring preference failed", error)
        tryPersistOffAgain()
        false
    }

    private suspend fun tryPersistOffAgain() {
        try {
            container.appPreferencesRepository.setMonitoringEnabled(false)
            container.appPreferencesRepository.setMonitoringPaused(false)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            MonitoringLog.failure(this, "Repairing stopped monitoring preference failed", error)
        }
    }

    private fun RingerModeResult?.isActualRestorationFailure(): Boolean =
        this is RingerModeResult.Failure && reason != RingerModeFailure.NO_ACTIVE_CHANGE

}
