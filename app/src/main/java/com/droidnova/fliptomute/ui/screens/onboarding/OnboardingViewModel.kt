package com.droidnova.fliptomute.ui.screens.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.audio.FlipFeedback
import com.droidnova.fliptomute.data.analytics.Funnel
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.DeviceOrientationMonitor
import com.droidnova.fliptomute.sensor.FaceDownDetectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** In order, as Secret Calculator's onboarding steps (architecture A4). */
enum class OnboardingStep { WELCOME, ACCESS, TRY_IT }

/** The three accesses, in the order the Access step asks for them (design spec 4.2). */
enum class AccessStep { PHONE, NOTIFICATIONS, SOUND }

/** The practice flip on the last step. */
data class TryState(
    val faceDown: Boolean = false,
    /** A face-down flip was detected at least once. */
    val flipped: Boolean = false,
    /** 20 seconds passed without a flip: show "Place it flat on a table, screen down." */
    val showHint: Boolean = false,
)

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    /** Opened from Home, Settings or a test screen just to grant access: no Welcome, no Try it. */
    val accessOnly: Boolean = false,
    val access: SetupAccessState = SetupAccessState(),
    /** The step the primary button asks for next, or null when all access is granted. */
    val nextAccess: AccessStep? = null,
    /** The next access was refused for good, so the button opens system settings instead. */
    val nextNeedsSettings: Boolean = false,
    /** Cellular calling is missing: Flip to Mute cannot work on this device. */
    val unsupported: Boolean = false,
    val showSoundHint: Boolean = false,
    val tryState: TryState = TryState(),
    @StringRes val message: Int? = null,
)

/** What the activity must launch for the primary Access button. */
sealed interface AccessCommand {
    data class RequestPermission(val permission: RuntimeSetupPermission) : AccessCommand
    data object OpenAppDetails : AccessCommand
    data object OpenNotificationSettings : AccessCommand
    data object None : AccessCommand
}

/** How the first run ends. */
sealed interface OnboardingExit {
    /** Open Home; [enableMonitoring] turns Flip to Mute on with the same request the tile uses. */
    data class OpenHome(val enableMonitoring: Boolean) : OnboardingExit
    /** Access-only mode is done or was left: return to the screen that opened it. */
    data object CloseAccess : OnboardingExit
    /** Back on the first step: close the app. */
    data object Close : OnboardingExit
}

/**
 * First run (M4): Welcome, Access, Try it. Built like Secret Calculator's OnboardingViewModel: a
 * step enum in saved state, one UiState, and a Boolean or exit event for the activity to act on.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val setupAccessRepository: SetupAccessRepository,
    private val preferencesRepository: AppPreferencesRepository,
    private val orientationMonitor: DeviceOrientationMonitor,
    private val flipFeedback: FlipFeedback,
    /** Null in plain JVM tests; Hilt always provides it. */
    private val funnel: Funnel? = null,
) : ViewModel() {

    private val accessOnly: Boolean = savedStateHandle[EXTRA_ACCESS_ONLY] ?: false
    private val step = savedStateHandle.getStateFlow(
        KEY_STEP,
        (if (accessOnly) OnboardingStep.ACCESS else OnboardingStep.WELCOME).name,
    )
    private val transient = MutableStateFlow(Transient())
    private val tryState = MutableStateFlow(TryState())
    private val mutableExit = MutableStateFlow<OnboardingExit?>(null)
    val exit: StateFlow<OnboardingExit?> = mutableExit

    private var visible = false
    private var sensorJob: Job? = null
    private var hintJob: Job? = null
    private var accessCompletionHandled = false

    val uiState: StateFlow<OnboardingUiState> = combine(
        step,
        setupAccessRepository.accessState,
        transient,
        tryState,
    ) { stepName, access, t, tryIt ->
        val next = nextAccess(access)
        OnboardingUiState(
            step = OnboardingStep.valueOf(stepName),
            accessOnly = accessOnly,
            access = access,
            nextAccess = next,
            nextNeedsSettings = next != null && next in t.refusedForGood,
            unsupported = access.phoneStateStatus == SetupAccessStatus.NOT_SUPPORTED,
            showSoundHint = t.showSoundHint,
            tryState = tryIt,
            message = t.message,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingUiState(accessOnly = accessOnly))

    init {
        viewModelScope.launch {
            setupAccessRepository.accessState.collect { access ->
                if (access.isSetupComplete && currentStep() == OnboardingStep.ACCESS) onAccessComplete()
            }
        }
    }

    // --- Steps ---

    fun onGetStarted() = goTo(OnboardingStep.ACCESS)

    /** Back goes to the previous step; on the first one it leaves (as Secret Calculator does). */
    fun back() {
        when {
            accessOnly -> mutableExit.value = OnboardingExit.CloseAccess
            currentStep() == OnboardingStep.WELCOME -> mutableExit.value = OnboardingExit.Close
            else -> goTo(OnboardingStep.entries[currentStep().ordinal - 1])
        }
    }

    /** "Do this later": Home opens in the "setup needed" state, and the first run never returns. */
    fun later() {
        if (accessOnly) {
            mutableExit.value = OnboardingExit.CloseAccess
            return
        }
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            mutableExit.value = OnboardingExit.OpenHome(enableMonitoring = false)
        }
    }

    /** The activity is in front (true) or not; the sensor runs only while Try it is visible. */
    fun onVisibilityChanged(isVisible: Boolean) {
        visible = isVisible
        if (isVisible) setupAccessRepository.refresh()
        updateSensor()
    }

    // --- Access ---

    /**
     * The primary Access button. [phoneRationale] and [notificationRationale] are Android's
     * "should show rationale" answers; [notificationRuntimeNeeded] is true on Android 13 and later
     * while the notification permission itself is missing.
     */
    fun onPrimaryAccess(
        phoneRationale: Boolean,
        notificationRationale: Boolean,
        notificationRuntimeNeeded: Boolean,
    ): AccessCommand = when (nextAccess(setupAccessRepository.accessState.value)) {
        AccessStep.PHONE -> runtimeCommand(RuntimeSetupPermission.PHONE, phoneRationale) { AccessCommand.OpenAppDetails }
        AccessStep.NOTIFICATIONS -> if (notificationRuntimeNeeded) {
            runtimeCommand(RuntimeSetupPermission.NOTIFICATIONS, notificationRationale) { AccessCommand.OpenNotificationSettings }
        } else {
            AccessCommand.OpenNotificationSettings
        }
        AccessStep.SOUND -> {
            // Leaving the app for a system list: explain what to tap first (design spec 4.2, M4-05)
            transient.update { it.copy(showSoundHint = true) }
            AccessCommand.None
        }
        null -> AccessCommand.None
    }

    fun dismissSoundHint() = transient.update { it.copy(showSoundHint = false) }

    fun onPermissionResult(permission: RuntimeSetupPermission, granted: Boolean, permanentlyDenied: Boolean) {
        setupAccessRepository.refresh()
        if (granted) return
        val step = if (permission == RuntimeSetupPermission.PHONE) AccessStep.PHONE else AccessStep.NOTIFICATIONS
        transient.update {
            it.copy(
                message = if (permission == RuntimeSetupPermission.PHONE) R.string.phone_access_denied else R.string.notification_access_denied,
                refusedForGood = if (permanentlyDenied) it.refusedForGood + step else it.refusedForGood,
            )
        }
    }

    fun onSettingsUnavailable() = transient.update { it.copy(message = R.string.settings_unavailable) }

    fun messageShown() = transient.update { it.copy(message = null) }

    private inline fun runtimeCommand(
        permission: RuntimeSetupPermission,
        rationale: Boolean,
        settings: () -> AccessCommand,
    ): AccessCommand {
        val requestedBefore: Boolean = savedStateHandle[permission.savedStateKey] ?: false
        return when (resolveRuntimePermissionAction(granted = false, shouldShowRationale = rationale, requestedBefore = requestedBefore)) {
            RuntimePermissionAction.OPEN_SETTINGS -> settings()
            RuntimePermissionAction.SHOW_EXPLANATION, RuntimePermissionAction.REFRESH -> {
                savedStateHandle[permission.savedStateKey] = true
                AccessCommand.RequestPermission(permission)
            }
        }
    }

    private fun onAccessComplete() {
        if (accessCompletionHandled) return
        accessCompletionHandled = true
        viewModelScope.launch {
            val firstTime = !preferencesRepository.preferences.first().onboardingCompleted
            if (firstTime) funnel?.setupComplete()
            when {
                accessOnly -> {
                    preferencesRepository.setOnboardingCompleted(true)
                    mutableExit.value = OnboardingExit.CloseAccess
                }
                // No usable motion sensor: there is nothing to practise, so finish (design spec 4.3)
                !orientationMonitor.isSensorAvailable -> complete()
                else -> goTo(OnboardingStep.TRY_IT)
            }
        }
    }

    // --- Try it ---

    /** "Turn on Flip to Mute" after a successful flip, or "Skip". */
    fun finish() = complete()

    private fun complete() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            funnel?.onboardingComplete(triedFlip = tryState.value.flipped)
            mutableExit.value = OnboardingExit.OpenHome(
                enableMonitoring = setupAccessRepository.accessState.value.isSetupComplete,
            )
        }
    }

    private fun updateSensor() {
        val shouldRun = visible && currentStep() == OnboardingStep.TRY_IT && !tryState.value.flipped
        if (shouldRun && sensorJob == null) {
            orientationMonitor.start()
            sensorJob = viewModelScope.launch {
                orientationMonitor.state.collect { state ->
                    val faceDown = state is FaceDownDetectionState.Detecting && state.orientation == DeviceOrientation.FACE_DOWN
                    tryState.update { it.copy(faceDown = faceDown) }
                    if (faceDown && !tryState.value.flipped) onFlipDetected()
                }
            }
            if (hintJob == null && !tryState.value.showHint) {
                hintJob = viewModelScope.launch {
                    delay(HINT_DELAY_MILLIS)
                    if (!tryState.value.flipped) tryState.update { it.copy(showHint = true) }
                }
            }
        } else if (!shouldRun) {
            stopSensor()
        }
    }

    private fun onFlipDetected() {
        flipFeedback.play()
        tryState.update { it.copy(flipped = true, showHint = false) }
        hintJob?.cancel()
        stopSensor()
    }

    private fun stopSensor() {
        if (sensorJob == null) return
        sensorJob?.cancel()
        sensorJob = null
        orientationMonitor.stop()
    }

    // --- Helpers ---

    private fun currentStep() = OnboardingStep.valueOf(step.value)

    private fun goTo(target: OnboardingStep) {
        savedStateHandle[KEY_STEP] = target.name
        updateSensor()
    }

    override fun onCleared() {
        stopSensor()
        super.onCleared()
    }

    private data class Transient(
        val showSoundHint: Boolean = false,
        val refusedForGood: Set<AccessStep> = emptySet(),
        @StringRes val message: Int? = null,
    )

    companion object {
        /** Intent extra: open only the Access step (from Home, Settings or a test screen). */
        const val EXTRA_ACCESS_ONLY = "access_only"
        private const val KEY_STEP = "onboarding_step"
        internal const val HINT_DELAY_MILLIS = 20_000L

        /** Phone, then notifications, then sound control; null when everything is granted. */
        fun nextAccess(access: SetupAccessState): AccessStep? = when {
            access.phoneStateStatus == SetupAccessStatus.NOT_GRANTED -> AccessStep.PHONE
            access.notificationStatus == SetupAccessStatus.NOT_GRANTED -> AccessStep.NOTIFICATIONS
            access.soundControlStatus == SetupAccessStatus.NOT_GRANTED -> AccessStep.SOUND
            else -> null
        }
    }
}
