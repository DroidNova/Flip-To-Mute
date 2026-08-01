package com.droidnova.fliptomute.quicksettings

import android.content.Intent

sealed interface MainActivityLaunchRequest {
    data object None : MainActivityLaunchRequest
    data object OpenSetupAndEnableMonitoring : MainActivityLaunchRequest
    data object OpenSetupAndResumeMonitoring : MainActivityLaunchRequest
}

data class MainActivityLaunchEvent(val sequence: Long = 0L, val request: MainActivityLaunchRequest = MainActivityLaunchRequest.None)

object MainActivityLaunchRequestParser {
    const val OPEN_SETUP_AND_ENABLE_ACTION = "com.droidnova.fliptomute.action.OPEN_SETUP_AND_ENABLE"
    const val OPEN_SETUP_AND_RESUME_ACTION = "com.droidnova.fliptomute.action.OPEN_SETUP_AND_RESUME"

    fun parse(intent: Intent?): MainActivityLaunchRequest =
        parseAction(intent?.action)

    fun parseAction(action: String?): MainActivityLaunchRequest =
        when (action) {
            OPEN_SETUP_AND_ENABLE_ACTION -> MainActivityLaunchRequest.OpenSetupAndEnableMonitoring
            OPEN_SETUP_AND_RESUME_ACTION -> MainActivityLaunchRequest.OpenSetupAndResumeMonitoring
            else -> MainActivityLaunchRequest.None
        }
}
