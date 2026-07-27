package com.droidnova.fliptomute.sensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccelerometerGravityFilterTest {
    @Test
    fun firstSampleInitializesAndStableSamplesRemainFinite() {
        val filter = AccelerometerGravityFilter()
        filter.update(0f, 0f, 9.81f)
        repeat(20) { filter.update(0f, 0f, 9.81f) }

        assertTrue(filter.isInitialized)
        assertEquals(9.81f, filter.z, 0.001f)
        assertTrue(filter.x.isFinite() && filter.y.isFinite() && filter.z.isFinite())
    }

    @Test
    fun repeatedSamplesConvergeAndResetClearsState() {
        val filter = AccelerometerGravityFilter()
        filter.update(0f, 0f, 0f)
        repeat(30) { filter.update(0f, 0f, 9.81f) }
        assertTrue(filter.z > 9f)

        filter.reset()
        assertFalse(filter.isInitialized)
        assertEquals(0f, filter.z, 0f)
    }
}
