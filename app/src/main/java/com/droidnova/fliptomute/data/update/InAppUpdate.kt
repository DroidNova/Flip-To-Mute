package com.droidnova.fliptomute.data.update

import android.app.Activity
import android.content.Context
import com.droidnova.fliptomute.utils.MonitoringLog
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play's in-app update (M7-09). The Home dialog still appears only when Remote Config says a newer
 * version exists; its Update button now downloads in the background (flexible flow) while the app
 * keeps working, then asks for a restart. Where Play cannot update in place (not installed from
 * Play, or no update visible yet), it falls back to the Play Store page as before.
 */
@Singleton
class InAppUpdate @Inject constructor(@ApplicationContext private val context: Context) {
    private val manager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(context) }
    private var listener: InstallStateUpdatedListener? = null

    fun start(activity: Activity, onDownloaded: () -> Unit, onFallback: () -> Unit) {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                val flexibleAllowed = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                if (!flexibleAllowed) {
                    onFallback()
                    return@addOnSuccessListener
                }
                watchDownload(onDownloaded)
                manager.startUpdateFlow(info, activity, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE))
                    .addOnFailureListener { error ->
                        MonitoringLog.d(context, "In-app update flow failed: ${error.message}")
                        stopWatching()
                        onFallback()
                    }
            }
            .addOnFailureListener { error ->
                MonitoringLog.d(context, "In-app update info unavailable: ${error.message}")
                onFallback()
            }
    }

    /** A download finished while the app was away: Google asks apps to offer the restart on return. */
    fun checkDownloaded(onDownloaded: () -> Unit) {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) onDownloaded()
        }
    }

    /** Installs the downloaded update; Play restarts the app. */
    fun completeUpdate() {
        stopWatching()
        manager.completeUpdate()
    }

    private fun watchDownload(onDownloaded: () -> Unit) {
        stopWatching()
        val newListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                stopWatching()
                onDownloaded()
            }
        }
        listener = newListener
        manager.registerListener(newListener)
    }

    private fun stopWatching() {
        listener?.let(manager::unregisterListener)
        listener = null
    }
}
