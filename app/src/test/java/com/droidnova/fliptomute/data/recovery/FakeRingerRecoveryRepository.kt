package com.droidnova.fliptomute.data.recovery

import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRingerRecoveryRepository(
    initial: RingerRecoverySession? = null,
) : RingerRecoveryRepository {
    private val state = MutableStateFlow(initial)
    override val recoverySession = state
    var failWrites = false
    val events = mutableListOf<String>()

    override suspend fun getRecoverySession() = state.value
    override suspend fun saveRecoverySession(session: RingerRecoverySession) {
        if (failWrites) throw IOException("test")
        events += "save"
        state.value = session
    }
    override suspend fun clearRecoverySession() {
        if (failWrites) throw IOException("test")
        events += "clear"
        state.value = null
    }
}
