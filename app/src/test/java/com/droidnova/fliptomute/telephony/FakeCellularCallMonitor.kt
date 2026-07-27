package com.droidnova.fliptomute.telephony

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCellularCallMonitor(
    override val isTelephonyAvailable: Boolean = true,
    private val stateAfterStart: CellularCallMonitorState =
        CellularCallMonitorState.Listening(CellularCallState.UNKNOWN, 1),
) : CellularCallMonitor {
    private val mutableState = MutableStateFlow<CellularCallMonitorState>(CellularCallMonitorState.Stopped)
    override val state = mutableState.asStateFlow()
    var startCount = 0
    var stopCount = 0
    private var started = false

    override fun start() {
        if (started) return
        started = true
        startCount++
        mutableState.value = stateAfterStart
    }

    override fun stop() {
        if (!started && mutableState.value == CellularCallMonitorState.Stopped) return
        started = false
        stopCount++
        mutableState.value = CellularCallMonitorState.Stopped
    }

    fun emit(state: CellularCallMonitorState) {
        mutableState.value = state
    }
}
