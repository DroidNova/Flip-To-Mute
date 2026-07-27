package com.droidnova.fliptomute.data.recovery

import com.droidnova.fliptomute.audio.DeviceRingerMode
import kotlinx.coroutines.flow.Flow

data class RingerRecoverySession(
    val previousMode: DeviceRingerMode,
    val appliedMode: DeviceRingerMode,
)

interface RingerRecoveryRepository {
    val recoverySession: Flow<RingerRecoverySession?>
    suspend fun getRecoverySession(): RingerRecoverySession?
    suspend fun saveRecoverySession(session: RingerRecoverySession)
    suspend fun clearRecoverySession()
}
