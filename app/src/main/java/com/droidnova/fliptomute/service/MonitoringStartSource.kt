package com.droidnova.fliptomute.service

/** Who asked the service to start. Decides what a failed start is allowed to change. */
enum class MonitoringStartSource(val analyticsName: String) {
    /** The user, from Home, the tile or a notification. */
    USER("user"),
    /** BOOT_COMPLETED, when "Start after phone restart" is on. */
    BOOT("boot"),
    /** MY_PACKAGE_REPLACED, after an app update killed the old process. */
    PACKAGE_REPLACED("update"),
    /** Android recreated the sticky service after killing the process. */
    SYSTEM_RESTART("system_restart"),
    /** The periodic health check found the service missing. */
    HEALTH_CHECK("health"),
    /** Opening the app found the service missing and rebuilt it. */
    APP_RECOVERY("app_open"),
    ;

    companion object {
        /** Unknown or missing values are treated as the user, which keeps the 1.x behaviour. */
        fun fromName(name: String?): MonitoringStartSource = entries.firstOrNull { it.name == name } ?: USER
    }
}

/** Whether a failure happened while starting, or later while the service was running. */
internal enum class FailurePhase { STARTING, RUNNING }

internal enum class InterruptionDecision {
    /** Clear the saved "on" choice: the user just tried and failed, or it can never work now. */
    TURN_OFF,
    /** Keep the saved "on" choice, show an error and tell the user that Flip to Mute stopped. */
    KEEP_ON_AND_ALERT,
}

/**
 * The rule from v2.0 plan section 4.1 (audit R3): only the user's own failed start, or a failure
 * that can never be fixed by retrying, may turn Flip to Mute off. Everything else keeps the user's
 * choice so the app can come back by itself or with one tap.
 */
internal object InterruptionPolicy {
    fun decide(source: MonitoringStartSource, phase: FailurePhase, reason: MonitoringFailure): InterruptionDecision =
        when {
            reason == MonitoringFailure.SETUP_REQUIRED -> InterruptionDecision.TURN_OFF
            reason == MonitoringFailure.TELEPHONY_UNAVAILABLE -> InterruptionDecision.TURN_OFF
            source == MonitoringStartSource.USER && phase == FailurePhase.STARTING -> InterruptionDecision.TURN_OFF
            else -> InterruptionDecision.KEEP_ON_AND_ALERT
        }
}
