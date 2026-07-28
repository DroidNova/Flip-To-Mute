package com.droidnova.fliptomute.audio

object DeviceRingerModeMapper {
    const val ANDROID_MODE_SILENT = 0
    const val ANDROID_MODE_VIBRATE = 1
    const val ANDROID_MODE_NORMAL = 2

    fun fromAndroidMode(mode: Int): DeviceRingerMode = when (mode) {
        ANDROID_MODE_NORMAL -> DeviceRingerMode.NORMAL
        ANDROID_MODE_VIBRATE -> DeviceRingerMode.VIBRATE
        ANDROID_MODE_SILENT -> DeviceRingerMode.SILENT
        else -> DeviceRingerMode.UNKNOWN
    }

    fun toAndroidMode(mode: DeviceRingerMode): Int? = when (mode) {
        DeviceRingerMode.NORMAL -> ANDROID_MODE_NORMAL
        DeviceRingerMode.VIBRATE -> ANDROID_MODE_VIBRATE
        DeviceRingerMode.SILENT -> ANDROID_MODE_SILENT
        DeviceRingerMode.UNKNOWN -> null
    }
}
