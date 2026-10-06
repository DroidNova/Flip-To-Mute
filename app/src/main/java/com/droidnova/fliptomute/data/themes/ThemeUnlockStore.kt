package com.droidnova.fliptomute.data.themes

import android.content.Context
import androidx.core.content.edit
import com.droidnova.fliptomute.ui.theme.Appearance
import com.droidnova.fliptomute.utils.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether a colour theme may be used (future features F36, F34). Earned themes open for good at
 * their milestone, or for a week after a rewarded ad. Pure, so every boundary is tested.
 */
object ThemeUnlockPolicy {
    const val AD_UNLOCK_MS = 7 * 24 * 60 * 60 * 1000L

    fun isUnlocked(theme: AppTheme, totalFlips: Int, adUnlockedUntil: Long, now: Long): Boolean =
        theme.unlockAt == 0 || totalFlips >= theme.unlockAt || now < adUnlockedUntil
}

/** Remembers the week a rewarded ad bought for a theme; a small store in Secret Calculator's style. */
@Singleton
class ThemeUnlockStore(
    context: Context,
    private val now: () -> Long,
) {
    @Inject constructor(@ApplicationContext context: Context) : this(context, System::currentTimeMillis)

    private val appContext = context.applicationContext
    private val prefs get() = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isUnlocked(theme: AppTheme, totalFlips: Int): Boolean =
        ThemeUnlockPolicy.isUnlocked(theme, totalFlips, prefs.getLong(theme.value, 0L), now())

    fun lockedThemes(totalFlips: Int): Set<AppTheme> = AppTheme.entries.filterNot { isUnlocked(it, totalFlips) }.toSet()

    /** A rewarded ad was watched to the end. */
    fun unlockWithAd(theme: AppTheme) = prefs.edit { putLong(theme.value, now() + ThemeUnlockPolicy.AD_UNLOCK_MS) }

    /** The week from an ad ran out before the milestone was reached: back to the default theme. */
    fun enforce(totalFlips: Int) {
        if (!isUnlocked(Appearance.appTheme, totalFlips)) Appearance.updateTheme(AppTheme.BLUE)
    }

    private companion object {
        const val PREFS = "theme_unlocks"
    }
}
