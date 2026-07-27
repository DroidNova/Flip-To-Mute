package com.droidnova.fliptomute.telephony

import org.junit.Assert.assertEquals
import org.junit.Test

class CellularCallStateMapperTest {
    @Test fun idleMapsToIdle() = assertMapping(CellularCallStateMapper.ANDROID_STATE_IDLE, CellularCallState.IDLE)
    @Test fun ringingMapsToRinging() = assertMapping(CellularCallStateMapper.ANDROID_STATE_RINGING, CellularCallState.RINGING)
    @Test fun offhookMapsToActive() = assertMapping(CellularCallStateMapper.ANDROID_STATE_OFFHOOK, CellularCallState.ACTIVE)
    @Test fun unexpectedMapsToUnknown() = assertMapping(99, CellularCallState.UNKNOWN)
    @Test fun negativeMapsToUnknown() = assertMapping(-1, CellularCallState.UNKNOWN)

    private fun assertMapping(androidState: Int, expected: CellularCallState) {
        assertEquals(expected, CellularCallStateMapper.fromAndroidState(androidState))
    }
}
