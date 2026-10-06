package com.droidnova.fliptomute.utils.ads

import android.app.Activity
import android.content.Context
import com.droidnova.fliptomute.utils.MonitoringLog
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google's User Messaging Platform (M7-02, architecture X5). Secret Calculator has no consent flow,
 * so this follows Google's documented order: update the consent information on every launch, show
 * the form when required, and request ads only once [ConsentInformation.canRequestAds] is true.
 */
object AdConsent {
    private fun info(context: Context): ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    /** True when a previous session already allows ads, so they can start before this update returns. */
    fun canRequestAds(context: Context): Boolean = info(context).canRequestAds()

    /** "Privacy options" shows in Settings only where the user must be able to change their choice. */
    fun isPrivacyOptionsRequired(context: Context): Boolean =
        info(context).privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Updates consent, shows the form if needed, then reports whether ads may be requested. */
    fun gather(activity: Activity, onDone: (canRequestAds: Boolean) -> Unit) {
        val information = info(activity)
        information.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    if (error != null) MonitoringLog.d(activity, "Consent form: ${error.message}")
                    onDone(information.canRequestAds())
                }
            },
            { error ->
                // Offline or misconfigured: fall back to the stored answer
                MonitoringLog.d(activity, "Consent update: ${error.message}")
                onDone(information.canRequestAds())
            },
        )
    }

    fun showPrivacyOptions(activity: Activity, onDone: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) MonitoringLog.d(activity, "Privacy options: ${error.message}")
            onDone()
        }
    }
}
