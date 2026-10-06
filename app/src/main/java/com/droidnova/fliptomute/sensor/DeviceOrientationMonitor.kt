package com.droidnova.fliptomute.sensor

import kotlinx.coroutines.flow.StateFlow

interface DeviceOrientationMonitor {
    val state: StateFlow<FaceDownDetectionState>
    val isSensorAvailable: Boolean
    fun start()
    fun stop()

    /** Changes how readily a flip is accepted (future features F7). Monitors that cannot, keep their own. */
    fun configure(configuration: FaceDownDetectionConfiguration) = Unit
}

fun interface DeviceOrientationMonitorFactory {
    fun create(): DeviceOrientationMonitor
}
