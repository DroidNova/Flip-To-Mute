package com.droidnova.fliptomute.telephony

object CellularCallStateMapper {
    const val ANDROID_STATE_IDLE = 0
    const val ANDROID_STATE_RINGING = 1
    const val ANDROID_STATE_OFFHOOK = 2

    fun fromAndroidState(state: Int): CellularCallState = when (state) {
        ANDROID_STATE_IDLE -> CellularCallState.IDLE
        ANDROID_STATE_RINGING -> CellularCallState.RINGING
        ANDROID_STATE_OFFHOOK -> CellularCallState.ACTIVE
        else -> CellularCallState.UNKNOWN
    }
}
