package com.droidnova.fliptomute.service

internal enum class MonitoringServiceCommand { START, STOP, RESTART, UNKNOWN }

internal object MonitoringServiceCommandClassifier {
    const val START_ACTION = "com.droidnova.fliptomute.action.START_MONITORING"
    const val STOP_ACTION = "com.droidnova.fliptomute.action.STOP_MONITORING"

    fun classify(hasIntent: Boolean, action: String?): MonitoringServiceCommand = when {
        !hasIntent -> MonitoringServiceCommand.RESTART
        action == START_ACTION -> MonitoringServiceCommand.START
        action == STOP_ACTION -> MonitoringServiceCommand.STOP
        else -> MonitoringServiceCommand.UNKNOWN
    }
}

internal sealed interface StickyRestartDecision {
    data object Continue : StickyRestartDecision
    data object StopDisabled : StickyRestartDecision
    data class StopFailure(val reason: MonitoringFailure) : StickyRestartDecision
}

internal object StickyRestartPolicy {
    fun decide(storedMonitoring: Boolean, setupComplete: Boolean): StickyRestartDecision = when {
        !storedMonitoring -> StickyRestartDecision.StopDisabled
        !setupComplete -> StickyRestartDecision.StopFailure(MonitoringFailure.SETUP_REQUIRED)
        else -> StickyRestartDecision.Continue
    }
}
