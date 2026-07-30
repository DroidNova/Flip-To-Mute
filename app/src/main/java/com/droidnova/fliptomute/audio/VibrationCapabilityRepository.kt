package com.droidnova.fliptomute.audio

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface VibrationCapabilityRepository {
    val availability: StateFlow<VibrationAvailability>
    fun refresh(): VibrationAvailability
}

class AndroidVibrationCapabilityRepository(context: Context) : VibrationCapabilityRepository {
    private val context = context.applicationContext
    private val mutableAvailability = MutableStateFlow(detect())
    override val availability: StateFlow<VibrationAvailability> = mutableAvailability

    override fun refresh(): VibrationAvailability = detect().also { mutableAvailability.value = it }

    private fun detect(): VibrationAvailability {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return VibrationAvailability.UNAVAILABLE.also { Log.d(TAG, "Vibrator service unavailable") }
        if (!vibrator.hasVibrator()) return VibrationAvailability.NO_VIBRATOR
        val filter = context.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter
        if (filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
            filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
        ) return VibrationAvailability.TEMPORARILY_BLOCKED.also { Log.d(TAG, "DND restriction detected") }
        Log.d(TAG, "Vibrator hardware available")
        return VibrationAvailability.AVAILABLE
    }

    private companion object { const val TAG = "IncomingVibration" }
}
