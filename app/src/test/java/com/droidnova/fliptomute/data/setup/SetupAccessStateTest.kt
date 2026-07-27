package com.droidnova.fliptomute.data.setup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupAccessStateTest {
    private val granted = SetupAccessStatus.GRANTED
    private val missing = SetupAccessStatus.NOT_GRANTED

    @Test fun phoneMissingIsIncomplete() = assertFalse(state(phone = missing).isSetupComplete)
    @Test fun soundMissingIsIncomplete() = assertFalse(state(sound = missing).isSetupComplete)
    @Test fun notificationsMissingIsIncomplete() = assertFalse(state(notifications = missing).isSetupComplete)
    @Test fun allGrantedIsComplete() = assertTrue(state().isSetupComplete)

    private fun state(
        phone: SetupAccessStatus = granted,
        sound: SetupAccessStatus = granted,
        notifications: SetupAccessStatus = granted,
    ) = SetupAccessState(phone, sound, notifications)
}
