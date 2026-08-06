package com.droidnova.fliptomute.sensor

import kotlinx.coroutines.flow.StateFlow

enum class ProximityState { UNKNOWN, NEAR, FAR, UNAVAILABLE }

sealed interface ProximityMonitorState {
    data object Stopped : ProximityMonitorState
    data object WaitingForReading : ProximityMonitorState
    data class Listening(val proximityState: ProximityState) : ProximityMonitorState
    data object SensorUnavailable : ProximityMonitorState
    data object Error : ProximityMonitorState
}

interface ProximityMonitor {
    val state: StateFlow<ProximityMonitorState>
    val isSensorAvailable: Boolean
    fun start()
    fun stop()
}

fun interface ProximityMonitorFactory { fun create(): ProximityMonitor }

interface ProximitySensorCapability { val isAvailable: Boolean }
