package com.droidnova.fliptomute.sensor

import org.junit.Assert.assertEquals
import org.junit.Test

class ProximityValueClassifierTest {
    private val classifier = ProximityValueClassifier()

    @Test fun belowMaximumIsNear() = assertEquals(ProximityState.NEAR, classifier.classify(2f, 5f))
    @Test fun equalToMaximumIsFar() = assertEquals(ProximityState.FAR, classifier.classify(5f, 5f))
    @Test fun closeToMaximumIsFar() = assertEquals(ProximityState.FAR, classifier.classify(4.99f, 5f))
    @Test fun nanIsUnknown() = assertEquals(ProximityState.UNKNOWN, classifier.classify(Float.NaN, 5f))
    @Test fun infinityIsUnknown() = assertEquals(ProximityState.UNKNOWN, classifier.classify(Float.POSITIVE_INFINITY, 5f))
    @Test fun invalidMaximumIsUnknown() = assertEquals(ProximityState.UNKNOWN, classifier.classify(1f, 0f))
    @Test fun negativeValueIsUnknown() = assertEquals(ProximityState.UNKNOWN, classifier.classify(-1f, 5f))
}
