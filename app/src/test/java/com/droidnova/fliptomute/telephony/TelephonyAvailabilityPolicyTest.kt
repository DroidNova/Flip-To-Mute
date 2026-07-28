package com.droidnova.fliptomute.telephony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelephonyAvailabilityPolicyTest {
    @Test fun api33BaseFeatureWithoutCallingFeatureStillAllowsRegistration() {
        assertTrue(canAttempt(baseFeature = true, callingFeature = false, managerAvailable = true))
    }

    @Test fun api33CallingFeatureWithoutBaseFeatureStillAllowsRegistration() {
        assertTrue(canAttempt(baseFeature = false, callingFeature = true, managerAvailable = true))
    }

    @Test fun absentFeatureFlagsStillAllowSafeAttemptWhenManagerExists() {
        assertTrue(canAttempt(baseFeature = false, callingFeature = false, managerAvailable = true))
    }

    @Test fun missingManagerIsUnavailableRegardlessOfFeatures() {
        assertFalse(canAttempt(baseFeature = true, callingFeature = true, managerAvailable = false))
    }

    @Test fun securityFailureTakesPrecedenceOverUnsupportedAttempts() {
        assertEquals(
            TelephonyRegistrationFailure.PERMISSION_REQUIRED,
            TelephonyRegistrationResult.resolve(2, 1, hadSecurityException = true),
        )
    }

    @Test fun allUnsupportedAttemptsAreUnavailable() {
        assertEquals(
            TelephonyRegistrationFailure.UNSUPPORTED,
            TelephonyRegistrationResult.resolve(2, 2, hadSecurityException = false),
        )
    }

    @Test fun illegalStateOrOtherFailureIsRegistrationFailure() {
        assertEquals(
            TelephonyRegistrationFailure.REGISTRATION_FAILED,
            TelephonyRegistrationResult.resolve(2, 1, hadSecurityException = false),
        )
    }

    private fun canAttempt(
        baseFeature: Boolean,
        callingFeature: Boolean,
        managerAvailable: Boolean,
    ) = TelephonyAvailabilityPolicy.canAttemptRegistration(managerAvailable, baseFeature, callingFeature)
}
