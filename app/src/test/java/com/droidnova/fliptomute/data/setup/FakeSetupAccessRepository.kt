package com.droidnova.fliptomute.data.setup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSetupAccessRepository(initialState: SetupAccessState = SetupAccessState()) : SetupAccessRepository {
    private val mutableState = MutableStateFlow(initialState)
    override val accessState = mutableState.asStateFlow()
    var refreshCount = 0
        private set
    var stateOnRefresh: SetupAccessState? = null

    override fun refreshAndGet(): SetupAccessState {
        refreshCount++
        stateOnRefresh?.let { mutableState.value = it }
        return accessState.value
    }

    fun setState(state: SetupAccessState) {
        mutableState.value = state
    }
}
