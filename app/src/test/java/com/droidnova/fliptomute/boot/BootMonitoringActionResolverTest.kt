package com.droidnova.fliptomute.boot

import org.junit.Assert.assertEquals
import org.junit.Test

class BootMonitoringActionResolverTest {
    private val resolver = BootMonitoringActionResolver()

    @Test fun disabledSettingAlwaysStaysOff() {
        assertEquals(BootMonitoringAction.StayOff, resolver.resolve(false, false, false))
        assertEquals(BootMonitoringAction.StayOff, resolver.resolve(false, true, false))
        assertEquals(BootMonitoringAction.StayOff, resolver.resolve(false, true, true))
    }

    @Test fun enabledSettingRespectsDurableMonitoringIntent() {
        assertEquals(BootMonitoringAction.StayOff, resolver.resolve(true, false, false))
        assertEquals(BootMonitoringAction.StayOff, resolver.resolve(true, false, true))
        assertEquals(BootMonitoringAction.StartMonitoring, resolver.resolve(true, true, false))
        assertEquals(BootMonitoringAction.RestorePausedState, resolver.resolve(true, true, true))
    }
}
