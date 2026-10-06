package com.droidnova.fliptomute.data.premium

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers the "remove ads" purchase between launches (future features F19), so ads stay away
 * before Play has answered. Secret Calculator keeps this flag in its global PreferenceUtil; here it
 * is an injected store, as every other setting is (architecture X1). Play remains the authority:
 * the flag is rewritten each time the purchases are read.
 */
@Singleton
class PremiumStore @Inject constructor(@ApplicationContext context: Context) {
    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var hasBoughtPremium: Boolean
        get() = prefs.getBoolean(KEY_PREMIUM, false)
        set(value) = prefs.edit { putBoolean(KEY_PREMIUM, value) }

    private companion object {
        const val PREFS = "premium"
        const val KEY_PREMIUM = "has_bought_premium"
    }
}
