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
    }

    @Test fun stickyRestartRequiresStoredIntentAndCompleteSetup() {
        assertEquals(StickyRestartDecision.StopDisabled, StickyRestartPolicy.decide(false, true))
        assertEquals(
            StickyRestartDecision.StopFailure(MonitoringFailure.SETUP_REQUIRED),
            StickyRestartPolicy.decide(true, false),
        )
        assertEquals(StickyRestartDecision.Continue, StickyRestartPolicy.decide(true, true))
    }
}
