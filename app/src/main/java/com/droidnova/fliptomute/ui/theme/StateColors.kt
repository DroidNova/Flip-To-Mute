package com.droidnova.fliptomute.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** One state's colours: the accent and its container. */
data class StateColor(val accent: Color, val container: Color, val onContainer: Color)

/**
 * Colours for Flip to Mute's own states (design spec 3.1). They stay the same in every colour theme,
 * so "on" always reads as on. State is never shown by colour alone: every use also has an icon and text.
 */
data class StateColors(
    val on: StateColor,
    val paused: StateColor,
    val off: StateColor,
    val attention: StateColor,
)

internal val LightStateColors = StateColors(
    on = StateColor(Color(0xFF1B8A4B), Color(0xFFD5F5E0), Color(0xFF00391B)),
    paused = StateColor(Color(0xFF8A5A00), Color(0xFFFFE2B5), Color(0xFF2C1A00)),
    off = StateColor(Color(0xFF74777F), Color(0xFFE1E2EC), Color(0xFF191C22)),
    attention = StateColor(Color(0xFFBA1A1A), Color(0xFFFFDAD6), Color(0xFF410002)),
)

internal val DarkStateColors = StateColors(
    on = StateColor(Color(0xFF6FDB9A), Color(0xFF0F5131), Color(0xFFD5F5E0)),
    paused = StateColor(Color(0xFFFFB95C), Color(0xFF5C3F00), Color(0xFFFFE2B5)),
    off = StateColor(Color(0xFF8E9099), Color(0xFF2B2D34), Color(0xFFE1E2EC)),
    attention = StateColor(Color(0xFFFFB4AB), Color(0xFF93000A), Color(0xFFFFDAD6)),
)

internal val LocalStateColors = staticCompositionLocalOf { LightStateColors }

/** The state colours for the current light or dark mode. */
val stateColors: StateColors
    @Composable @ReadOnlyComposable get() = LocalStateColors.current
