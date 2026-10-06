package com.droidnova.fliptomute.data.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPolicyTest {

    private val now = 1_000_000_000L

    /** A user who has met every condition. */
    private val ready = ReviewState(
        launchCount = 3,
        valueMoments = 3,
        askCount = 0,
        lastAskedLaunch = null,
        lastAdAt = null,
    )

    @Test
    fun satisfiedRegularUser_isAsked() = assertTrue(ReviewPolicy.shouldAsk(ready, now))

    @Test
    fun firstTwoLaunches_areNotAsked() {
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(launchCount = 1), now))
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(launchCount = 2), now))
    }

    @Test
    fun onlyEveryThirdLaunch_isAsked() {
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(launchCount = 4), now))
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(launchCount = 5), now))
        assertTrue(ReviewPolicy.shouldAsk(ready.copy(launchCount = 6), now))
    }

    @Test
    fun tooFewGoodMoments_isNotAsked() = assertFalse(ReviewPolicy.shouldAsk(ready.copy(valueMoments = 2), now))

    @Test
    fun rightAfterAnAd_isNotAsked() =
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(lastAdAt = now - 60_000L), now))

    @Test
    fun wellAfterAnAd_isAsked() =
        assertTrue(ReviewPolicy.shouldAsk(ready.copy(lastAdAt = now - ReviewPolicy.AD_QUIET_MS - 1), now))

    @Test
    fun askedThisLaunch_isNotAskedAgain() =
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(launchCount = 6, askCount = 1, lastAskedLaunch = 6), now))

    @Test
    fun nextThirdLaunch_isAskedAgain() =
        assertTrue(ReviewPolicy.shouldAsk(ready.copy(launchCount = 6, askCount = 1, lastAskedLaunch = 3), now))

    @Test
    fun afterMaxAsks_isNeverAskedAgain() =
        assertFalse(ReviewPolicy.shouldAsk(ready.copy(launchCount = 12, askCount = ReviewPolicy.MAX_ASKS, lastAskedLaunch = 9), now))
}
