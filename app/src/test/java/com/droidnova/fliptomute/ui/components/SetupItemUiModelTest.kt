package com.droidnova.fliptomute.ui.components

import com.droidnova.fliptomute.data.setup.SetupAccessState
import com.droidnova.fliptomute.data.setup.SetupAccessStatus
import com.droidnova.fliptomute.data.setup.SetupAccessType
import org.junit.Assert.assertEquals
import org.junit.Test

class SetupItemUiModelTest {
    @Test fun setupStateMapsEveryAccessTypeAndStatus() {
        val items = SetupAccessState(
            phoneStateStatus = SetupAccessStatus.GRANTED,
            soundControlStatus = SetupAccessStatus.NOT_GRANTED,
            notificationStatus = SetupAccessStatus.NOT_SUPPORTED,
        ).toSetupItems()

        assertEquals(SetupAccessType.entries.toList(), items.map { it.type })
        assertEquals(
            listOf(
                SetupAccessStatus.GRANTED,
                SetupAccessStatus.NOT_GRANTED,
                SetupAccessStatus.NOT_SUPPORTED,
            ),
            items.map { it.status },
        )
    }
}
