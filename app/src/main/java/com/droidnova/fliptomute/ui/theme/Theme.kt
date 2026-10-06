package com.droidnova.fliptomute.ui.theme

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.droidnova.fliptomute.R
import com.droidnova.fliptomute.utils.AppTheme
import com.droidnova.fliptomute.utils.ThemeMode

fun AppTheme.colorScheme(dark: Boolean): ColorScheme = when (this) {
    AppTheme.BLUE -> if (dark) BlueColorScheme else BlueLightColorScheme
    AppTheme.TEAL -> if (dark) TealColorScheme else TealLightColorScheme
    AppTheme.SUNSET -> if (dark) SunsetColorScheme else SunsetLightColorScheme
    AppTheme.FOREST -> if (dark) ForestColorScheme else ForestLightColorScheme
    AppTheme.ROSE -> if (dark) RoseColorScheme else RoseLightColorScheme
    AppTheme.MIDNIGHT -> if (dark) MidnightColorScheme else MidnightLightColorScheme
}

@StringRes
fun AppTheme.labelRes(): Int = when (this) {
    AppTheme.BLUE -> R.string.theme_blue
    AppTheme.TEAL -> R.string.theme_teal
    AppTheme.SUNSET -> R.string.theme_sunset
    AppTheme.FOREST -> R.string.theme_forest
    AppTheme.ROSE -> R.string.theme_rose
    AppTheme.MIDNIGHT -> R.string.theme_midnight
}

/**
 * Root theme for every screen, built like Secret Calculator's: the colour theme and light or dark
 * mode picked in Settings, the shared typography, and status and navigation bar icons that match.
 * Wallpaper colours are not used, so the brand always shows (design spec 3.1).
 */
@Composable
fun FlipToMuteTheme(
    appTheme: AppTheme = Appearance.appTheme,
    darkTheme: Boolean = isDark(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? Activity
        SideEffect {
            val window = activity?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(LocalStateColors provides if (darkTheme) DarkStateColors else LightStateColors) {
        MaterialTheme(
            colorScheme = appTheme.colorScheme(darkTheme),
            typography = AppTypography,
            content = content,
        )
    }
}

@Composable
private fun isDark(): Boolean {
    val system = isSystemInDarkTheme()
    return when (Appearance.themeMode) {
        ThemeMode.SYSTEM -> system
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}
