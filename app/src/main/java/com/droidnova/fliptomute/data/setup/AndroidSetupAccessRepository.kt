package com.droidnova.fliptomute.data.setup

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidSetupAccessRepository(context: Context) : SetupAccessRepository {
    private val applicationContext = context.applicationContext
    private val mutableAccessState = MutableStateFlow(readAccessState())
    override val accessState: StateFlow<SetupAccessState> = mutableAccessState.asStateFlow()

    override fun refreshAndGet(): SetupAccessState {
        val refreshed = readAccessState()
        mutableAccessState.value = refreshed
        return refreshed
    }

    private fun readAccessState() = SetupAccessState(
        phoneStateStatus = phoneStateStatus(),
        soundControlStatus = soundControlStatus(),
        notificationStatus = notificationStatus(),
    )

    private fun phoneStateStatus(): SetupAccessStatus {
        if (!applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return SetupAccessStatus.NOT_SUPPORTED
        }
        return permissionStatus(Manifest.permission.READ_PHONE_STATE)
    }

    private fun soundControlStatus(): SetupAccessStatus {
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        return if (notificationManager.isNotificationPolicyAccessGranted) {
            SetupAccessStatus.GRANTED
        } else {
            SetupAccessStatus.NOT_GRANTED
        }
    }

    private fun notificationStatus(): SetupAccessStatus {
        val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        val notificationsEnabled = NotificationManagerCompat.from(applicationContext)
            .areNotificationsEnabled()
        return if (runtimePermissionGranted && notificationsEnabled) {
            SetupAccessStatus.GRANTED
        } else {
            SetupAccessStatus.NOT_GRANTED
        }
    }

    private fun permissionStatus(permission: String) =
        if (ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED) {
            SetupAccessStatus.GRANTED
        } else {
            SetupAccessStatus.NOT_GRANTED
        }
}
