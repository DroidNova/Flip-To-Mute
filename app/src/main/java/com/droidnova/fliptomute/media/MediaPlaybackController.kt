package com.droidnova.fliptomute.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether music or video is playing, and the way to pause it (future features F3). */
interface MediaPlaybackController {
    val isPlaying: StateFlow<Boolean>

    /** Sends the pause key every media app understands. False when it could not be sent. */
    fun pause(): Boolean
}

/**
 * Android reports which kinds of sound are playing without saying which app plays them, and takes
 * a media key from any app. No permission or special access is involved.
 */
class AndroidMediaPlaybackController(context: Context) : MediaPlaybackController {
    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
    private val mutableIsPlaying = MutableStateFlow(read())
    override val isPlaying: StateFlow<Boolean> = mutableIsPlaying.asStateFlow()

    private val callback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>?) {
            mutableIsPlaying.value = configs.orEmpty().any(::isMedia)
        }
    }

    init {
        try {
            audioManager?.registerAudioPlaybackCallback(callback, Handler(Looper.getMainLooper()))
        } catch (_: RuntimeException) {
            // Without the callback the feature simply never sees playback
        }
    }

    override fun pause(): Boolean {
        val manager = audioManager ?: return false
        return try {
            manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
            manager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun read(): Boolean = try {
        audioManager?.activePlaybackConfigurations?.any(::isMedia) == true
    } catch (_: RuntimeException) {
        false
    }

    /** Music, video and games; never ringtones, alarms or navigation prompts. */
    private fun isMedia(configuration: AudioPlaybackConfiguration): Boolean {
        val usage = configuration.audioAttributes.usage
        return usage == AudioAttributes.USAGE_MEDIA || usage == AudioAttributes.USAGE_GAME
    }
}
