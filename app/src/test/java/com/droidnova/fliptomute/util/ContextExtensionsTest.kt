package com.droidnova.fliptomute.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextExtensionsTest {
    @Test fun successfulPrimarySettingsRouteDoesNotUseFallback() {
        var fallbackCalled = false

        val result = settingsLaunchResult(primaryOpened = true) {
            fallbackCalled = true
            SettingsLaunchResult.UNAVAILABLE
        }

        assertEquals(SettingsLaunchResult.OPENED, result)
        assertEquals(false, fallbackCalled)
    }

    @Test fun failedPrimaryAndFallbackReturnRecoverableFailure() {
        val result = settingsLaunchResult(primaryOpened = false) {
            SettingsLaunchResult.UNAVAILABLE
        }

        assertEquals(SettingsLaunchResult.UNAVAILABLE, result)
    }
}
