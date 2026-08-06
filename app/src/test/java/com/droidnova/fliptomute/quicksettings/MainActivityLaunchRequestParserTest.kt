package com.droidnova.fliptomute.quicksettings

import org.junit.Assert.assertEquals
import org.junit.Test

class MainActivityLaunchRequestParserTest {
    @Test fun normalLaunchHasNoRequest() {
        assertEquals(MainActivityLaunchRequest.None, MainActivityLaunchRequestParser.parseAction(null))
        assertEquals(MainActivityLaunchRequest.None, MainActivityLaunchRequestParser.parseAction("android.intent.action.MAIN"))
    }

    @Test fun tileSetupActionIsRecognized() {
        assertEquals(
            MainActivityLaunchRequest.OpenSetupAndEnableMonitoring,
            MainActivityLaunchRequestParser.parseAction(MainActivityLaunchRequestParser.OPEN_SETUP_AND_ENABLE_ACTION),
        )
        assertEquals(
            MainActivityLaunchRequest.OpenSetupAndResumeMonitoring,
            MainActivityLaunchRequestParser.parseAction(MainActivityLaunchRequestParser.OPEN_SETUP_AND_RESUME_ACTION),
        )
    }
}
