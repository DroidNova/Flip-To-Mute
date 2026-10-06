package com.droidnova.fliptomute.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * One short buzz when a flip is detected, for "Buzz when a flip is detected" (audit R8). The
 * screen is face down, so this is the only sign that the flip worked.
 */
fun interface FlipFeedback {
    fun play()
}

class AndroidFlipFeedback(context: Context) : FlipFeedback {
    private val applicationContext = context.applicationContext

    override fun play() {
        try {
            val vibrator = vibrator()?.takeIf { it.hasVibrator() } ?: return
            val effect = VibrationEffect.createOneShot(BUZZ_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    effect,
                    VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK).build(),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    effect,
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build(),
                )
            }
        } catch (_: RuntimeException) {
            // Feedback is a nicety: it must never stop the flip action
        }
    }

    private fun vibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        applicationContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private companion object {
        const val BUZZ_MILLIS = 80L
    }
}
