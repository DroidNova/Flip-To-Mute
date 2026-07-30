package com.droidnova.fliptomute.quicksettings

import android.content.Intent

sealed interface MainActivityLaunchRequest {
    data object None : MainActivityLaunchRequest
    data object OpenSetupAndEnableMonitoring : MainActivityLaunchRequest
}

object MainActivityLaunchRequestParser {
    const val OPEN_SETUP_AND_ENABLE_ACTION = "com.droidnova.fliptomute.action.OPEN_SETUP_AND_ENABLE"

    fun parse(intent: Intent?): MainActivityLaunchRequest =
        parseAction(intent?.action)

    fun parseAction(action: String?): MainActivityLaunchRequest =
        if (action == OPEN_SETUP_AND_ENABLE_ACTION) {
            MainActivityLaunchRequest.OpenSetupAndEnableMonitoring
        } else {
            MainActivityLaunchRequest.None
        }
}
