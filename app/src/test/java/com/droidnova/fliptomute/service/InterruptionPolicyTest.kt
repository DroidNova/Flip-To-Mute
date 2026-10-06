package com.droidnova.fliptomute.service

import org.junit.Assert.assertEquals
import org.junit.Test

class InterruptionPolicyTest {

    @Test fun usersOwnFailedStart_turnsOff() {
        assertEquals(
            InterruptionDecision.TURN_OFF,
            InterruptionPolicy.decide(MonitoringStartSource.USER, FailurePhase.STARTING, MonitoringFailure.CALL_MONITOR_FAILED),
        )
    }

    @Test fun automaticStartFailures_keepTheChoiceAndAlert() {
        val automatic = MonitoringStartSource.entries - MonitoringStartSource.USER
        val recoverable = listOf(
            MonitoringFailure.SERVICE_START_NOT_ALLOWED,
            MonitoringFailure.NOTIFICATION_UNAVAILABLE,
            MonitoringFailure.CALL_MONITOR_FAILED,
            MonitoringFailure.SOUND_CONTROL_FAILED,
            MonitoringFailure.SENSOR_UNAVAILABLE,
            MonitoringFailure.UNKNOWN,
        )
        for (source in automatic) for (reason in recoverable) {
            assertEquals("$source $reason", InterruptionDecision.KEEP_ON_AND_ALERT,
                InterruptionPolicy.decide(source, FailurePhase.STARTING, reason))
        }
    }

    @Test fun failuresWhileRunning_keepTheChoiceEvenWhenTheUserStartedIt() {
        assertEquals(
            InterruptionDecision.KEEP_ON_AND_ALERT,
            InterruptionPolicy.decide(MonitoringStartSource.USER, FailurePhase.RUNNING, MonitoringFailure.SENSOR_UNAVAILABLE),
        )
    }

    @Test fun lostAccessAndMissingTelephony_alwaysTurnOff() {
        for (source in MonitoringStartSource.entries) for (phase in FailurePhase.entries) {
            assertEquals(InterruptionDecision.TURN_OFF,
                InterruptionPolicy.decide(source, phase, MonitoringFailure.SETUP_REQUIRED))
            assertEquals(InterruptionDecision.TURN_OFF,
                InterruptionPolicy.decide(source, phase, MonitoringFailure.TELEPHONY_UNAVAILABLE))
        }
    }

    @Test fun unknownSourceNames_areTreatedAsTheUser() {
        assertEquals(MonitoringStartSource.USER, MonitoringStartSource.fromName(null))
        assertEquals(MonitoringStartSource.USER, MonitoringStartSource.fromName("SOMETHING_ELSE"))
        assertEquals(MonitoringStartSource.BOOT, MonitoringStartSource.fromName("BOOT"))
    }
}
