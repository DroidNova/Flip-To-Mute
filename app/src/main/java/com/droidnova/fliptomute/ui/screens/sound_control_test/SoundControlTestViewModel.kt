package com.droidnova.fliptomute.ui.screens.sound_control_test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.audio.DeviceRingerMode
import com.droidnova.fliptomute.audio.RingerModeController
import com.droidnova.fliptomute.audio.RingerModeFailure
import com.droidnova.fliptomute.audio.RingerModeResult
import com.droidnova.fliptomute.audio.RingerModeSuccessType
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SoundControlTestViewModel(
    preferencesRepository: AppPreferencesRepository,
    private val setupAccessRepository: SetupAccessRepository,
    private val ringerModeController: RingerModeController,
) : ViewModel() {
    private val runtimeState = MutableStateFlow(
        RuntimeState(currentMode = ringerModeController.getCurrentMode()),
    )
    private var countdownJob: Job? = null

    val uiState: StateFlow<SoundControlTestUiState> = combine(
        preferencesRepository.preferences,
        setupAccessRepository.accessState,
        runtimeState,
    ) { preferences, accessState, runtime ->
        SoundControlTestUiState(
            selectedAction = preferences.selectedFlipAction,
            currentMode = runtime.currentMode,
            isTestRunning = runtime.isTestRunning,
            remainingSeconds = runtime.remainingSeconds,
            result = runtime.result,
            hasSoundControlAccess = accessState.soundControlStatus == SetupAccessStatus.GRANTED,
            canChangeSoundMode = runtime.canChangeSoundMode,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SoundControlTestUiState(currentMode = ringerModeController.getCurrentMode()),
    )

    fun startTest() {
        if (runtimeState.value.isTestRunning) return
        setupAccessRepository.refresh()
        when (val result = ringerModeController.applyTemporaryAction(uiState.value.selectedAction)) {
            is RingerModeResult.Success -> {
                if (result.type == RingerModeSuccessType.NO_CHANGE) {
                    runtimeState.value = RuntimeState(
                        currentMode = result.currentMode,
                        result = SoundControlTestResult.ALREADY_SET,
                    )
                } else {
                    runtimeState.value = RuntimeState(
                        currentMode = result.currentMode,
                        isTestRunning = true,
                        remainingSeconds = TEST_DURATION_SECONDS,
                        result = SoundControlTestResult.TEST_STARTED,
                    )
                    startCountdown()
                }
            }
            is RingerModeResult.Failure -> applyFailure(result.reason)
        }
    }

    fun restoreNow() {
        countdownJob?.cancel()
        countdownJob = null
        applyRestoreResult(ringerModeController.restorePreviousMode())
    }

    fun refreshCurrentMode() {
        setupAccessRepository.refresh()
        runtimeState.update { it.copy(currentMode = ringerModeController.getCurrentMode()) }
    }

    fun onScreenLeaving() {
        countdownJob?.cancel()
        countdownJob = null
        ringerModeController.restorePreviousMode()
        runtimeState.value = RuntimeState(currentMode = ringerModeController.getCurrentMode())
    }

    override fun onCleared() {
        countdownJob?.cancel()
        ringerModeController.restorePreviousMode()
        super.onCleared()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            repeat(TEST_DURATION_SECONDS) {
                delay(ONE_SECOND_MILLIS)
                runtimeState.update { state -> state.copy(remainingSeconds = state.remainingSeconds - 1) }
            }
            applyRestoreResult(ringerModeController.restorePreviousMode())
            countdownJob = null
        }
    }

    private fun applyRestoreResult(result: RingerModeResult) {
        when (result) {
            is RingerModeResult.Success -> runtimeState.value = RuntimeState(
                currentMode = result.currentMode,
                result = if (result.type == RingerModeSuccessType.MANUAL_CHANGE_PRESERVED) {
                    SoundControlTestResult.MANUAL_CHANGE_PRESERVED
                } else {
                    SoundControlTestResult.RESTORED
                },
            )
            is RingerModeResult.Failure -> applyFailure(result.reason)
        }
    }

    private fun applyFailure(failure: RingerModeFailure) {
        runtimeState.value = RuntimeState(
            currentMode = ringerModeController.getCurrentMode(),
            result = when (failure) {
                RingerModeFailure.SOUND_CONTROL_ACCESS_REQUIRED -> SoundControlTestResult.ACCESS_REQUIRED
                RingerModeFailure.FIXED_VOLUME_DEVICE,
                RingerModeFailure.AUDIO_SERVICE_UNAVAILABLE,
                -> SoundControlTestResult.DEVICE_NOT_SUPPORTED
                else -> SoundControlTestResult.CHANGE_FAILED
            },
            canChangeSoundMode = failure != RingerModeFailure.FIXED_VOLUME_DEVICE &&
                failure != RingerModeFailure.AUDIO_SERVICE_UNAVAILABLE,
        )
    }

    private data class RuntimeState(
        val currentMode: DeviceRingerMode,
        val isTestRunning: Boolean = false,
        val remainingSeconds: Int = 0,
        val result: SoundControlTestResult? = null,
        val canChangeSoundMode: Boolean = true,
    )

    private companion object {
        const val TEST_DURATION_SECONDS = 5
        const val ONE_SECOND_MILLIS = 1_000L
    }
}
