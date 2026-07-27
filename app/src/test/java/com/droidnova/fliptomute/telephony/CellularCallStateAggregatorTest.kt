package com.droidnova.fliptomute.telephony

import org.junit.Assert.assertEquals
import org.junit.Test

class CellularCallStateAggregatorTest {
    @Test fun emptyIsUnknown() = assertAggregate(CellularCallState.UNKNOWN)
    @Test fun idleIsIdle() = assertAggregate(CellularCallState.IDLE, CellularCallState.IDLE)
    @Test fun ringingIsRinging() = assertAggregate(CellularCallState.RINGING, CellularCallState.RINGING)
    @Test fun activeIsActive() = assertAggregate(CellularCallState.ACTIVE, CellularCallState.ACTIVE)
    @Test fun ringingBeatsActive() = assertAggregate(CellularCallState.RINGING, CellularCallState.ACTIVE, CellularCallState.RINGING)
    @Test fun ringingBeatsIdle() = assertAggregate(CellularCallState.RINGING, CellularCallState.IDLE, CellularCallState.RINGING)
    @Test fun activeBeatsIdle() = assertAggregate(CellularCallState.ACTIVE, CellularCallState.IDLE, CellularCallState.ACTIVE)
    @Test fun allIdleIsIdle() = assertAggregate(CellularCallState.IDLE, CellularCallState.IDLE, CellularCallState.IDLE)
    @Test fun unknownDoesNotOverrideKnown() = assertAggregate(CellularCallState.ACTIVE, CellularCallState.UNKNOWN, CellularCallState.ACTIVE)

    private fun assertAggregate(expected: CellularCallState, vararg states: CellularCallState) {
        assertEquals(expected, CellularCallStateAggregator.aggregate(states.toList()))
    }
}
