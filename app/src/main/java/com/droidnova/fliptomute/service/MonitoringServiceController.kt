package com.droidnova.fliptomute.service

import android.content.Context
import androidx.core.content.ContextCompat

interface MonitoringServiceController {
    fun startMonitoring(): MonitoringCommandResult
    fun stopMonitoring()
}

class AndroidMonitoringServiceController(context: Context) : MonitoringServiceController {
    private val context = context.applicationContext

    override fun startMonitoring(): MonitoringCommandResult = try {
        ContextCompat.startForegroundService(context, FlipMonitoringService.createStartIntent(context))
        MonitoringCommandResult.Accepted
    } catch (_: SecurityException) {
        MonitoringCommandResult.Rejected(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
    } catch (_: IllegalStateException) {
        // Includes ForegroundServiceStartNotAllowedException on Android 12+.
        MonitoringCommandResult.Rejected(MonitoringFailure.SERVICE_START_NOT_ALLOWED)
    }

    override fun stopMonitoring() {
        try {
            context.startService(FlipMonitoringService.createStopIntent(context))
        } catch (_: SecurityException) {
            // The service is already inaccessible, so there is nothing left to stop.
        } catch (_: IllegalStateException) {
            // A background stop command can be rejected when the service is already gone.
        }
    }
}
