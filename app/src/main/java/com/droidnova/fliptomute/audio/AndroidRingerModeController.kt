package com.droidnova.fliptomute.audio

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager

class AndroidRingerModeController(context: Context) : RingerModeController by DefaultRingerModeController(
    AndroidRingerModePlatform(context.applicationContext),
)

private class AndroidRingerModePlatform(context: Context) : RingerModePlatform {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    override val isAvailable: Boolean get() = audioManager != null && notificationManager != null
    override val isVolumeFixed: Boolean get() = audioManager?.isVolumeFixed ?: true
    override val hasNotificationPolicyAccess: Boolean
        get() = notificationManager?.isNotificationPolicyAccessGranted == true

    override fun getRingerMode(): Int = checkNotNull(audioManager).ringerMode

    override fun setRingerMode(mode: Int) {
        checkNotNull(audioManager).ringerMode = mode
    }
}
