package com.droidnova.fliptomute.quicksettings

import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickSettingsResolversTest {
    private val stateResolver = QuickSettingsTileStateResolver()
    private val clickResolver = QuickSettingsTileClickResolver()

    @Test fun runtimeStatesMapToTileStatuses() {
        assertEquals(QuickSettingsTileStatus.ON, stateResolver.resolve(MonitoringRuntimeState.Active, false))
        assertEquals(QuickSettingsTileStatus.STARTING, stateResolver.resolve(MonitoringRuntimeState.Starting, true))
        assertEquals(QuickSettingsTileStatus.STOPPING, stateResolver.resolve(MonitoringRuntimeState.Stopping, true))
        assertEquals(QuickSettingsTileStatus.OFF, stateResolver.resolve(MonitoringRuntimeState.Stopped, true))
        assertEquals(QuickSettingsTileStatus.SETUP_REQUIRED, stateResolver.resolve(MonitoringRuntimeState.Stopped, false))
        assertEquals(
            QuickSettingsTileStatus.ERROR,
            stateResolver.resolve(MonitoringRuntimeState.Error(MonitoringFailure.UNKNOWN), true),
        )
    }

    @Test fun clicksMapToExactlyOneSafeAction() {
        assertEquals(QuickSettingsTileClickAction.StopMonitoring, clickResolver.resolve(MonitoringRuntimeState.Active, true))
        assertEquals(QuickSettingsTileClickAction.Ignore, clickResolver.resolve(MonitoringRuntimeState.Starting, true))
        assertEquals(QuickSettingsTileClickAction.Ignore, clickResolver.resolve(MonitoringRuntimeState.Stopping, true))
        assertEquals(QuickSettingsTileClickAction.StartMonitoring, clickResolver.resolve(MonitoringRuntimeState.Stopped, true))
        assertEquals(
            QuickSettingsTileClickAction.StartMonitoring,
            clickResolver.resolve(MonitoringRuntimeState.Error(MonitoringFailure.UNKNOWN), true),
        )
        assertEquals(
            QuickSettingsTileClickAction.OpenSetupAndEnable,
            clickResolver.resolve(MonitoringRuntimeState.Stopped, false),
        )
        assertEquals(
            QuickSettingsTileClickAction.OpenSetupAndEnable,
            clickResolver.resolve(MonitoringRuntimeState.Error(MonitoringFailure.UNKNOWN), false),
        )
    }

    @Test fun addTilePlatformResultsMapSafely() {
        assertEquals(QuickSettingsTileAddResult.Added, QuickSettingsTileAddResultMapper.map(2))
        assertEquals(QuickSettingsTileAddResult.AlreadyAdded, QuickSettingsTileAddResultMapper.map(1))
        assertEquals(QuickSettingsTileAddResult.NotAdded, QuickSettingsTileAddResultMapper.map(0))
        assertEquals(QuickSettingsTileAddResult.RequestInProgress, QuickSettingsTileAddResultMapper.map(1001))
        assertEquals(QuickSettingsTileAddResult.Failed, QuickSettingsTileAddResultMapper.map(-1))
        assertEquals(QuickSettingsTileAddResult.Failed, QuickSettingsTileAddResultMapper.map(9999))
    }
}
