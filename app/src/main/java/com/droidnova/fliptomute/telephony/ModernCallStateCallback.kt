package com.droidnova.fliptomute.telephony

import android.os.Build
import android.telephony.TelephonyCallback
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.S)
internal class ModernCallStateCallback(
    private val onStateChanged: (Int) -> Unit,
) : TelephonyCallback(), TelephonyCallback.CallStateListener {
    override fun onCallStateChanged(state: Int) = onStateChanged(state)
}
