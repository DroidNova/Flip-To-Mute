package com.droidnova.fliptomute.ui.screens.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingGateTest {

    @Test fun brandNewUser_seesTheFirstRun() =
        assertEquals(StartDestination.ONBOARDING, OnboardingGate.decide(false, false, false))

    @Test fun anyoneWhoFinishedOrSetUpOrTurnedItOn_goesHome() {
        assertEquals(StartDestination.HOME, OnboardingGate.decide(true, false, false))
        assertEquals(StartDestination.HOME, OnboardingGate.decide(false, true, false))
        assertEquals(StartDestination.HOME, OnboardingGate.decide(false, false, true))
    }

    @Test fun earlierUsersWithoutTheFlag_areMarkedAsDone() {
        assertTrue(OnboardingGate.shouldMarkCompleted(false, true, false))
        assertTrue(OnboardingGate.shouldMarkCompleted(false, false, true))
        assertFalse(OnboardingGate.shouldMarkCompleted(true, true, true))
        assertFalse(OnboardingGate.shouldMarkCompleted(false, false, false))
    }
}

/** Moved with the rules from the 1.x setup screen's tests (M4-09). */
class RuntimePermissionsTest {

    @Test fun granted_refreshes() = assertEquals(
        RuntimePermissionAction.REFRESH,
        resolveRuntimePermissionAction(granted = true, shouldShowRationale = false, requestedBefore = true),
    )

    @Test fun firstRequestAndRationale_explainAndAsk() {
        assertEquals(RuntimePermissionAction.SHOW_EXPLANATION, resolveRuntimePermissionAction(false, false, false))
        assertEquals(RuntimePermissionAction.SHOW_EXPLANATION, resolveRuntimePermissionAction(false, true, true))
    }

    @Test fun previouslyRequestedWithoutRationale_opensSettings() = assertEquals(
        RuntimePermissionAction.OPEN_SETTINGS,
        resolveRuntimePermissionAction(granted = false, shouldShowRationale = false, requestedBefore = true),
    )
}
