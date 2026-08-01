package com.droidnova.fliptomute.service

import android.content.Context
import androidx.core.content.ContextCompat
import com.droidnova.fliptomute.util.MonitoringLog

interface MonitoringServiceController {
    fun startMonitoring(): MonitoringCommandResult
    fun pauseMonitoring(): MonitoringCommandResult
    fun resumeMonitoring(): MonitoringCommandResult
    fun stopMonitoring(): MonitoringCommandResult
}

class AndroidMonitoringServiceController(context: Context) : MonitoringServiceController {
    private val context = context.applicationContext

    override fun startMonitoring(): MonitoringCommandResult = try {
        MonitoringLog.d(context, "Monitoring start requested")
        ContextCompat.startForegroundService(context, FlipMonitoringService.createStartIntent(context))
        MonitoringLog.d(context, "Foreground service start intent sent")
        MonitoringCommandResult.Accepted
    } catch (error: SecurityException) {
        MonitoringLog.failure(context, "Foreground service start intent rejected", error)
        MonitoringCommandResult.Rejected(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
    } catch (error: IllegalStateException) {
        MonitoringLog.failure(context, "Foreground service start intent rejected", error)
        // Includes ForegroundServiceStartNotAllowedException on Android 12+.
        MonitoringCommandResult.Rejected(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
    }

    override fun resumeMonitoring(): MonitoringCommandResult = sendForegroundCommand(
        FlipMonitoringService.createResumeIntent(context),
    )

    override fun pauseMonitoring(): MonitoringCommandResult = sendServiceCommand(
        FlipMonitoringService.createPauseIntent(context),
    )

    override fun stopMonitoring(): MonitoringCommandResult = sendServiceCommand(
        FlipMonitoringService.createStopIntent(context),
    )

    private fun sendForegroundCommand(intent: android.content.Intent): MonitoringCommandResult = try {
        ContextCompat.startForegroundService(context, intent)
        MonitoringCommandResult.Accepted
    } catch (_: SecurityException) {
        MonitoringCommandResult.Rejected(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
    } catch (_: IllegalStateException) {
        MonitoringCommandResult.Rejected(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
    }

    private fun sendServiceCommand(intent: android.content.Intent): MonitoringCommandResult = try {
        context.startService(intent)
        MonitoringCommandResult.Accepted
    } catch (_: SecurityException) {
        MonitoringCommandResult.Rejected(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
    } catch (_: IllegalStateException) {
        MonitoringCommandResult.Rejected(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
    }
}
