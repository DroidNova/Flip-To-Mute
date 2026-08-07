package com.droidnova.fliptomute.screenlock

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ScreenStateRepository {
    val isInteractiveAndUnlocked: StateFlow<Boolean>
    fun refresh(): Boolean
}

class AndroidScreenStateRepository(context: Context) : ScreenStateRepository {
    private val applicationContext = context.applicationContext
    private val powerManager = applicationContext.getSystemService(PowerManager::class.java)
    private val keyguardManager = applicationContext.getSystemService(KeyguardManager::class.java)
    private val mutableState = MutableStateFlow(readState())
    override val isInteractiveAndUnlocked: StateFlow<Boolean> = mutableState.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refresh()
        }
    }

    init {
        ContextCompat.registerReceiver(
            applicationContext,
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    override fun refresh(): Boolean = readState().also { mutableState.value = it }

    private fun readState(): Boolean = powerManager?.isInteractive == true && keyguardManager?.isDeviceLocked == false
}
