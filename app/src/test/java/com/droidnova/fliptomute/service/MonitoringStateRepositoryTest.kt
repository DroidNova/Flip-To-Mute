package com.droidnova.fliptomute.service

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringStateRepositoryTest {
    @Test fun defaultsToStoppedAndPublishesUpdates() {
        val repository = InMemoryMonitoringStateRepository()
        assertEquals(MonitoringRuntimeState.Stopped, repository.state.value)
        repository.updateState(MonitoringRuntimeState.Starting)
        assertEquals(MonitoringRuntimeState.Starting, repository.state.value)
    }
}
