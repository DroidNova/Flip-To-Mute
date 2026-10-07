package com.droidnova.fliptomute.utils

/**
 * The colour themes, as Secret Calculator's AppTheme enum (architecture A8). Teal is the default
 * (owner, 2026-10-07); Blue and Sunset are the other two that are always open.
 * A theme with [unlockAt] above 0 is earned: it opens at that many calls silenced (future features F36).
 */
enum class AppTheme(val value: String, val unlockAt: Int = 0) {
    TEAL("teal"),
    BLUE("blue"),
    SUNSET("sunset"),
    FOREST("forest", unlockAt = 10),
    ROSE("rose", unlockAt = 25),
    MIDNIGHT("midnight", unlockAt = 50),
    ;

    companion object {
        fun fromValue(value: String?): AppTheme = entries.firstOrNull { it.value == value } ?: TEAL
    }
}

/** Light or dark, independent of the colour theme. System is the default (design spec 3.1). */
enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        fun fromValue(value: String?): ThemeMode = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}
