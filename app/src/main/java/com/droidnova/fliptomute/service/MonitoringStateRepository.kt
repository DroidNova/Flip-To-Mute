package com.droidnova.fliptomute.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface MonitoringStateRepository {
    val state: StateFlow<MonitoringRuntimeState>
    fun updateState(state: MonitoringRuntimeState)
}

class InMemoryMonitoringStateRepository : MonitoringStateRepository {
    private val mutableState = MutableStateFlow<MonitoringRuntimeState>(MonitoringRuntimeState.Recovering)
    override val state: StateFlow<MonitoringRuntimeState> = mutableState.asStateFlow()
    override fun updateState(state: MonitoringRuntimeState) { mutableState.value = state }
}
