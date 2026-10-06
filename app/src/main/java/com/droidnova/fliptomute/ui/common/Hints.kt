package com.droidnova.fliptomute.ui.common

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Discovery cards on Home (design spec 4.4), shown one at a time. */
enum class HomeHint { START_AFTER_RESTART, FLIP_TO_LOCK, QUICK_SETTINGS_TILE }

/**
 * Remembers which Home cards were dismissed, and the other one-time things Home shows. Copied from
 * Secret Calculator's HintStore (architecture A12) and extended with the "what's new" version, the
 * tile flag and the battery card snooze.
 */
@Singleton
class HintStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs get() = context.getSharedPreferences("hints", Context.MODE_PRIVATE)

    fun isDismissed(hint: HomeHint): Boolean = prefs.getBoolean(hint.name, false)

    fun dismiss(hint: HomeHint) = prefs.edit { putBoolean(hint.name, true) }

    fun dismissedHints(): Set<HomeHint> = HomeHint.entries.filter(::isDismissed).toSet()

    /** The last version whose "what's new" was seen (or skipped by a fresh install). */
    var whatsNewSeenVersion: Long
        get() = prefs.getLong(KEY_WHATS_NEW, 0L)
        set(value) = prefs.edit { putLong(KEY_WHATS_NEW, value) }

    /** Set by the tile service: the Quick Settings tile is in the user's panel. */
    var tileAdded: Boolean
        get() = prefs.getBoolean(KEY_TILE_ADDED, false)
        set(value) = prefs.edit { putBoolean(KEY_TILE_ADDED, value) }

    /** "Later" on the battery card hides it until this time. */
    var batteryCardHiddenUntil: Long
        get() = prefs.getLong(KEY_BATTERY_HIDDEN_UNTIL, 0L)
        set(value) = prefs.edit { putLong(KEY_BATTERY_HIDDEN_UNTIL, value) }

    private companion object {
        const val KEY_WHATS_NEW = "whats_new_seen_version"
        const val KEY_TILE_ADDED = "tile_added"
        const val KEY_BATTERY_HIDDEN_UNTIL = "battery_card_hidden_until"
    }
}
