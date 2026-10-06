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

/**
 * An app update kills the running process, and Android does not restart a sticky service after
 * that (audit R1). This brings Flip to Mute back without the user opening the app. Android allows
 * a foreground service to start from MY_PACKAGE_REPLACED, as it does from BOOT_COMPLETED.
 */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val applicationContext = context.applicationContext
        MonitoringLog.d(applicationContext, "MY_PACKAGE_REPLACED received")
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val application = applicationContext as FlipToMuteApplication
                val result = application.container.bootMonitoringCoordinator.handle(AutoStartTrigger.PACKAGE_REPLACED)
                MonitoringLog.d(applicationContext, "Update action finished: ${result.javaClass.simpleName}")
            } catch (error: RuntimeException) {
                MonitoringLog.failure(applicationContext, "Update handling failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
