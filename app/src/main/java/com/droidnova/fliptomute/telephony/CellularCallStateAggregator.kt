package com.droidnova.fliptomute.telephony

object CellularCallStateAggregator {
    fun aggregate(states: Collection<CellularCallState>): CellularCallState = when {
        CellularCallState.RINGING in states -> CellularCallState.RINGING
        CellularCallState.ACTIVE in states -> CellularCallState.ACTIVE
        CellularCallState.IDLE in states -> CellularCallState.IDLE
        else -> CellularCallState.UNKNOWN
    }
}
