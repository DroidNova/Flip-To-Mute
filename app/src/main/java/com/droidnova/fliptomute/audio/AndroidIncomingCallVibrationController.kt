package com.droidnova.fliptomute.audio

import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import android.util.Log

class AndroidIncomingCallVibrationController(
    context: Context,
    private val capabilityRepository: VibrationCapabilityRepository,
) : IncomingCallVibrationController {
    private val context = context.applicationContext
    private var running = false

    override fun getAvailability() = capabilityRepository.refresh()

    @Synchronized
    override fun start(): IncomingCallVibrationResult {
        if (running) return IncomingCallVibrationResult.Started
        val unavailable = when (getAvailability()) {
            VibrationAvailability.AVAILABLE -> null
            VibrationAvailability.NO_VIBRATOR -> IncomingCallVibrationFailure.NO_VIBRATOR
            VibrationAvailability.TEMPORARILY_BLOCKED -> IncomingCallVibrationFailure.BLOCKED_BY_SYSTEM_POLICY
            VibrationAvailability.UNAVAILABLE -> IncomingCallVibrationFailure.VIBRATOR_SERVICE_UNAVAILABLE
        }
        if (unavailable != null) return IncomingCallVibrationResult.Failed(unavailable)
        val vibrator = vibrator() ?: return IncomingCallVibrationResult.Failed(
            IncomingCallVibrationFailure.VIBRATOR_SERVICE_UNAVAILABLE,
        )
        Log.d(TAG, "Vibration start requested")
        return try {
            val effect = VibrationEffect.createWaveform(INCOMING_CALL_PATTERN, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(effect, VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_RINGTONE).build())
            } else vibrateLegacy(vibrator, effect)
            running = true
            Log.d(TAG, "Vibration started")
            IncomingCallVibrationResult.Started
        } catch (_: SecurityException) { failed() }
        catch (_: IllegalStateException) { failed() }
        catch (_: IllegalArgumentException) { failed() }
        catch (_: UnsupportedOperationException) { failed() }
    }

    @Suppress("DEPRECATION")
    private fun vibrateLegacy(vibrator: Vibrator, effect: VibrationEffect) = vibrator.vibrate(
        effect,
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
    )

    @Synchronized
    override fun stop() {
        vibrator()?.cancel()
        if (running) Log.d(TAG, "Vibration stopped")
        running = false
    }

    private fun vibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private fun failed(): IncomingCallVibrationResult.Failed {
        running = false
        Log.d(TAG, "Vibration start failed: START_FAILED")
        return IncomingCallVibrationResult.Failed(IncomingCallVibrationFailure.START_FAILED)
    }

    private companion object {
        const val TAG = "IncomingVibration"
        val INCOMING_CALL_PATTERN = longArrayOf(0L, 600L, 800L)
    }
}
