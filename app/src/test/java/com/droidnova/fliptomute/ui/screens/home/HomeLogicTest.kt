package com.droidnova.fliptomute.ui.screens.home

import com.droidnova.fliptomute.data.preferences.CallActionSelection
import com.droidnova.fliptomute.data.stats.FlipStats
import com.droidnova.fliptomute.service.MonitoringErrorRecoveryIntent
import com.droidnova.fliptomute.service.MonitoringFailure
import com.droidnova.fliptomute.service.MonitoringRuntimeState
import com.droidnova.fliptomute.ui.common.HomeHint
import com.droidnova.fliptomute.ui.components.FlipChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** M5-01, M5-03, M5-04, M5-09, M5-11: the pure rules behind Home. */
class HomeLogicTest {

    // --- Home states table (design spec 4.4) ---

    @Test fun everyRuntimeStateMapsToItsRow() {
        val rows = mapOf(
            MonitoringRuntimeState.Unresolved to HomeStatus.CHECKING,
            MonitoringRuntimeState.Recovering to HomeStatus.CHECKING,
            MonitoringRuntimeState.Stopped to HomeStatus.OFF,
            MonitoringRuntimeState.Starting to HomeStatus.TURNING_ON,
            MonitoringRuntimeState.Resuming to HomeStatus.TURNING_ON,
            MonitoringRuntimeState.Active to HomeStatus.ON,
            MonitoringRuntimeState.Pausing to HomeStatus.BUSY,
            MonitoringRuntimeState.Stopping to HomeStatus.BUSY,
            MonitoringRuntimeState.Paused to HomeStatus.PAUSED,
            MonitoringRuntimeState.Error(MonitoringFailure.UNKNOWN) to HomeStatus.ATTENTION,
        )
        rows.forEach { (runtime, status) -> assertEquals(runtime.toString(), status, homeStatus(runtime, true, true)) }
    }

    @Test fun missingAccessWhileOff_isSetupNeeded_butErrorsStillShowAttention() {
        assertEquals(HomeStatus.SETUP_NEEDED, homeStatus(MonitoringRuntimeState.Stopped, setupComplete = false, monitoringChecked = false))
        assertEquals(
            HomeStatus.ATTENTION,
            homeStatus(MonitoringRuntimeState.Error(MonitoringFailure.SETUP_REQUIRED), setupComplete = false, monitoringChecked = false),
        )
    }

    // --- Silence or Vibrate ---

    @Test fun storedSelectionsMapToOneChoice() {
        assertEquals(FlipChoice.SILENCE, CallActionSelection(muteRingtone = true, vibratePhone = false).toFlipChoice())
        assertEquals(FlipChoice.VIBRATE, CallActionSelection(muteRingtone = false, vibratePhone = true).toFlipChoice())
        // 1.x allowed both; the phone ends up vibrating, so it shows as Vibrate
        assertEquals(FlipChoice.VIBRATE, CallActionSelection(muteRingtone = true, vibratePhone = true).toFlipChoice())
    }

    @Test fun choicesStoreAValidSelection() {
        assertEquals(CallActionSelection(true, false), FlipChoice.SILENCE.toSelection())
        assertEquals(CallActionSelection(false, true), FlipChoice.VIBRATE.toSelection())
        FlipChoice.entries.forEach { assertTrue(it.toSelection().isValid) }
    }

    // --- Attention table ---

    @Test fun everyFailureHasATitle_andOnlyTelephonyHasNoFix() {
        MonitoringFailure.entries.forEach { failure ->
            val attention = attentionFor(MonitoringRuntimeState.Error(failure))
            assertEquals(failure, attention.failure)
            if (failure == MonitoringFailure.TELEPHONY_UNAVAILABLE) {
                assertEquals(AttentionFix.NONE, attention.fix)
                assertNull(attention.fixLabel)
            } else {
                assertTrue(failure.name, attention.fixLabel != null)
            }
        }
    }

    @Test fun fixesMatchTheDesignSpec() {
        fun fix(failure: MonitoringFailure) = attentionFor(MonitoringRuntimeState.Error(failure)).fix
        assertEquals(AttentionFix.TURN_BACK_ON, fix(MonitoringFailure.SERVICE_START_NOT_ALLOWED))
        assertEquals(AttentionFix.FIX_ACCESS, fix(MonitoringFailure.SETUP_REQUIRED))
        assertEquals(AttentionFix.FIX_ACCESS, fix(MonitoringFailure.NOTIFICATION_UNAVAILABLE))
        assertEquals(AttentionFix.FIX_ACCESS, fix(MonitoringFailure.SOUND_CONTROL_FAILED))
        assertEquals(AttentionFix.CHECK_SETUP, fix(MonitoringFailure.SENSOR_UNAVAILABLE))
        assertEquals(AttentionFix.TRY_AGAIN, fix(MonitoringFailure.CALL_MONITOR_FAILED))
        assertEquals(AttentionFix.ACKNOWLEDGE, fix(MonitoringFailure.CLEANUP_FAILED))
        assertEquals(
            AttentionFix.RESUME,
            attentionFor(MonitoringRuntimeState.Error(MonitoringFailure.UNKNOWN, MonitoringErrorRecoveryIntent.RESUME)).fix,
        )
    }

    // --- Card order ---

    private val someStats = FlipStats(thisMonth = 3, total = 10)
    private val attention = attentionFor(MonitoringRuntimeState.Error(MonitoringFailure.UNKNOWN))

    @Test fun attentionComesFirst_thenStats_andNoDiscoveryNextToAProblem() {
        val cards = orderCards(HomeStatus.ATTENTION, attention, batteryWarning = false, someStats, HomeHint.FLIP_TO_LOCK)
        assertEquals(listOf(HomeCard.NeedsAttention(attention), HomeCard.Stats(someStats)), cards)
    }

    @Test fun batteryWarningShowsOnlyWhileOn_andHidesDiscovery() {
        assertEquals(
            listOf(HomeCard.BatteryWarning, HomeCard.Stats(someStats)),
            orderCards(HomeStatus.ON, null, batteryWarning = true, someStats, HomeHint.FLIP_TO_LOCK),
        )
        assertEquals(emptyList<HomeCard>(), orderCards(HomeStatus.OFF, null, batteryWarning = true, FlipStats(), null))
    }

    @Test fun statsWaitForTheFirstFlip_andDiscoveryShowsWhileOn() {
        assertEquals(
            listOf(HomeCard.Discovery(HomeHint.QUICK_SETTINGS_TILE)),
            orderCards(HomeStatus.ON, null, batteryWarning = false, FlipStats(), HomeHint.QUICK_SETTINGS_TILE),
        )
        assertEquals(emptyList<HomeCard>(), orderCards(HomeStatus.OFF, null, false, FlipStats(), HomeHint.QUICK_SETTINGS_TILE))
    }

    // --- Discovery rules ---

    private val day = 24 * 60 * 60 * 1000L
    private val base = DiscoveryInputs(
        now = 10 * day,
        firstOnAt = 0L,
        startAfterRestart = true,
        flipToLockEnabled = false,
        flipToLockSupported = true,
        tileAdded = false,
        tileRequestSupported = true,
        dismissed = emptySet(),
    )

    @Test fun restartSettingOffComesFirst() =
        assertEquals(HomeHint.START_AFTER_RESTART, nextDiscovery(base.copy(startAfterRestart = false)))

    @Test fun flipToLockAfterTwoDays_thenTileAfterFour() {
        assertNull(nextDiscovery(base.copy(now = day)))
        assertEquals(HomeHint.FLIP_TO_LOCK, nextDiscovery(base.copy(now = 2 * day)))
        assertEquals(HomeHint.QUICK_SETTINGS_TILE, nextDiscovery(base.copy(now = 4 * day, flipToLockEnabled = true)))
    }

    @Test fun dismissedOrIrrelevantCards_neverShow() {
        assertNull(nextDiscovery(base.copy(dismissed = setOf(HomeHint.FLIP_TO_LOCK, HomeHint.QUICK_SETTINGS_TILE))))
        assertNull(nextDiscovery(base.copy(flipToLockSupported = false, tileAdded = true)))
        assertNull(nextDiscovery(base.copy(firstOnAt = null)))
    }
}

class SameDayTest {
    @Test fun sameDayAndDifferentDays() {
        val noon = java.util.Calendar.getInstance().apply { set(2026, 9, 6, 12, 0, 0) }.timeInMillis
        assertTrue(isSameDay(noon, noon + 60 * 60 * 1000L))
        assertTrue(!isSameDay(noon, noon + 24 * 60 * 60 * 1000L))
    }
}
