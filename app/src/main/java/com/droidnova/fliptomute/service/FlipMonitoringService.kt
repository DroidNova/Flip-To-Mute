package com.droidnova.fliptomute.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.droidnova.fliptomute.audio.FlipFeedback
import com.droidnova.fliptomute.audio.IncomingCallVibrationControllerFactory
import com.droidnova.fliptomute.audio.RingerModeControllerFactory
import com.droidnova.fliptomute.data.analytics.AnalyticsLogger
import com.droidnova.fliptomute.data.analytics.Funnel
import com.droidnova.fliptomute.data.review.ReviewStore
import com.droidnova.fliptomute.data.stats.FlipStatsStore
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.deviceadmin.DeviceAdminCapabilityRepository
import com.droidnova.fliptomute.notification.InterruptionAlertController
import com.droidnova.fliptomute.quicksettings.QuickSettingsTileUpdateRequester
import com.droidnova.fliptomute.screenlock.ScreenLockController
import com.droidnova.fliptomute.screenlock.ScreenStateRepository
import com.droidnova.fliptomute.sensor.DeviceOrientationMonitorFactory
import com.droidnova.fliptomute.sensor.ProximityMonitorFactory
import com.droidnova.fliptomute.telephony.CellularCallMonitorFactory
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.droidnova.fliptomute.audio.RingerModeRecoveryResult
import com.droidnova.fliptomute.audio.RingerModeFailure
import com.droidnova.fliptomute.audio.RingerModeResult
import com.droidnova.fliptomute.data.analytics.AnalyticsEvents
import com.droidnova.fliptomute.data.setup.AndroidSetupAccessObserver
import com.droidnova.fliptomute.notification.MonitoringNotificationManager
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import com.droidnova.fliptomute.utils.MonitoringLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FlipMonitoringService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @Inject lateinit var analyticsLogger: AnalyticsLogger
    @Inject lateinit var appPreferencesRepository: AppPreferencesRepository
    @Inject lateinit var cellularCallMonitorFactory: CellularCallMonitorFactory
    @Inject lateinit var deviceAdminCapabilityRepository: DeviceAdminCapabilityRepository
    @Inject lateinit var deviceOrientationMonitorFactory: DeviceOrientationMonitorFactory
    @Inject lateinit var flipFeedback: FlipFeedback
    @Inject lateinit var funnel: Funnel
    @Inject lateinit var flipStatsStore: FlipStatsStore
    @Inject lateinit var incomingCallVibrationControllerFactory: IncomingCallVibrationControllerFactory
    @Inject lateinit var interruptionAlertController: InterruptionAlertController
    @Inject lateinit var monitoringStateRepository: MonitoringStateRepository
    @Inject lateinit var proximityMonitorFactory: ProximityMonitorFactory
    @Inject lateinit var quickSettingsTileUpdateRequester: QuickSettingsTileUpdateRequester
    @Inject lateinit var reviewStore: ReviewStore
    @Inject lateinit var ringerModeControllerFactory: RingerModeControllerFactory
    @Inject lateinit var screenLockController: ScreenLockController
    @Inject lateinit var screenStateRepository: ScreenStateRepository
    @Inject lateinit var setupAccessRepository: SetupAccessRepository
    private val notificationHelper by lazy { MonitoringNotificationManager(this) }
    private var coordinator: FlipMonitoringCoordinator? = null
    private var commandJob: Job? = null
    private var foregroundStarted = false
    private val commandSequencer = MonitoringCommandSequencer()
    private var accessRevalidationJob: Job? = null
    /** Who started the current session; decides what a later failure may change (InterruptionPolicy). */
    private var sessionSource = MonitoringStartSource.USER
    /** Ends a timed pause; null when none is running. */
    private var timedPauseJob: Job? = null
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
                if (isTimedPauseActive()) {
                    endTimedPause()
                } else {
                    startMonitoring(
                        isRestart = false,
                        isResume = monitoringStateRepository.state.value is MonitoringRuntimeState.Paused,
                        startId = startId,
                        source = MonitoringStartSource.fromName(intent?.getStringExtra(EXTRA_START_SOURCE)),
                    )
                }
                START_STICKY
            }
            MonitoringServiceCommand.PAUSE -> {
                // An open-ended pause replaces a timed one
                if (isTimedPauseActive()) endTimedPause()
                pauseMonitoring(startId)
                START_NOT_STICKY
            }
            MonitoringServiceCommand.PAUSE_UNTIL -> {
                timedPause(intent?.getLongExtra(EXTRA_PAUSE_UNTIL, 0L) ?: 0L)
                START_STICKY
            }
            MonitoringServiceCommand.RESUME -> {
                if (isTimedPauseActive()) {
                    endTimedPause()
                } else {
                    startMonitoring(isRestart = false, isResume = true, startId = startId)
                }
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
                startMonitoring(
                    isRestart = true,
                    isResume = false,
                    startId = startId,
                    source = MonitoringStartSource.SYSTEM_RESTART,
                )
                START_STICKY
            }
            MonitoringServiceCommand.UNKNOWN -> {
                stopMonitoring(startId)
                START_NOT_STICKY
            }
        }
    }

    private fun startMonitoring(
        isRestart: Boolean,
        isResume: Boolean,
        startId: Int,
        source: MonitoringStartSource = MonitoringStartSource.USER,
    ) {
        debugLog("Start command received from ${source.name}")
        val runtime = monitoringStateRepository.state.value
        if (runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Resuming ||
            runtime is MonitoringRuntimeState.Active
        ) return
        sessionSource = source
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
                val storedPreferences = appPreferencesRepository.preferences.first()
                val recoveryController = ringerModeControllerFactory.create()
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
                val setup = setupAccessRepository.refreshAndGet()
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
                    appPreferencesRepository,
                    cellularCallMonitorFactory.create(),
                    deviceOrientationMonitorFactory.create(),
                    ringerModeControllerFactory.create(),
                    incomingCallVibrationControllerFactory.create(),
                    serviceScope,
                    onFailure = { reason ->
                        serviceScope.launch {
                            if (!commandSequencer.isCurrent(token)) return@launch
                            if (reason == MonitoringFailure.SETUP_REQUIRED) {
                                requestAccessRevalidation()
                            } else if (isResume) {
                                failResume(reason)
                            } else {
                                failStart(reason, phase = FailurePhase.RUNNING)
                            }
                        }
                    },
                    proximityMonitor = proximityMonitorFactory.create(),
                    debugLog = ::debugLog,
                    deviceAdminRepository = deviceAdminCapabilityRepository,
                    screenLockController = screenLockController,
                    screenStateRepository = screenStateRepository,
                    monitoringStateRepository = monitoringStateRepository,
                    flipFeedback = flipFeedback,
                    onFlipApplied = ::onFlipApplied,
                )
                debugLog("Starting cellular call monitor")
                when (val result = coordinator?.startAndAwaitReady()) {
                    MonitoringCoordinatorStartResult.Started -> {
                        debugLog("Coordinator startup result: Started")
                        if (!commandSequencer.isCurrent(token)) return@launch
                        if (!setupAccessRepository.refreshAndGet().isSetupComplete) {
                            commandSequencer.supersede()
                            failStart(MonitoringFailure.SETUP_REQUIRED, startId)
                            return@launch
                        }
                        try {
                            appPreferencesRepository.setMonitoringPaused(false)
                            appPreferencesRepository.setMonitoringEnabled(true)
                        } catch (error: Exception) {
                            if (error is CancellationException) throw error
                            MonitoringLog.failure(this@FlipMonitoringService, "Saving monitoring preference failed", error)
                            failStart(MonitoringFailure.UNKNOWN, startId)
                            return@launch
                        }
                        if (!commandSequencer.isCurrent(token)) return@launch
                        publishMonitoringState(MonitoringRuntimeState.Active)
                        debugLog("Runtime state changed to Active")
                        interruptionAlertController.clearStoppedAlert()
                        flipStatsStore.recordOn()
                        logEvent(
                            AnalyticsEvents.SERVICE_STATE_CHANGED,
                            AnalyticsEvents.PARAM_STATE to AnalyticsEvents.STATE_ON,
                            AnalyticsEvents.PARAM_SOURCE to source.analyticsName,
                        )
                        if (source != MonitoringStartSource.USER) {
                            logEvent(
                                AnalyticsEvents.AUTO_RESUME,
                                AnalyticsEvents.PARAM_SOURCE to source.analyticsName,
                                AnalyticsEvents.PARAM_SUCCESS to "true",
                            )
                        }
                        // A timed pause that was running when the process died continues until its end
                        val savedPauseEnd = appPreferencesRepository.preferences.first().pauseUntilEpochMs
                        if (savedPauseEnd != null) {
                            if (savedPauseEnd > System.currentTimeMillis()) {
                                enterTimedPause(savedPauseEnd, persist = false)
                            } else {
                                appPreferencesRepository.setPauseUntil(null)
                            }
                        }
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
        // The user turned it off, so an earlier "stopped" alert no longer applies
        interruptionAlertController.clearStoppedAlert()
        logEvent(
            AnalyticsEvents.SERVICE_STATE_CHANGED,
            AnalyticsEvents.PARAM_STATE to AnalyticsEvents.STATE_OFF,
            AnalyticsEvents.PARAM_SOURCE to AnalyticsEvents.SOURCE_USER,
        )
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
            val runtime = monitoringStateRepository.state.value
            if (!shouldShutdownForAccessLoss(runtime, setupComplete = false)) return@launch
            val accessComplete = try {
                setupAccessRepository.refreshAndGet().isSetupComplete
            } catch (error: RuntimeException) {
                MonitoringLog.failure(this@FlipMonitoringService, "Refreshing setup access failed", error)
                false
            }
            if (!shouldShutdownForAccessLoss(monitoringStateRepository.state.value, accessComplete)) {
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

    // --- Timed pause (M5-06) ---
    // The service stays in the foreground and the coordinator keeps listening, but ringing calls are
    // ignored until the end time. No background start is needed to come back, and a call after the
    // end time is handled even if the timer below runs late.

    private fun isTimedPauseActive(): Boolean =
        monitoringStateRepository.pausedUntil.value != null && coordinator != null && foregroundStarted

    private fun timedPause(untilEpochMs: Long) {
        if (monitoringStateRepository.state.value !is MonitoringRuntimeState.Active || coordinator == null) return
        if (untilEpochMs <= System.currentTimeMillis()) return
        enterTimedPause(untilEpochMs, persist = true)
        logEvent(
            AnalyticsEvents.SERVICE_STATE_CHANGED,
            AnalyticsEvents.PARAM_STATE to AnalyticsEvents.STATE_PAUSED,
            AnalyticsEvents.PARAM_SOURCE to AnalyticsEvents.SOURCE_USER,
            AnalyticsEvents.PARAM_TIMED to "true",
        )
    }

    private fun enterTimedPause(untilEpochMs: Long, persist: Boolean) {
        coordinator?.ignoreCallsUntil(untilEpochMs)
        monitoringStateRepository.updatePausedUntil(untilEpochMs)
        publishMonitoringState(MonitoringRuntimeState.Paused)
        notificationHelper.updateForegroundNotification(pausedUntilEpochMs = untilEpochMs)
        if (persist) serviceScope.launch { appPreferencesRepository.setPauseUntil(untilEpochMs) }
        timedPauseJob?.cancel()
        timedPauseJob = serviceScope.launch {
            delay((untilEpochMs - System.currentTimeMillis()).coerceAtLeast(0L))
            timedPauseJob = null
            endTimedPause()
        }
        debugLog("Timed pause until $untilEpochMs")
    }

    private fun endTimedPause() {
        timedPauseJob?.cancel()
        timedPauseJob = null
        coordinator?.ignoreCallsUntil(null)
        monitoringStateRepository.updatePausedUntil(null)
        if (monitoringStateRepository.state.value is MonitoringRuntimeState.Paused) {
            publishMonitoringState(MonitoringRuntimeState.Active)
        }
        notificationHelper.updateForegroundNotification(pausedUntilEpochMs = null)
        serviceScope.launch { appPreferencesRepository.setPauseUntil(null) }
        debugLog("Timed pause ended")
    }

    /** The service is stopping or pausing for good: forget any timed pause in memory. */
    private fun clearTimedPauseInMemory() {
        timedPauseJob?.cancel()
        timedPauseJob = null
        monitoringStateRepository.updatePausedUntil(null)
    }

    private fun pauseMonitoring(startId: Int) {
        val runtime = monitoringStateRepository.state.value
        if (runtime is MonitoringRuntimeState.Paused || runtime is MonitoringRuntimeState.Pausing) return
        commandSequencer.supersede()
        commandJob?.cancel()
        publishMonitoringState(MonitoringRuntimeState.Pausing)
        logEvent(
            AnalyticsEvents.SERVICE_STATE_CHANGED,
            AnalyticsEvents.PARAM_STATE to AnalyticsEvents.STATE_PAUSED,
            AnalyticsEvents.PARAM_SOURCE to AnalyticsEvents.SOURCE_USER,
        )
        coordinator?.beginStopping()
        commandJob = serviceScope.launch { finishPaused() }
    }

    private suspend fun finishPaused(startId: Int = commandSequencer.latestStartId) {
        clearTimedPauseInMemory()
        try {
            appPreferencesRepository.setPauseUntil(null)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            MonitoringLog.failure(this, "Clearing timed pause failed", error)
        }
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

    private suspend fun failStart(
        reason: MonitoringFailure,
        startId: Int = commandSequencer.latestStartId,
        phase: FailurePhase = FailurePhase.STARTING,
    ) {
        val source = sessionSource
        val decision = InterruptionPolicy.decide(source, phase, reason)
        debugLog("Monitoring failure ${reason.name} from ${source.name} while ${phase.name}: ${decision.name}")
        publishMonitoringState(MonitoringRuntimeState.Error(reason))
        debugLog("Runtime state changed to Error")
        logEvent(
            AnalyticsEvents.SERVICE_INTERRUPTED,
            AnalyticsEvents.PARAM_REASON to reason.name.lowercase(),
            AnalyticsEvents.PARAM_SOURCE to source.analyticsName,
            AnalyticsEvents.PARAM_PHASE to phase.name.lowercase(),
        )
        if (source != MonitoringStartSource.USER && phase == FailurePhase.STARTING) {
            logEvent(
                AnalyticsEvents.AUTO_RESUME,
                AnalyticsEvents.PARAM_SOURCE to source.analyticsName,
                AnalyticsEvents.PARAM_SUCCESS to "false",
            )
        }
        when (decision) {
            InterruptionDecision.TURN_OFF ->
                finishStopped(writePreference = true, preserveError = true, failure = reason, startId = startId)
            InterruptionDecision.KEEP_ON_AND_ALERT -> {
                // Keep the user's "on" choice: the app restarts it on the next open, health check or tap on the alert
                finishStopped(writePreference = false, preserveError = true, failure = reason, startId = startId)
                interruptionAlertController.showStoppedAlert()
            }
        }
    }

    private suspend fun finishStopped(
        writePreference: Boolean,
        preserveError: Boolean = false,
        failure: MonitoringFailure? = null,
        startId: Int = commandSequencer.latestStartId,
    ) {
        debugLog("Service cleanup started")
        clearTimedPauseInMemory()
        val restoration = coordinator?.stop()
        coordinator = null
        var persistenceFailed = false
        if (writePreference) {
            try {
                appPreferencesRepository.setMonitoringEnabled(false)
                appPreferencesRepository.setMonitoringPaused(false)
                appPreferencesRepository.setPauseUntil(null)
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
        val runtime = monitoringStateRepository.state.value
        if (runtime !is MonitoringRuntimeState.Error && runtime !is MonitoringRuntimeState.Paused) {
            publishMonitoringState(MonitoringRuntimeState.Stopped)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        internal const val EXTRA_START_SOURCE = "com.droidnova.fliptomute.extra.START_SOURCE"

        fun createStartIntent(
            context: Context,
            source: MonitoringStartSource = MonitoringStartSource.USER,
        ) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.START_ACTION)
            .putExtra(EXTRA_START_SOURCE, source.name)
        fun createStopIntent(context: Context) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.STOP_ACTION)
        internal const val EXTRA_PAUSE_UNTIL = "com.droidnova.fliptomute.extra.PAUSE_UNTIL"

        fun createPauseUntilIntent(context: Context, untilEpochMs: Long) = Intent(context, FlipMonitoringService::class.java)
            .setAction(MonitoringServiceCommandClassifier.PAUSE_UNTIL_ACTION)
            .putExtra(EXTRA_PAUSE_UNTIL, untilEpochMs)

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

    private fun onFlipApplied(event: FlipAppliedEvent) {
        logEvent(
            AnalyticsEvents.FLIP_APPLIED,
            AnalyticsEvents.PARAM_ACTION to if (event.action == FlipAction.VIBRATE) "vibrate" else "silence",
            AnalyticsEvents.PARAM_FLAT_ONLY to event.flatOnly.toString(),
            AnalyticsEvents.PARAM_POCKET_PROTECTION to event.pocketProtection.toString(),
        )
        try {
            flipStatsStore.recordFlip()
            funnel.flipApplied()
            // A call silenced or vibrated by a flip: the moment the review policy counts (M6-07)
            reviewStore.recordValueMoment()
        } catch (error: RuntimeException) {
            MonitoringLog.failure(this, "Funnel update failed", error)
        }
    }

    /** Analytics must never stop the service from working. */
    private fun logEvent(event: String, vararg params: Pair<String, String>) {
        try {
            analyticsLogger.log(event, params.toMap())
        } catch (error: RuntimeException) {
            MonitoringLog.failure(this, "Analytics event failed", error)
        }
    }

    private fun publishMonitoringState(state: MonitoringRuntimeState) {
        monitoringStateRepository.updateState(state)
        quickSettingsTileUpdateRequester.requestUpdate()
    }

    private suspend fun persistPausedIntentOrFallbackOff(): Boolean = try {
        appPreferencesRepository.setMonitoringEnabled(true)
        appPreferencesRepository.setMonitoringPaused(true)
        true
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        MonitoringLog.failure(this, "Saving paused monitoring preference failed", error)
        tryPersistOffAgain()
        false
    }

    private suspend fun tryPersistOffAgain() {
        try {
            appPreferencesRepository.setMonitoringEnabled(false)
            appPreferencesRepository.setMonitoringPaused(false)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            MonitoringLog.failure(this, "Repairing stopped monitoring preference failed", error)
        }
    }

    private fun RingerModeResult?.isActualRestorationFailure(): Boolean =
        this is RingerModeResult.Failure && reason != RingerModeFailure.NO_ACTIVE_CHANGE

}
