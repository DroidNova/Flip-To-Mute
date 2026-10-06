package com.droidnova.fliptomute.utils

/** The colour themes, as Secret Calculator's AppTheme enum (architecture A8). Blue is the default. */
enum class AppTheme(val value: String) {
    BLUE("blue"),
    TEAL("teal"),
    SUNSET("sunset"),
    ;

    companion object {
        fun fromValue(value: String?): AppTheme = entries.firstOrNull { it.value == value } ?: BLUE
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
