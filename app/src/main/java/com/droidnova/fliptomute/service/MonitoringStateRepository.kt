package com.droidnova.fliptomute.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface MonitoringStateRepository {
    val state: StateFlow<MonitoringRuntimeState>
    fun updateState(state: MonitoringRuntimeState)

    /**
     * End of a timed pause (epoch ms) while the service stays on and ignores calls, or null. Set only
     * together with [MonitoringRuntimeState.Paused]; an open-ended pause leaves it null (M5-06).
     */
    val pausedUntil: StateFlow<Long?>
    fun updatePausedUntil(epochMs: Long?)
}

class InMemoryMonitoringStateRepository : MonitoringStateRepository {
    private val mutableState = MutableStateFlow<MonitoringRuntimeState>(MonitoringRuntimeState.Unresolved)
    override val state: StateFlow<MonitoringRuntimeState> = mutableState.asStateFlow()
    override fun updateState(state: MonitoringRuntimeState) { mutableState.value = state }

    private val mutablePausedUntil = MutableStateFlow<Long?>(null)
    override val pausedUntil: StateFlow<Long?> = mutablePausedUntil.asStateFlow()
    override fun updatePausedUntil(epochMs: Long?) { mutablePausedUntil.value = epochMs }
}
