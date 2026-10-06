package com.droidnova.fliptomute.utils.about_utils

object AppConstants {
    const val PLAY_CONSOLE_URL = "https://play.google.com/store/apps/dev?id=8766579115812433944"
    const val SUPPORT_EMAIL = "droidnova7@gmail.com"
    const val WHATSAPP_COMMUNITY_GROUP_URL = "https://chat.whatsapp.com/Lx6lcBXpN3L9EzibKGg1Zp"
    const val INSTAGRAM_COMMUNITY_URL = "https://www.instagram.com/droid_nova?igsh=MWdjMGtsZGNmMm45dg=="
    /** Flip to Mute's privacy policy. Empty until the owner provides it (decision D7); Settings hides the row meanwhile. */
    const val PRIVACY_POLICY_URL = "https://sites.google.com/view/fliptomute/home"

    /**
     * The app's licence key from Play Console (Monetisation setup), used to check a "remove ads"
     * purchase. Empty until the owner pastes it; purchases are then accepted without the check,
     * as Secret Calculator does with an empty key.
     */
    const val PLAY_STORE_LICENSE_KEY = ""

    /**
     * Flip to Mute Premium (no ads, every colour theme) is built but switched off (owner, 2026-10-07).
     * To switch it on: create the in-app product `one_time_remove_ads` in Play Console, paste the
     * licence key above, and set this to true. While false, Settings has no Premium row, the app
     * never connects to Play Billing, and the extra themes are earned or opened by a rewarded ad.
     */
    const val PREMIUM_ENABLED = false
}
