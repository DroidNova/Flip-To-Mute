package com.droidnova.fliptomute.data.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupAccessRepositoryRefreshTest {
    @Test fun refreshAndGetReturnsNewlyCalculatedStateInsteadOfCachedState() {
        val repository = FakeSetupAccessRepository(SetupAccessState())
        val granted = SetupAccessState(
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.GRANTED,
        )
        repository.stateOnRefresh = granted

        assertEquals(granted, repository.refreshAndGet())
        assertTrue(repository.accessState.value.isSetupComplete)
    }

    @Test fun setupRequiresAllThreeCurrentAccessResults() {
        SetupAccessState(
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.NOT_GRANTED,
        ).let { assertFalse(it.isSetupComplete) }
        SetupAccessState(
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.GRANTED,
            SetupAccessStatus.GRANTED,
        ).let { assertTrue(it.isSetupComplete) }
    }
}
