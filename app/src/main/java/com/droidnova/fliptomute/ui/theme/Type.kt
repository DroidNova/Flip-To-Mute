package com.droidnova.fliptomute.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.droidnova.fliptomute.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/**
 * Copied from Secret Calculator, which shares it with Speedometer: Outfit for display, headline and title
 * styles, the platform sans for body and labels so longer text stays easy to read. Outfit is a
 * downloadable font; until it arrives (or without Play services) the platform sans is used at the
 * same weights.
 */
private val outfit = GoogleFont("Outfit")

val DisplayFontFamily = FontFamily(
    Font(googleFont = outfit, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = outfit, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = outfit, fontProvider = provider, weight = FontWeight.Bold),
)

private val BodyFontFamily: FontFamily = FontFamily.Default

private val Baseline = Typography()

internal val AppTypography = Typography(
    displayLarge = Baseline.displayLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
    displayMedium = Baseline.displayMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
    displaySmall = Baseline.displaySmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold),
    headlineLarge = Baseline.headlineLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    headlineMedium = Baseline.headlineMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    headlineSmall = Baseline.headlineSmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = Baseline.titleLarge.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = Baseline.titleMedium.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = Baseline.titleSmall.copy(fontFamily = DisplayFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = Baseline.bodyLarge.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal),
    bodyMedium = Baseline.bodyMedium.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal),
    bodySmall = Baseline.bodySmall.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Normal),
    labelLarge = Baseline.labelLarge.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium),
    labelMedium = Baseline.labelMedium.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = Baseline.labelSmall.copy(fontFamily = BodyFontFamily, fontWeight = FontWeight.Medium),
)
