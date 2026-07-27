package com.droidnova.fliptomute.telephony

import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

/** API 26–30 fallback; these APIs are deprecated only because API 31 introduced TelephonyCallback. */
@Suppress("DEPRECATION")
internal class LegacyCallStateRegistration(
    private val telephonyManager: TelephonyManager,
    onStateChanged: (Int) -> Unit,
) {
    private val listener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, ignoredPhoneNumber: String?) {
            onStateChanged(state)
        }
    }

    fun register() {
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    fun unregister() {
        telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
    }
}
