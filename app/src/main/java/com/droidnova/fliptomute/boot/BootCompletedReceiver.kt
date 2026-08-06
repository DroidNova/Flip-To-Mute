package com.droidnova.fliptomute.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.droidnova.fliptomute.app.FlipToMuteApplication
import com.droidnova.fliptomute.util.MonitoringLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val applicationContext = context.applicationContext
        MonitoringLog.d(applicationContext, "BOOT_COMPLETED received")
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val application = applicationContext as FlipToMuteApplication
                val result = application.container.bootMonitoringCoordinator.handleBootCompleted()
                MonitoringLog.d(applicationContext, "Boot action finished: ${result.javaClass.simpleName}")
            } catch (error: IllegalStateException) {
                MonitoringLog.failure(applicationContext, "Boot handling failed", error)
            } finally {
                pendingResult.finish()
                MonitoringLog.d(applicationContext, "Boot receiver finished")
            }
        }
    }
}
