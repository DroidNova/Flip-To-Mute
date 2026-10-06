package com.droidnova.fliptomute.data.analytics

/**
 * Event and parameter names from v2.0 plan section 10. Values are short anonymous labels only:
 * never phone numbers, contact names, call times or call details.
 */
object AnalyticsEvents {
    const val SERVICE_STATE_CHANGED = "service_state_changed"
    const val SERVICE_INTERRUPTED = "service_interrupted"
    const val AUTO_RESUME = "auto_resume"
    const val FLIP_APPLIED = "flip_applied"

    const val PARAM_STATE = "state"
    const val PARAM_SOURCE = "source"
    const val PARAM_REASON = "reason"
    const val PARAM_PHASE = "phase"
    const val PARAM_SUCCESS = "success"
    const val PARAM_ACTION = "action"
    const val PARAM_FLAT_ONLY = "flat_only"
    const val PARAM_POCKET_PROTECTION = "pocket_protection"

    const val STATE_ON = "on"
    const val STATE_OFF = "off"
    const val STATE_PAUSED = "paused"
    const val SOURCE_USER = "user"
}
