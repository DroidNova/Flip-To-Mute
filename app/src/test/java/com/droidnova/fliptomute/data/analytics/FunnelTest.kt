package com.droidnova.fliptomute.data.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Ported from Secret Calculator's FunnelTest, with Flip to Mute's milestones. */
@RunWith(RobolectricTestRunner::class)
class FunnelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val events = mutableListOf<Pair<String, Map<String, String>>>()
    private lateinit var funnel: Funnel

    private val names get() = events.map { it.first }

    @Before
    fun setUp() {
        context.getSharedPreferences("funnel", Context.MODE_PRIVATE).edit().clear().commit()
        funnel = Funnel(context) { event, params -> events += event to params }
    }

    @Test
    fun existingUsers_reportNothing() {
        funnel.onboardingComplete(triedFlip = true)
        funnel.flipApplied()
        funnel.appOpened(now = START + DAY + 1)
        funnel.appOpened(now = START + 7 * DAY + 1)
        assertEquals(emptyList<String>(), names)
    }

    @Test
    fun newUser_walksTheFunnelOnce() {
        funnel.setupComplete(now = START)
        funnel.setupComplete(now = START + 5)
        funnel.onboardingComplete(triedFlip = false)
        funnel.onboardingComplete(triedFlip = true)
        funnel.flipApplied()
        funnel.flipApplied()
        funnel.appOpened(now = START + DAY / 2)
        funnel.appOpened(now = START + DAY + 1)
        funnel.appOpened(now = START + DAY + 2)
        funnel.appOpened(now = START + 7 * DAY + 1)
        funnel.appOpened(now = START + 7 * DAY + 2)

        assertEquals(
            listOf(Funnel.SETUP_COMPLETE, Funnel.ONBOARDING_COMPLETE, Funnel.FIRST_FLIP, Funnel.RETURN_D1, Funnel.RETURN_D7),
            names,
        )
        assertEquals(Funnel.VALUE_SKIPPED, events[1].second[Funnel.PARAM_TRY_FLIP])
    }

    @Test
    fun returnsOutsideDayOneAndSeven_areNotReported() {
        funnel.setupComplete(now = START)
        funnel.appOpened(now = START + 3 * DAY)
        funnel.appOpened(now = START + 8 * DAY)
        assertEquals(listOf(Funnel.SETUP_COMPLETE), names)
    }

    @Test
    fun funnelState_survivesANewInstance() {
        funnel.setupComplete(now = START)
        funnel.flipApplied()
        val again = Funnel(context) { event, params -> events += event to params }
        again.setupComplete(now = START + DAY)
        again.flipApplied()
        again.appOpened(now = START + DAY + 1)
        assertEquals(listOf(Funnel.SETUP_COMPLETE, Funnel.FIRST_FLIP, Funnel.RETURN_D1), names)
    }

    private companion object {
        const val DAY = 24 * 60 * 60 * 1000L
        const val START = 1_700_000_000_000L
    }
}
