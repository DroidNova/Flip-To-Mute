package com.droidnova.fliptomute.sensor

import kotlinx.coroutines.flow.StateFlow

interface DeviceOrientationMonitor {
    val state: StateFlow<FaceDownDetectionState>
    val isSensorAvailable: Boolean
    fun start()
    fun stop()
}

fun interface DeviceOrientationMonitorFactory {
    fun create(): DeviceOrientationMonitor
}
