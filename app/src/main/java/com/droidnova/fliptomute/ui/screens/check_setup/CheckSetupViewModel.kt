package com.droidnova.fliptomute.ui.screens.check_setup

import androidx.lifecycle.ViewModel
import com.droidnova.fliptomute.data.reliability.BatteryOptimizationStatus
import com.droidnova.fliptomute.data.review.ReviewStore
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.ui.screens.sound_control_test.SoundControlTestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The five steps of "Check my setup", in order (design spec 4.8). */
enum class CheckStep { ACCESS, BATTERY, FLIP_SENSOR, SOUND, CALLS }

enum class StepResult { NOT_CHECKED, RUNNING, PASS, WARNING, FAIL, SKIPPED }

data class CheckSetupUiState(val results: Map<CheckStep, StepResult> = CheckStep.entries.associateWith { StepResult.NOT_CHECKED }) {
    operator fun get(step: CheckStep): StepResult = results.getValue(step)

    /** Problems, not warnings: the battery step can only warn. */
    val problems: Int get() = results.values.count { it == StepResult.FAIL }

    /** Every required step has a result; call detection is optional. */
    val finished: Boolean
        get() = results.filterKeys { it != CheckStep.CALLS }.values
            .none { it == StepResult.NOT_CHECKED || it == StepResult.RUNNING }
}

/**
 * "Check my setup" (M6-03): one guided flow instead of the three 1.x test screens (audit U8). The
 * sensor, sound and call steps reuse the existing test view models; this one collects the results,
 * runs the two automatic steps and builds the text for "Send report".
 */
@HiltViewModel
class CheckSetupViewModel @Inject constructor(
    private val setupAccessRepository: SetupAccessRepository,
    private val batteryOptimizationStatus: BatteryOptimizationStatus,
    private val reviewStore: ReviewStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CheckSetupUiState())
    val uiState: StateFlow<CheckSetupUiState> = mutableState.asStateFlow()
    private var cleanRunRecorded = false

    init {
        refreshAutomaticSteps()
    }

    /** Access and battery are read, not tested, so they refresh whenever the screen returns. */
    fun refreshAutomaticSteps() {
        val accessOk = setupAccessRepository.refreshAndGet().isSetupComplete
        val batteryRestricted = batteryOptimizationStatus.isIgnoringBatteryOptimizations() == false
        set(CheckStep.ACCESS, if (accessOk) StepResult.PASS else StepResult.FAIL)
        set(CheckStep.BATTERY, if (batteryRestricted) StepResult.WARNING else StepResult.PASS)
    }

    fun onStepStarted(step: CheckStep) = set(step, StepResult.RUNNING)

    fun onStepSkipped(step: CheckStep) = set(step, StepResult.SKIPPED)

    /** The flip sensor saw the phone face down, or there is no usable sensor. */
    fun onSensorResult(faceDownSeen: Boolean, sensorAvailable: Boolean) {
        when {
            !sensorAvailable -> set(CheckStep.FLIP_SENSOR, StepResult.FAIL)
            faceDownSeen -> set(CheckStep.FLIP_SENSOR, StepResult.PASS)
        }
    }

    fun onSoundResult(result: SoundControlTestResult?) {
        val mapped = when (result) {
            null -> return
            SoundControlTestResult.TEST_STARTED, SoundControlTestResult.RESTORED,
            SoundControlTestResult.ALREADY_SET, SoundControlTestResult.MANUAL_CHANGE_PRESERVED -> StepResult.PASS
            SoundControlTestResult.ACCESS_REQUIRED, SoundControlTestResult.DEVICE_NOT_SUPPORTED,
            SoundControlTestResult.CHANGE_FAILED -> StepResult.FAIL
        }
        set(CheckStep.SOUND, mapped)
    }

    fun onCallRinging() = set(CheckStep.CALLS, StepResult.PASS)

    fun onCallsUnavailable() = set(CheckStep.CALLS, StepResult.FAIL)

    /** One line per step, for the support email. */
    fun reportText(): String = CheckStep.entries.joinToString("\n") { step -> "${step.name}: ${uiState.value[step].name}" }

    private fun set(step: CheckStep, result: StepResult) {
        mutableState.update { it.copy(results = it.results + (step to result)) }
        val state = mutableState.value
        // A clean run is a moment the app visibly helped, for the review policy (M6-07)
        if (!cleanRunRecorded && state.finished && state.problems == 0) {
            cleanRunRecorded = true
            reviewStore.recordValueMoment()
        }
    }
}
