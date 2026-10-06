package com.droidnova.fliptomute.service

import com.droidnova.fliptomute.data.analytics.AnalyticsEvents
import com.droidnova.fliptomute.data.analytics.AnalyticsLogger
import com.droidnova.fliptomute.data.preferences.AppPreferencesRepository
import com.droidnova.fliptomute.data.setup.SetupAccessRepository
import com.droidnova.fliptomute.notification.InterruptionAlertController
import kotlinx.coroutines.flow.first

enum class HealthCheckResult {
    /** The user has Flip to Mute off or paused. */
    NOTHING_TO_DO,
    /** The service is running or changing state. */
    HEALTHY,
    /** Access is missing; only the user can fix that, and the app reports it when opened. */
    SETUP_INCOMPLETE,
    /** The service was missing and a start was requested. */
    START_REQUESTED,
    /** The service was missing and Android refused to start it; the "stopped" alert was posted. */
    START_REJECTED,
}

/**
 * The periodic check behind HealthCheckWorker (v2.0 plan section 4.1, audit R3 and R4). If the
 * user wants Flip to Mute on and it is not running, it tries to start it. Android 12 and later
 * usually refuse a start from the background unless battery optimisation is off for the app, so
 * a refusal ends in the "stopped" alert rather than silence.
 */
class MonitoringHealthCheck(
    private val preferencesRepository: AppPreferencesRepository,
    private val setupAccessRepository: SetupAccessRepository,
    private val monitoringStateRepository: MonitoringStateRepository,
    private val serviceController: MonitoringServiceController,
    private val alertController: InterruptionAlertController,
    private val analytics: AnalyticsLogger = AnalyticsLogger { _, _ -> },
) {
    suspend fun run(): HealthCheckResult {
        val preferences = preferencesRepository.preferences.first()
        if (!preferences.monitoringEnabled || preferences.monitoringPaused) return HealthCheckResult.NOTHING_TO_DO
        val runtime = monitoringStateRepository.state.value
        if (runtime.isRunningOrChanging()) return HealthCheckResult.HEALTHY
        // A timed pause keeps the service running: nothing to restart (M5-06)
        if (runtime is MonitoringRuntimeState.Paused && monitoringStateRepository.pausedUntil.value != null) {
            return HealthCheckResult.HEALTHY
        }
        if (!setupAccessRepository.refreshAndGet().isSetupComplete) return HealthCheckResult.SETUP_INCOMPLETE
        return when (val result = serviceController.startMonitoring(MonitoringStartSource.HEALTH_CHECK)) {
            MonitoringCommandResult.Accepted -> HealthCheckResult.START_REQUESTED
            is MonitoringCommandResult.Rejected -> {
                monitoringStateRepository.updateState(MonitoringRuntimeState.Error(result.reason))
                alertController.showStoppedAlert()
                analytics.log(
                    AnalyticsEvents.AUTO_RESUME,
                    mapOf(
                        AnalyticsEvents.PARAM_SOURCE to MonitoringStartSource.HEALTH_CHECK.analyticsName,
                        AnalyticsEvents.PARAM_SUCCESS to "false",
                    ),
                )
                HealthCheckResult.START_REJECTED
            }
        }
    }

    private fun MonitoringRuntimeState.isRunningOrChanging(): Boolean =
        this is MonitoringRuntimeState.Active || this is MonitoringRuntimeState.Starting ||
            this is MonitoringRuntimeState.Resuming || this is MonitoringRuntimeState.Pausing ||
            this is MonitoringRuntimeState.Stopping
}
