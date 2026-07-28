package com.droidnova.fliptomute.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceRingerModeMapperTest {
    @Test fun normalMapsBothWays() = assertBoth(DeviceRingerMode.NORMAL, DeviceRingerModeMapper.ANDROID_MODE_NORMAL)
    @Test fun vibrateMapsBothWays() = assertBoth(DeviceRingerMode.VIBRATE, DeviceRingerModeMapper.ANDROID_MODE_VIBRATE)
    @Test fun silentMapsBothWays() = assertBoth(DeviceRingerMode.SILENT, DeviceRingerModeMapper.ANDROID_MODE_SILENT)
    @Test fun unexpectedIsUnknown() = assertEquals(DeviceRingerMode.UNKNOWN, DeviceRingerModeMapper.fromAndroidMode(99))
    @Test fun unknownHasNoAndroidMode() = assertNull(DeviceRingerModeMapper.toAndroidMode(DeviceRingerMode.UNKNOWN))

    private fun assertBoth(mode: DeviceRingerMode, androidMode: Int) {
        assertEquals(mode, DeviceRingerModeMapper.fromAndroidMode(androidMode))
        assertEquals(androidMode, DeviceRingerModeMapper.toAndroidMode(mode))
    }
}
