package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.RingerModeController
import com.droidnova.fliptomute.audio.RingerModeResult
import com.droidnova.fliptomute.audio.DeviceRingerMode
import com.droidnova.fliptomute.audio.IncomingCallVibrationController
import com.droidnova.fliptomute.audio.IncomingCallVibrationResult
import com.droidnova.fliptomute.audio.VibrationAvailability
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.DeviceOrientationMonitor
import com.droidnova.fliptomute.sensor.FaceDownDetectionState
import com.droidnova.fliptomute.telephony.CellularCallMonitor
import com.droidnova.fliptomute.telephony.CellularCallMonitorState
import com.droidnova.fliptomute.telephony.CellularCallState
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class FlipMonitoringCoordinator(
    private val preferencesRepository: AppPreferencesRepository,
    private val callMonitor: CellularCallMonitor,
    private val orientationMonitor: DeviceOrientationMonitor,
    private val ringerModeController: RingerModeController,
    private val vibrationController: IncomingCallVibrationController,
    private val scope: CoroutineScope,
    private val onFailure: (MonitoringFailure) -> Unit,
) {
    constructor(
        preferencesRepository: AppPreferencesRepository,
        callMonitor: CellularCallMonitor,
        orientationMonitor: DeviceOrientationMonitor,
        ringerModeController: RingerModeController,
        scope: CoroutineScope,
        onFailure: (MonitoringFailure) -> Unit,
    ) : this(
        preferencesRepository, callMonitor, orientationMonitor, ringerModeController,
        object : IncomingCallVibrationController {
            override fun getAvailability() = VibrationAvailability.UNAVAILABLE
            override fun start() = IncomingCallVibrationResult.Failed(
                com.droidnova.fliptomute.audio.IncomingCallVibrationFailure.VIBRATOR_SERVICE_UNAVAILABLE,
            )
            override fun stop() = Unit
        }, scope, onFailure,
    )
    private val mutex = Mutex()
    private var latestAction = FlipAction.SILENT
    private var ringingSession: RingingSession? = null
    private var started = false
    private var stopping = false
    private var readyReported = false
    private var preferencesJob: Job? = null
    private var callJob: Job? = null
    private var orientationJob: Job? = null
    private var startupResult = CompletableDeferred<MonitoringCoordinatorStartResult>()

    suspend fun startAndAwaitReady(): MonitoringCoordinatorStartResult {
        if (started) {
            return if (readyReported) MonitoringCoordinatorStartResult.Started else startupResult.await()
        }
        started = true
        stopping = false
        startupResult = CompletableDeferred()
        preferencesJob = scope.launch {
            preferencesRepository.preferences.collectLatest { latestAction = it.selectedFlipAction }
        }
        orientationJob = scope.launch { orientationMonitor.state.collectLatest(::handleOrientationState) }
        callJob = scope.launch { callMonitor.state.collectLatest(::handleCallState) }
        callMonitor.start()
        mapStartupState(callMonitor.state.value)?.let { result ->
            if (result is MonitoringCoordinatorStartResult.Started) readyReported = true
            startupResult.complete(result)
        }
        val result = withTimeoutOrNull(MONITORING_START_TIMEOUT_MILLIS) { startupResult.await() }
            ?: MonitoringCoordinatorStartResult.Failed(MonitoringFailure.CALL_MONITOR_FAILED)
        if (result is MonitoringCoordinatorStartResult.Failed) beginStopping()
        return result
    }

    fun beginStopping() {
        if (stopping && !started) return
        stopping = true
        vibrationController.stop()
        orientationMonitor.stop()
        callMonitor.stop()
        ringingSession = null
        started = false
        preferencesJob?.cancel()
        callJob?.cancel()
        orientationJob?.cancel()
        readyReported = false
    }

    suspend fun stop() {
        beginStopping()
        ringerModeController.restorePreviousMode()
    }

    private suspend fun handleCallState(state: CellularCallMonitorState) = mutex.withLock {
        if (stopping || !started) return@withLock
        when (state) {
            is CellularCallMonitorState.Listening -> {
                if (!readyReported) {
                    readyReported = true
                    startupResult.complete(MonitoringCoordinatorStartResult.Started)
                }
                if (state.callState == CellularCallState.RINGING) {
                    if (ringingSession == null) {
                        if (!orientationMonitor.isSensorAvailable) {
                            fail(MonitoringFailure.SENSOR_UNAVAILABLE)
                        } else {
                            ringingSession = RingingSession(latestAction)
                            orientationMonitor.start()
                        }
                    }
                } else if (ringingSession != null) {
                    vibrationController.stop()
                    orientationMonitor.stop()
                    ringerModeController.restorePreviousMode()
                    ringingSession = null
                }
            }
            CellularCallMonitorState.PermissionRequired -> reportFailure(MonitoringFailure.SETUP_REQUIRED)
            CellularCallMonitorState.TelephonyUnavailable -> reportFailure(MonitoringFailure.TELEPHONY_UNAVAILABLE)
            is CellularCallMonitorState.Error -> reportFailure(MonitoringFailure.CALL_MONITOR_FAILED)
            CellularCallMonitorState.Stopped -> Unit
        }
    }

    private suspend fun handleOrientationState(state: FaceDownDetectionState) = mutex.withLock {
        if (stopping || !started) return@withLock
        when (state) {
            is FaceDownDetectionState.Detecting -> {
                val session = ringingSession ?: return@withLock
                if (state.orientation != DeviceOrientation.FACE_DOWN || session.actionHandled) return@withLock
                if (session.selectedAction == FlipAction.SILENT) vibrationController.stop()
                val result = ringerModeController.applyTemporaryAction(session.selectedAction)
                when (result) {
                    is RingerModeResult.Success -> {
                        val handled = if (session.selectedAction == FlipAction.VIBRATE) {
                            result.currentMode == DeviceRingerMode.VIBRATE &&
                                vibrationController.getAvailability() == VibrationAvailability.AVAILABLE &&
                                vibrationController.start() is IncomingCallVibrationResult.Started
                        } else true
                        if (!handled) {
                            vibrationController.stop()
                            ringerModeController.applyTemporaryAction(FlipAction.SILENT)
                        }
                        ringingSession = session.copy(actionHandled = true)
                        orientationMonitor.stop()
                    }
                    is RingerModeResult.Failure -> {
                        if (session.selectedAction == FlipAction.VIBRATE) {
                            vibrationController.stop()
                            when (ringerModeController.applyTemporaryAction(FlipAction.SILENT)) {
                                is RingerModeResult.Success -> {
                                    ringingSession = session.copy(actionHandled = true)
                                    orientationMonitor.stop()
                                }
                                is RingerModeResult.Failure -> fail(MonitoringFailure.SOUND_CONTROL_FAILED)
                            }
                        } else fail(MonitoringFailure.SOUND_CONTROL_FAILED)
                    }
                }
            }
            FaceDownDetectionState.SensorUnavailable,
            FaceDownDetectionState.Error,
            -> if (ringingSession != null) fail(MonitoringFailure.SENSOR_UNAVAILABLE)
            is FaceDownDetectionState.Idle -> Unit
        }
    }

    private suspend fun fail(reason: MonitoringFailure) {
        if (stopping) return
        stopping = true
        vibrationController.stop()
        orientationMonitor.stop()
        callMonitor.stop()
        ringerModeController.restorePreviousMode()
        ringingSession = null
        onFailure(reason)
    }

    private suspend fun reportFailure(reason: MonitoringFailure) {
        if (!readyReported) {
            startupResult.complete(MonitoringCoordinatorStartResult.Failed(reason))
        } else {
            fail(reason)
        }
    }

    private fun mapStartupState(state: CellularCallMonitorState): MonitoringCoordinatorStartResult? = when (state) {
        is CellularCallMonitorState.Listening -> MonitoringCoordinatorStartResult.Started
        CellularCallMonitorState.PermissionRequired ->
            MonitoringCoordinatorStartResult.Failed(MonitoringFailure.SETUP_REQUIRED)
        CellularCallMonitorState.TelephonyUnavailable ->
            MonitoringCoordinatorStartResult.Failed(MonitoringFailure.TELEPHONY_UNAVAILABLE)
        is CellularCallMonitorState.Error ->
            MonitoringCoordinatorStartResult.Failed(MonitoringFailure.CALL_MONITOR_FAILED)
        CellularCallMonitorState.Stopped -> null
    }

    private data class RingingSession(
        val selectedAction: FlipAction,
        val actionHandled: Boolean = false,
    )

    private companion object { const val MONITORING_START_TIMEOUT_MILLIS = 10_000L }
}
