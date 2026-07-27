package com.droidnova.fliptomute.data.setup

import kotlinx.coroutines.flow.StateFlow

interface SetupAccessRepository {
    val accessState: StateFlow<SetupAccessState>
    fun refresh()
}
