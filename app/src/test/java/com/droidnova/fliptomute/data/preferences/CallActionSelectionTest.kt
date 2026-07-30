package com.droidnova.fliptomute.data.preferences

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallActionSelectionTest {
    @Test fun muteOnlyIsValid() = assertTrue(
        CallActionSelection(muteRingtone = true, vibratePhone = false).isValid,
    )

    @Test fun vibrateOnlyIsValid() = assertTrue(
        CallActionSelection(muteRingtone = false, vibratePhone = true).isValid,
    )

    @Test fun muteAndVibrateIsValid() = assertTrue(
        CallActionSelection(muteRingtone = true, vibratePhone = true).isValid,
    )

    @Test fun emptySelectionIsInvalid() = assertFalse(
        CallActionSelection(muteRingtone = false, vibratePhone = false).isValid,
    )
}
