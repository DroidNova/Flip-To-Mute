package com.droidnova.fliptomute.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.droidnova.fliptomute.utils.AppTheme
import com.droidnova.fliptomute.utils.ThemeMode

/**
 * The look the user picked in Settings, as Compose state, copied from Secret Calculator: changing it
 * redraws every screen at once without restarting the activity. Loaded once in Application.onCreate,
 * which Android runs before any activity, service or receiver (architecture X1).
 */
object Appearance {
    var appTheme: AppTheme by mutableStateOf(AppTheme.BLUE)
        private set
    var themeMode: ThemeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    private var appContext: Context? = null

    fun load(context: Context) {
        appContext = context.applicationContext
        val prefs = prefs() ?: return
        appTheme = AppTheme.fromValue(prefs.getString(KEY_THEME, null))
        themeMode = ThemeMode.fromValue(prefs.getString(KEY_THEME_MODE, null))
    }

    fun updateTheme(theme: AppTheme) {
        prefs()?.edit { putString(KEY_THEME, theme.value) }
        appTheme = theme
    }

    fun updateThemeMode(mode: ThemeMode) {
        prefs()?.edit { putString(KEY_THEME_MODE, mode.value) }
        themeMode = mode
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val PREFS = "appearance"
    private const val KEY_THEME = "app_theme"
    private const val KEY_THEME_MODE = "theme_mode"
}
