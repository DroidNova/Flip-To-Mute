package com.droidnova.fliptomute.ui.screens.sensor_test

import com.droidnova.fliptomute.sensor.DeviceOrientation
import com.droidnova.fliptomute.sensor.OrientationSensorSource

data class SensorTestUiState(
    val isSensorAvailable: Boolean = true,
    val isTesting: Boolean = false,
    val orientation: DeviceOrientation = DeviceOrientation.UNKNOWN,
    val sensorSource: OrientationSensorSource? = null,
    val gravityX: Float? = null,
    val gravityY: Float? = null,
    val gravityZ: Float? = null,
    val hasError: Boolean = false,
)
