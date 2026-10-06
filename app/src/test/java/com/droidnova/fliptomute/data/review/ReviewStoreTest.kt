package com.droidnova.fliptomute.data.review

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReviewStoreTest {
    private val store = ReviewStore(ApplicationProvider.getApplicationContext())

    @Test fun startsEmpty() {
        val state = store.state()
        assertEquals(0, state.launchCount)
        assertEquals(0, state.valueMoments)
        assertEquals(0, state.askCount)
        assertNull(state.lastAskedLaunch)
        assertNull(state.lastAdAt)
    }

    @Test fun countsLaunchesAndValueMoments() {
        repeat(3) { store.recordLaunch() }
        repeat(2) { store.recordValueMoment() }
        assertEquals(3, store.state().launchCount)
        assertEquals(2, store.state().valueMoments)
    }

    @Test fun markAskedRemembersTheLaunchAndCountsTheAsk() {
        repeat(5) { store.recordLaunch() }
        store.markAsked(store.state())
        assertEquals(1, store.state().askCount)
        assertEquals(5, store.state().lastAskedLaunch)
    }

    @Test fun markAskedStartsTheCooldown_andAnAdStartsItsQuietTime() {
        assertNull(store.state().lastAskedAt)
        store.markAsked(store.state(), now = 7_000L)
        assertEquals(7_000L, store.state().lastAskedAt)

        store.recordAdClosed(now = 9_000L)
        assertEquals(9_000L, store.state().lastAdAt)
    }
}
