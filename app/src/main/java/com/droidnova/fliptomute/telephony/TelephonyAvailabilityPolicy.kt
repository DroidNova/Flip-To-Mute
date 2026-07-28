package com.droidnova.fliptomute.telephony

/** Feature flags are diagnostic only; a manager permits a definitive registration attempt. */
internal object TelephonyAvailabilityPolicy {
    @Suppress("UNUSED_PARAMETER")
    fun canAttemptRegistration(
        telephonyManagerAvailable: Boolean,
        hasBaseFeature: Boolean,
        hasCallingFeature: Boolean,
    ): Boolean = telephonyManagerAvailable
}

internal enum class TelephonyRegistrationFailure { PERMISSION_REQUIRED, UNSUPPORTED, REGISTRATION_FAILED }

internal object TelephonyRegistrationResult {
    fun resolve(
        attemptCount: Int,
        unsupportedCount: Int,
        hadSecurityException: Boolean,
    ): TelephonyRegistrationFailure = when {
        hadSecurityException -> TelephonyRegistrationFailure.PERMISSION_REQUIRED
        attemptCount > 0 && unsupportedCount == attemptCount -> TelephonyRegistrationFailure.UNSUPPORTED
        else -> TelephonyRegistrationFailure.REGISTRATION_FAILED
    }
}
