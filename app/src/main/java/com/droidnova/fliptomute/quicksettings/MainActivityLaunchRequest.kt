package com.droidnova.fliptomute.quicksettings

import android.content.Intent
import com.droidnova.fliptomute.data.analytics.AnalyticsEvents

sealed interface MainActivityLaunchRequest {
    data object None : MainActivityLaunchRequest
    data object OpenSetupAndEnableMonitoring : MainActivityLaunchRequest
    data object OpenSetupAndResumeMonitoring : MainActivityLaunchRequest

    /** Launcher shortcuts (future features F12). */
    data object PauseForOneHour : MainActivityLaunchRequest
    data object TurnOff : MainActivityLaunchRequest

    /** A flip or recap notification was tapped: show the activity screen. [source] is the analytics label. */
    data class OpenActivity(val source: String) : MainActivityLaunchRequest
}

data class MainActivityLaunchEvent(val sequence: Long = 0L, val request: MainActivityLaunchRequest = MainActivityLaunchRequest.None)

object MainActivityLaunchRequestParser {
    const val OPEN_SETUP_AND_ENABLE_ACTION = "com.droidnova.fliptomute.action.OPEN_SETUP_AND_ENABLE"
    const val OPEN_SETUP_AND_RESUME_ACTION = "com.droidnova.fliptomute.action.OPEN_SETUP_AND_RESUME"
    const val OPEN_ACTIVITY_FROM_FLIP_ACTION = "com.droidnova.fliptomute.action.OPEN_ACTIVITY_FROM_FLIP"
    const val OPEN_ACTIVITY_FROM_RECAP_ACTION = "com.droidnova.fliptomute.action.OPEN_ACTIVITY_FROM_RECAP"
    const val OPEN_ACTIVITY_FROM_SHORTCUT_ACTION = "com.droidnova.fliptomute.action.OPEN_ACTIVITY_FROM_SHORTCUT"
    const val SHORTCUT_PAUSE_ACTION = "com.droidnova.fliptomute.action.SHORTCUT_PAUSE_1_HOUR"
    const val SHORTCUT_TURN_OFF_ACTION = "com.droidnova.fliptomute.action.SHORTCUT_TURN_OFF"

    fun parse(intent: Intent?): MainActivityLaunchRequest =
        parseAction(intent?.action)

    fun parseAction(action: String?): MainActivityLaunchRequest =
        when (action) {
            OPEN_SETUP_AND_ENABLE_ACTION -> MainActivityLaunchRequest.OpenSetupAndEnableMonitoring
            OPEN_SETUP_AND_RESUME_ACTION -> MainActivityLaunchRequest.OpenSetupAndResumeMonitoring
            OPEN_ACTIVITY_FROM_FLIP_ACTION -> MainActivityLaunchRequest.OpenActivity(AnalyticsEvents.SOURCE_FLIP_NOTIFICATION)
            OPEN_ACTIVITY_FROM_RECAP_ACTION -> MainActivityLaunchRequest.OpenActivity(AnalyticsEvents.SOURCE_WEEKLY_RECAP)
            OPEN_ACTIVITY_FROM_SHORTCUT_ACTION -> MainActivityLaunchRequest.OpenActivity(AnalyticsEvents.SOURCE_SHORTCUT)
            SHORTCUT_PAUSE_ACTION -> MainActivityLaunchRequest.PauseForOneHour
            SHORTCUT_TURN_OFF_ACTION -> MainActivityLaunchRequest.TurnOff
            else -> MainActivityLaunchRequest.None
        }
}
