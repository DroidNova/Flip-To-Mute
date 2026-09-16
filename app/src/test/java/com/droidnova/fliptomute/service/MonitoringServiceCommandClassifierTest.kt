package com.droidnova.fliptomute.service

import org.junit.Assert.assertEquals
import org.junit.Test

class MonitoringServiceCommandClassifierTest {
    @Test fun nullIntentIsRestartButNullActionIsUnknown() {
        assertEquals(MonitoringServiceCommand.RESTART, MonitoringServiceCommandClassifier.classify(false, null))
        assertEquals(MonitoringServiceCommand.UNKNOWN, MonitoringServiceCommandClassifier.classify(true, null))
    }

    @Test fun explicitCommandsAreRecognised() {
        assertEquals(
            MonitoringServiceCommand.START,
            MonitoringServiceCommandClassifier.classify(true, MonitoringServiceCommandClassifier.START_ACTION),
        )
        assertEquals(
            MonitoringServiceCommand.STOP,
            MonitoringServiceCommandClassifier.classify(true, MonitoringServiceCommandClassifier.STOP_ACTION),
        )
        assertEquals(
            MonitoringServiceCommand.PAUSE,
            MonitoringServiceCommandClassifier.classify(true, MonitoringServiceCommandClassifier.PAUSE_ACTION),
        )
        assertEquals(
            MonitoringServiceCommand.RESUME,
            MonitoringServiceCommandClassifier.classify(true, MonitoringServiceCommandClassifier.RESUME_ACTION),
        )
        assertEquals(
            MonitoringServiceCommand.REVALIDATE_ACCESS,
            MonitoringServiceCommandClassifier.classify(
                true,
                MonitoringServiceCommandClassifier.REVALIDATE_ACCESS_ACTION,
            ),
        )
    }

    @Test fun missingAccessTerminatesOnlyOperationalStates() {
        listOf(
            MonitoringRuntimeState.Starting,
            MonitoringRuntimeState.Resuming,
            MonitoringRuntimeState.Active,
        ).forEach { assertEquals(true, shouldShutdownForAccessLoss(it, setupComplete = false)) }
        listOf(
            MonitoringRuntimeState.Paused,
            MonitoringRuntimeState.Stopped,
            MonitoringRuntimeState.Recovering,
        ).forEach { assertEquals(false, shouldShutdownForAccessLoss(it, setupComplete = false)) }
        assertEquals(false, shouldShutdownForAccessLoss(MonitoringRuntimeState.Active, setupComplete = true))
    }

    @Test fun stickyRestartRequiresStoredIntentAndCompleteSetup() {
        assertEquals(StickyRestartDecision.StopDisabled, StickyRestartPolicy.decide(false, true))
        assertEquals(
            StickyRestartDecision.StopFailure(MonitoringFailure.SETUP_REQUIRED),
            StickyRestartPolicy.decide(true, false),
        )
        assertEquals(StickyRestartDecision.Continue, StickyRestartPolicy.decide(true, true))
    }

    @Test fun sequencerTracksNewestStartIdAndRejectsStaleGenerations() {
        val sequencer = MonitoringCommandSequencer()
        sequencer.record(4)
        val pauseGeneration = sequencer.supersede()
        sequencer.record(5)

        assertEquals(5, sequencer.latestStartId)
        assertEquals(true, sequencer.isCurrent(pauseGeneration))

        val stopGeneration = sequencer.supersede()
        assertEquals(false, sequencer.isCurrent(pauseGeneration))
        assertEquals(true, sequencer.isCurrent(stopGeneration))
    }
}
