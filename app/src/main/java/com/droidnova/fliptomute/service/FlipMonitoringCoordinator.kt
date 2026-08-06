package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.audio.RingerModeController
import com.droidnova.fliptomute.audio.RingerModeResult
import com.droidnova.fliptomute.audio.DeviceRingerMode
import com.droidnova.fliptomute.audio.IncomingCallVibrationController
import com.droidnova.fliptomute.audio.IncomingCallVibrationResult
import com.droidnova.fliptomute.audio.VibrationAvailability
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.preferences.CallActionSelection
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.DeviceOrientationMonitor
import com.droidnova.fliptomute.sensor.FaceDownDetectionState
import com.droidnova.fliptomute.sensor.FlatSurfaceFlipGate
import com.droidnova.fliptomute.sensor.FlatSurfaceFlipResult
import com.droidnova.fliptomute.sensor.PocketProtectionDecision
import com.droidnova.fliptomute.sensor.PocketProtectionGate
import com.droidnova.fliptomute.sensor.ProximityMonitor
import com.droidnova.fliptomute.sensor.ProximityMonitorState
import com.droidnova.fliptomute.telephony.CellularCallMonitor
import com.droidnova.fliptomute.telephony.CellularCallMonitorState
import com.droidnova.fliptomute.telephony.CellularCallState
import com.droidnova.fliptomute.ui.screens.home.FlipAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
    private val proximityMonitor: ProximityMonitor? = null,
    private val debugLog: (String) -> Unit = {},
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
    private var latestPreferences = AppPreferences()
    private val flatSurfaceFlipGate = FlatSurfaceFlipGate()
    private val pocketProtectionGate = PocketProtectionGate()
    private var ringingSession: RingingSession? = null
    private var started = false
    private var stopping = false
    private var readyReported = false
    private var preferencesJob: Job? = null
    private var callJob: Job? = null
    private var orientationJob: Job? = null
    private var proximityJob: Job? = null
    private var pocketTimeoutJob: Job? = null
    private var startupResult = CompletableDeferred<MonitoringCoordinatorStartResult>()

    suspend fun startAndAwaitReady(): MonitoringCoordinatorStartResult {
        if (started) {
            return if (readyReported) MonitoringCoordinatorStartResult.Started else startupResult.await()
        }
        started = true
        stopping = false
        startupResult = CompletableDeferred()
        preferencesJob = scope.launch {
            preferencesRepository.preferences.collectLatest { latestPreferences = it }
        }
        orientationJob = scope.launch { orientationMonitor.state.collectLatest(::handleOrientationState) }
        proximityJob = scope.launch { proximityMonitor?.state?.collectLatest(::handleProximityState) }
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
        stopPocketMonitoring()
        flatSurfaceFlipGate.reset()
        callMonitor.stop()
        ringingSession = null
        started = false
        preferencesJob?.cancel()
        callJob?.cancel()
        orientationJob?.cancel()
        proximityJob?.cancel()
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
                            flatSurfaceFlipGate.reset()
                            pocketProtectionGate.reset()
                            val pocketEnabled = latestPreferences.pocketProtectionEnabled &&
                                proximityMonitor?.isSensorAvailable == true
                            debugLog("Pocket protection enabled=$pocketEnabled; sensor available=${proximityMonitor?.isSensorAvailable == true}")
                            ringingSession = RingingSession(
                                latestPreferences.callActionSelection,
                                latestPreferences.requireFlatSurfaceBeforeFlip,
                                if (pocketEnabled) PocketProtectionDecision.WAITING else PocketProtectionDecision.CLEAR,
                            )
                            orientationMonitor.start()
                            if (pocketEnabled) {
                                proximityMonitor?.start()
                                debugLog("Proximity monitor started")
                                pocketTimeoutJob = scope.launch {
                                    delay(POCKET_DECISION_TIMEOUT_MILLIS)
                                    handlePocketTimeout()
                                }
                            }
                        }
                    }
                } else if (ringingSession != null) {
                    vibrationController.stop()
                    orientationMonitor.stop()
                    stopPocketMonitoring()
                    flatSurfaceFlipGate.reset()
                    pocketProtectionGate.reset()
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
                if (session.actionHandled) return@withLock
                var currentSession = session
                if (session.pocketDecision == PocketProtectionDecision.WAITING) {
                    val decision = pocketProtectionGate.onOrientationSample(
                        state.orientation, state.normalizedZ, state.gravityMagnitude, state.timestampNanos,
                    )
                    updatePocketDecision(decision)
                    currentSession = ringingSession ?: return@withLock
                }
                if (currentSession.pocketDecision != PocketProtectionDecision.CLEAR) return@withLock
                val allowed = if (currentSession.requireFlatSurfaceBeforeFlip) {
                    flatSurfaceFlipGate.onSample(
                        state.orientation,
                        state.normalizedZ,
                        state.gravityMagnitude,
                        state.timestampNanos,
                    ) == FlatSurfaceFlipResult.Allowed
                } else {
                    state.orientation == DeviceOrientation.FACE_DOWN
                }
                if (!allowed) return@withLock
                if (!currentSession.selection.vibratePhone) vibrationController.stop()
                val modeAction = if (currentSession.selection.muteRingtone) FlipAction.SILENT else FlipAction.VIBRATE
                val result = ringerModeController.applyTemporaryAction(modeAction)
                when (result) {
                    is RingerModeResult.Success -> {
                        val handled = if (currentSession.selection.vibratePhone) {
                            (currentSession.selection.muteRingtone || result.currentMode == DeviceRingerMode.VIBRATE) &&
                                vibrationController.getAvailability() == VibrationAvailability.AVAILABLE &&
                                vibrationController.start() is IncomingCallVibrationResult.Started
                        } else true
                        if (!handled) {
                            vibrationController.stop()
                            ringerModeController.applyTemporaryAction(FlipAction.SILENT)
                        }
                        ringingSession = currentSession.copy(actionHandled = true)
                        orientationMonitor.stop()
                        flatSurfaceFlipGate.reset()
                    }
                    is RingerModeResult.Failure -> {
                        if (currentSession.selection.vibratePhone) {
                            vibrationController.stop()
                            when (ringerModeController.applyTemporaryAction(FlipAction.SILENT)) {
                                is RingerModeResult.Success -> {
                                    ringingSession = currentSession.copy(actionHandled = true)
                                    orientationMonitor.stop()
                                    flatSurfaceFlipGate.reset()
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

    private suspend fun handleProximityState(state: ProximityMonitorState) = mutex.withLock {
        val session = ringingSession ?: return@withLock
        if (session.pocketDecision != PocketProtectionDecision.WAITING) return@withLock
        val decision = when (state) {
            is ProximityMonitorState.Listening ->
                pocketProtectionGate.onProximityChanged(state.proximityState, System.nanoTime()).also {
                    debugLog("Initial proximity state ${state.proximityState.name.lowercase()}")
                }
            ProximityMonitorState.SensorUnavailable, ProximityMonitorState.Error ->
                pocketProtectionGate.onInitialTimeout().also { debugLog("Optional proximity sensor unavailable") }
            ProximityMonitorState.Stopped, ProximityMonitorState.WaitingForReading -> return@withLock
        }
        updatePocketDecision(decision)
    }

    private suspend fun handlePocketTimeout() = mutex.withLock {
        if (ringingSession?.pocketDecision == PocketProtectionDecision.WAITING) {
            debugLog("Initial proximity timeout")
            updatePocketDecision(pocketProtectionGate.onInitialTimeout())
        }
    }

    private fun updatePocketDecision(decision: PocketProtectionDecision) {
        if (decision == PocketProtectionDecision.WAITING) return
        val session = ringingSession ?: return
        ringingSession = session.copy(pocketDecision = decision)
        debugLog("Pocket decision ${decision.name.lowercase()}")
        pocketTimeoutJob?.cancel()
        pocketTimeoutJob = null
        proximityMonitor?.stop()
        debugLog("Proximity monitor stopped")
        if (decision == PocketProtectionDecision.BLOCKED) orientationMonitor.stop()
    }

    private fun stopPocketMonitoring() {
        pocketTimeoutJob?.cancel()
        pocketTimeoutJob = null
        proximityMonitor?.stop()
    }

    private suspend fun fail(reason: MonitoringFailure) {
        if (stopping) return
        stopping = true
        vibrationController.stop()
        orientationMonitor.stop()
        stopPocketMonitoring()
        flatSurfaceFlipGate.reset()
        pocketProtectionGate.reset()
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
        val selection: CallActionSelection,
        val requireFlatSurfaceBeforeFlip: Boolean,
        val pocketDecision: PocketProtectionDecision,
        val actionHandled: Boolean = false,
    )

    private companion object {
        const val MONITORING_START_TIMEOUT_MILLIS = 10_000L
        const val POCKET_DECISION_TIMEOUT_MILLIS = 600L
    }
}
