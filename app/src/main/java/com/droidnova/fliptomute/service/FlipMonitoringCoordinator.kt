package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.RingerModeController
import com.droidnova.fliptomute.audio.RingerModeResult
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.DeviceOrientationMonitor
import com.droidnova.fliptomute.sensor.FaceDownDetectionState
import com.droidnova.fliptomute.telephony.CellularCallMonitor
import com.droidnova.fliptomute.telephony.CellularCallMonitorState
import com.droidnova.fliptomute.telephony.CellularCallState
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FlipMonitoringCoordinator(
    private val preferencesRepository: AppPreferencesRepository,
    private val callMonitor: CellularCallMonitor,
    private val orientationMonitor: DeviceOrientationMonitor,
    private val ringerModeController: RingerModeController,
    private val scope: CoroutineScope,
    private val onReady: () -> Unit,
    private val onFailure: (MonitoringFailure) -> Unit,
) {
    private val mutex = Mutex()
    private var latestAction = FlipAction.SILENT
    private var ringingSession: RingingSession? = null
    private var started = false
    private var stopping = false
    private var readyReported = false
    private var preferencesJob: Job? = null
    private var callJob: Job? = null
    private var orientationJob: Job? = null

    fun start() {
        if (started) return
        started = true
        stopping = false
        preferencesJob = scope.launch {
            preferencesRepository.preferences.collectLatest { latestAction = it.selectedFlipAction }
        }
        orientationJob = scope.launch { orientationMonitor.state.collectLatest(::handleOrientationState) }
        callJob = scope.launch { callMonitor.state.collectLatest(::handleCallState) }
        callMonitor.start()
    }

    fun beginStopping() {
        if (stopping && !started) return
        stopping = true
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
                    onReady()
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
                    orientationMonitor.stop()
                    ringerModeController.restorePreviousMode()
                    ringingSession = null
                }
            }
            CellularCallMonitorState.PermissionRequired -> fail(MonitoringFailure.SETUP_REQUIRED)
            CellularCallMonitorState.TelephonyUnavailable -> fail(MonitoringFailure.TELEPHONY_UNAVAILABLE)
            is CellularCallMonitorState.Error -> fail(MonitoringFailure.CALL_MONITOR_FAILED)
            CellularCallMonitorState.Stopped -> Unit
        }
    }

    private suspend fun handleOrientationState(state: FaceDownDetectionState) = mutex.withLock {
        if (stopping || !started) return@withLock
        when (state) {
            is FaceDownDetectionState.Detecting -> {
                val session = ringingSession ?: return@withLock
                if (state.orientation != DeviceOrientation.FACE_DOWN || session.actionHandled) return@withLock
                when (ringerModeController.applyTemporaryAction(session.selectedAction)) {
                    is RingerModeResult.Success -> {
                        ringingSession = session.copy(actionHandled = true)
                        orientationMonitor.stop()
                    }
                    is RingerModeResult.Failure -> fail(MonitoringFailure.SOUND_CONTROL_FAILED)
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
        orientationMonitor.stop()
        callMonitor.stop()
        ringerModeController.restorePreviousMode()
        ringingSession = null
        onFailure(reason)
    }

    private data class RingingSession(
        val selectedAction: FlipAction,
        val actionHandled: Boolean = false,
    )
}
