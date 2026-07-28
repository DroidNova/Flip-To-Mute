package com.droidnova.fliptomute.telephony

import kotlinx.coroutines.flow.StateFlow

interface CellularCallMonitor {
    val state: StateFlow<CellularCallMonitorState>
    val isTelephonyAvailable: Boolean
    fun start()
    fun stop()
}

fun interface CellularCallMonitorFactory {
    fun create(): CellularCallMonitor
}
