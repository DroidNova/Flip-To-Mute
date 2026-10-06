package com.droidnova.fliptomute.ui.screens.home

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidnova.fliptomute.data.preferences.AppPreferences
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.reliability.BatteryOptimizationStatus
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.stats.FlipStats
import com.droidnova.fliptomute.data.stats.FlipStatsStore
import com.droidnova.fliptomute.deviceadmin.DeviceAdminAvailability
import com.droidnova.fliptomute.deviceadmin.DeviceAdminCapabilityRepository
import com.droidnova.fliptomute.service.AppRecoveryManager
import com.droidnova.fliptomute.service.MonitoringCommandResult
import com.droidnova.fliptomute.service.MonitoringErrorRecoveryIntent
import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.service.MonitoringServiceController
import com.droidnova.fliptomute.service.MonitoringStateRepository
import com.droidnova.fliptomute.service.shouldShutdownForAccessLoss
import com.droidnova.fliptomute.ui.common.HintStore
import com.droidnova.fliptomute.ui.common.HomeHint
import com.droidnova.fliptomute.ui.components.FlipChoice
import com.droidnova.fliptomute.utils.AppVersion
import com.droidnova.fliptomute.utils.UpdateAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Home (M5). The 1.x monitoring logic is unchanged; v2.0 adds the Home status, Silence or Vibrate,
 * timed pause, cards and the one-time dialogs. Built like Secret Calculator's view models: persisted
 * and live state combined with a private transient state into one UiState.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val setupAccessRepository: SetupAccessRepository,
    private val monitoringStateRepository: MonitoringStateRepository,
    private val serviceController: MonitoringServiceController,
    private val appRecoveryManager: AppRecoveryManager,
    private val savedStateHandle: SavedStateHandle,
    private val flipStatsStore: FlipStatsStore,
    private val hintStore: HintStore,
    private val batteryOptimizationStatus: BatteryOptimizationStatus,
    private val deviceAdminRepository: DeviceAdminCapabilityRepository,
    private val appVersion: AppVersion,
    private val updateAvailability: UpdateAvailability,
) : ViewModel() {
    /** Wall clock; a property so tests can move time. */
    internal var now: () -> Long = System::currentTimeMillis

    private val message = MutableStateFlow<MonitoringFailure?>(null)
    private val pendingAction = savedStateHandle.getStateFlow(
        PENDING_MONITORING_ACTION_KEY,
        PendingMonitoringAction.NONE.name,
    )
    private val transient = MutableStateFlow(
        Transient(dismissedHints = hintStore.dismissedHints(), batteryHiddenUntil = hintStore.batteryCardHiddenUntil),
    )
    private val device = MutableStateFlow(readDevice())
    private var accessCheckInProgress = false
    private val pendingDialogs = ArrayDeque<HomeDialog>()

    private val live = combine(
        preferencesRepository.preferences,
        setupAccessRepository.accessState,
        monitoringStateRepository.state,
        monitoringStateRepository.pausedUntil,
        flipStatsStore.stats,
    ) { preferences, access, runtime, pausedUntil, stats -> Live(preferences, access, runtime, pausedUntil, stats) }

    val uiState: StateFlow<HomeUiState> = combine(
        live,
        message,
        pendingAction,
        transient,
        device,
    ) { l, currentMessage, pendingActionName, t, d -> buildState(l, currentMessage, pendingActionName, t, d) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), HomeUiState())

    init {
        viewModelScope.launch {
            appRecoveryManager.reconcileMonitoringState(requestActiveReconstruction = true)
        }
        viewModelScope.launch {
            monitoringStateRepository.state.collect { runtime ->
                if (runtime is MonitoringRuntimeState.Error) message.value = runtime.reason
            }
        }
        if (currentPendingAction() != PendingMonitoringAction.NONE) refreshAccessState()
        // One-time dialogs, created once per visit to Home, as in Secret Calculator (M5-10)
        val version = appVersion.code()
        if (updateAvailability.isUpdateAvailable(version)) pendingDialogs += HomeDialog.UPDATE_AVAILABLE
        if (hintStore.whatsNewSeenVersion < version) pendingDialogs += HomeDialog.WHATS_NEW
        showNextDialog()
    }

    private fun buildState(l: Live, currentMessage: MonitoringFailure?, pendingActionName: String, t: Transient, d: Device): HomeUiState {
        val runtime = l.runtime
        val transitional = runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Pausing ||
            runtime is MonitoringRuntimeState.Resuming || runtime is MonitoringRuntimeState.Stopping ||
            runtime is MonitoringRuntimeState.Recovering || runtime is MonitoringRuntimeState.Unresolved
        val pausedError = runtime is MonitoringRuntimeState.Error &&
            runtime.recoveryIntent == MonitoringErrorRecoveryIntent.RESUME
        // An interrupted session keeps the saved choice but is not running: show it as off, with "Try again"
        val checked = (l.preferences.monitoringEnabled && runtime !is MonitoringRuntimeState.Error) ||
            pausedError || runtime is MonitoringRuntimeState.Active ||
            runtime is MonitoringRuntimeState.Starting || runtime is MonitoringRuntimeState.Resuming
        val status = homeStatus(runtime, l.access.isSetupComplete, checked)
        val attention = (runtime as? MonitoringRuntimeState.Error)
            ?.takeUnless { it.reason in t.acknowledged }
            ?.let(::attentionFor)
        val time = now()
        val batteryWarning = d.batteryRestricted && time >= t.batteryHiddenUntil
        val discovery = nextDiscovery(
            DiscoveryInputs(
                now = time,
                firstOnAt = l.stats.firstOnAt,
                startAfterRestart = l.preferences.startAfterPhoneRestart,
                flipToLockEnabled = l.preferences.flipToLockEnabled,
                flipToLockSupported = d.lockSupported,
                tileAdded = d.tileAdded,
                tileRequestSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                totalFlips = l.stats.total,
                flatOnlyEnabled = l.preferences.requireFlatSurfaceBeforeFlip,
                dismissed = t.dismissedHints,
            ),
        )
        return HomeUiState(
            isSetupComplete = l.access.isSetupComplete,
            selectedFlipAction = l.preferences.selectedFlipAction,
            callActionSelection = l.preferences.callActionSelection,
            monitoringState = runtime,
            isMonitoringChecked = checked,
            isMonitoringSwitchEnabled = !transitional,
            message = currentMessage,
            showPermissionsSheet = pendingActionName != PendingMonitoringAction.NONE.name,
            status = status,
            pausedUntilEpochMs = l.pausedUntil.takeIf { status == HomeStatus.PAUSED },
            flipChoice = l.preferences.callActionSelection.toFlipChoice(),
            cards = orderCards(status, attention, batteryWarning, l.stats, discovery),
            dialog = t.dialog,
            showPauseSheet = t.showPauseSheet,
            event = t.event,
        )
    }

    // --- The main control and its buttons ---

    /** The big round control: on, off, resume or fix, depending on what Home shows. */
    fun onPowerControlTapped() {
        when (uiState.value.status) {
            HomeStatus.SETUP_NEEDED -> {
                setPendingAction(PendingMonitoringAction.START)
                sendEvent(HomeEvent.OPEN_ACCESS)
            }
            HomeStatus.OFF -> onMonitoringChanged(true)
            HomeStatus.ON -> onMonitoringChanged(false)
            HomeStatus.PAUSED -> onResumeMonitoring()
            HomeStatus.ATTENTION -> uiState.value.cards.filterIsInstance<HomeCard.NeedsAttention>().firstOrNull()
                ?.let { onAttentionFix(it.attention.fix) }
                ?: onMonitoringChanged(true)
            HomeStatus.CHECKING, HomeStatus.TURNING_ON, HomeStatus.BUSY -> Unit
        }
    }

    fun onMonitoringChanged(enabled: Boolean) {
        if (enabled) {
            validateAndDispatch(PendingMonitoringAction.START)
        } else {
            setPendingAction(PendingMonitoringAction.NONE)
            serviceController.stopMonitoring()
        }
    }

    fun onPauseMonitoring() {
        if (monitoringStateRepository.state.value is MonitoringRuntimeState.Active) {
            when (val result = serviceController.pauseMonitoring()) {
                MonitoringCommandResult.Accepted -> Unit
                is MonitoringCommandResult.Rejected -> message.value = result.reason
            }
        }
    }

    fun onResumeMonitoring() {
        validateAndDispatch(PendingMonitoringAction.RESUME)
    }

    // --- Pause sheet (M5-07) ---

    fun showPauseSheet() = transient.update { it.copy(showPauseSheet = true) }

    fun dismissPauseSheet() = transient.update { it.copy(showPauseSheet = false) }

    /** [minutes] null pauses until the user turns it back on (the 1.x pause). */
    fun onPauseFor(minutes: Int?) {
        dismissPauseSheet()
        if (minutes == null) {
            onPauseMonitoring()
            return
        }
        if (monitoringStateRepository.state.value !is MonitoringRuntimeState.Active) return
        val result = serviceController.pauseMonitoringUntil(now() + minutes * MINUTE_MS)
        if (result is MonitoringCommandResult.Rejected) message.value = result.reason
    }

    // --- Silence or Vibrate (M5-03) ---

    fun onFlipChoiceSelected(choice: FlipChoice) {
        viewModelScope.launch { preferencesRepository.setCallActionSelection(choice.toSelection()) }
    }

    fun onFlipActionSelected(action: FlipAction) {
        viewModelScope.launch { preferencesRepository.setFlipAction(action) }
    }

    fun onMuteRingtoneChanged(selected: Boolean) = updateSelection(mute = selected)
    fun onVibratePhoneChanged(selected: Boolean) = updateSelection(vibrate = selected)

    private fun updateSelection(mute: Boolean? = null, vibrate: Boolean? = null) {
        val current = uiState.value.callActionSelection
        val updated = current.copy(
            muteRingtone = mute ?: current.muteRingtone,
            vibratePhone = vibrate ?: current.vibratePhone,
        )
        if (updated.isValid) viewModelScope.launch { preferencesRepository.setCallActionSelection(updated) }
    }

    // --- Cards (M5-04, M5-08, M5-09) ---

    fun onAttentionFix(fix: AttentionFix) {
        when (fix) {
            AttentionFix.TURN_BACK_ON, AttentionFix.TRY_AGAIN -> onMonitoringChanged(true)
            AttentionFix.RESUME -> onResumeMonitoring()
            AttentionFix.FIX_ACCESS -> sendEvent(HomeEvent.OPEN_ACCESS)
            AttentionFix.CHECK_SETUP -> sendEvent(HomeEvent.OPEN_CHECK_SETUP)
            AttentionFix.ACKNOWLEDGE -> (monitoringStateRepository.state.value as? MonitoringRuntimeState.Error)
                ?.let { error -> transient.update { it.copy(acknowledged = it.acknowledged + error.reason) } }
            AttentionFix.NONE -> Unit
        }
    }

    fun onBatteryFix() = sendEvent(HomeEvent.OPEN_BATTERY_SETTINGS)

    fun onBatteryLater() {
        val until = now() + BATTERY_SNOOZE_MS
        hintStore.batteryCardHiddenUntil = until
        transient.update { it.copy(batteryHiddenUntil = until) }
    }

    fun onDiscoveryAccepted(hint: HomeHint) {
        dismissHint(hint)
        when (hint) {
            HomeHint.START_AFTER_RESTART -> viewModelScope.launch { preferencesRepository.setStartAfterPhoneRestart(true) }
            HomeHint.FLIP_TO_LOCK -> sendEvent(HomeEvent.OPEN_SETTINGS)
            HomeHint.QUICK_SETTINGS_TILE -> sendEvent(HomeEvent.REQUEST_TILE)
            HomeHint.CHECK_SETUP -> sendEvent(HomeEvent.OPEN_CHECK_SETUP)
            HomeHint.FLAT_ONLY -> viewModelScope.launch { preferencesRepository.setRequireFlatSurfaceBeforeFlip(true) }
        }
    }

    fun onDiscoveryDismissed(hint: HomeHint) = dismissHint(hint)

    private fun dismissHint(hint: HomeHint) {
        hintStore.dismiss(hint)
        transient.update { it.copy(dismissedHints = it.dismissedHints + hint) }
    }

    // --- Dialogs (M5-10) ---

    fun onDialogConfirmed() {
        when (uiState.value.dialog) {
            HomeDialog.UPDATE_AVAILABLE -> sendEvent(HomeEvent.OPEN_STORE)
            HomeDialog.WHATS_NEW -> hintStore.whatsNewSeenVersion = appVersion.code()
            null -> Unit
        }
        showNextDialog()
    }

    fun onDialogDismissed() {
        if (uiState.value.dialog == HomeDialog.WHATS_NEW) hintStore.whatsNewSeenVersion = appVersion.code()
        showNextDialog()
    }

    private fun showNextDialog() = transient.update { it.copy(dialog = pendingDialogs.removeFirstOrNull()) }

    // --- Events, messages, resume ---

    fun onEventHandled() = transient.update { it.copy(event = null) }

    private fun sendEvent(event: HomeEvent) = transient.update { it.copy(event = event) }

    fun onMessageShown() { message.value = null }

    fun dismissPermissionsSheet() {
        setPendingAction(PendingMonitoringAction.NONE)
    }

    /** The permissions sheet's Continue: open the Access step, keeping the pending start. */
    fun onPermissionsSheetContinue() = sendEvent(HomeEvent.OPEN_ACCESS)

    /** Home came back to the front: access, battery, tile and stats may have changed elsewhere. */
    fun refreshAccessState() {
        val access = setupAccessRepository.refreshAndGet()
        if (shouldShutdownForAccessLoss(monitoringStateRepository.state.value, access.isSetupComplete)) {
            serviceController.revalidateAccess()
        }
        device.value = readDevice()
        flipStatsStore.refresh()
        val action = currentPendingAction()
        if (action != PendingMonitoringAction.NONE && access.isSetupComplete) {
            dispatchPendingAction(action)
        }
    }

    private fun readDevice() = Device(
        batteryRestricted = batteryOptimizationStatus.isIgnoringBatteryOptimizations() == false,
        lockSupported = deviceAdminRepository.availability.value != DeviceAdminAvailability.UNSUPPORTED,
        tileAdded = hintStore.tileAdded,
    )

    private fun validateAndDispatch(action: PendingMonitoringAction) {
        if (accessCheckInProgress) return
        accessCheckInProgress = true
        try {
            val access = setupAccessRepository.refreshAndGet()
            if (access.isSetupComplete) {
                dispatchPendingAction(action)
            } else {
                setPendingAction(action)
            }
        } finally {
            accessCheckInProgress = false
        }
    }

    private fun dispatchPendingAction(action: PendingMonitoringAction) {
        setPendingAction(PendingMonitoringAction.NONE)
        val result = when (action) {
            PendingMonitoringAction.START -> serviceController.startMonitoring()
            PendingMonitoringAction.RESUME -> serviceController.resumeMonitoring()
            PendingMonitoringAction.NONE -> return
        }
        if (result is MonitoringCommandResult.Rejected) message.value = result.reason
    }

    private fun currentPendingAction(): PendingMonitoringAction =
        runCatching { PendingMonitoringAction.valueOf(pendingAction.value) }
            .getOrDefault(PendingMonitoringAction.NONE)

    private fun setPendingAction(action: PendingMonitoringAction) {
        savedStateHandle[PENDING_MONITORING_ACTION_KEY] = action.name
    }

    private enum class PendingMonitoringAction { NONE, START, RESUME }

    private data class Live(
        val preferences: AppPreferences,
        val access: SetupAccessState,
        val runtime: MonitoringRuntimeState,
        val pausedUntil: Long?,
        val stats: FlipStats,
    )

    private data class Device(val batteryRestricted: Boolean, val lockSupported: Boolean, val tileAdded: Boolean)


    private data class Transient(
        val showPauseSheet: Boolean = false,
        val dialog: HomeDialog? = null,
        val event: HomeEvent? = null,
        val acknowledged: Set<MonitoringFailure> = emptySet(),
        val dismissedHints: Set<HomeHint> = emptySet(),
        val batteryHiddenUntil: Long = 0L,
    )

    private companion object {
        const val PENDING_MONITORING_ACTION_KEY = "pending_monitoring_action"
        const val MINUTE_MS = 60_000L
        const val BATTERY_SNOOZE_MS = 14 * 24 * 60 * 60 * 1000L
    }
}
