package com.opendroid.ai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * NOVA visual system: midnight navy surfaces, cool cyan highlights and restrained violet.
 * A single semantic palette keeps every screen visually consistent.
 */
data class OpenDroidColors(
    val background: Color,
    val surface: Color,
    val cardBackground: Color,
    val borderColor: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentNeonGreen: Color,
    val accentGreenButton: Color,
    val accentPurple: Color,
    val accentCyan: Color,
    val accentRed: Color,
    val accentOrange: Color = Color(0xFFFFB454),
    val isDark: Boolean
)

val DarkPalette = OpenDroidColors(
    background = Color(0xFF080D18),
    surface = Color(0xFF101A2B),
    cardBackground = Color(0xFF152238),
    borderColor = Color(0xFF263751),
    textPrimary = Color(0xFFF4F7FC),
    textSecondary = Color(0xFF9AAAC0),
    accentNeonGreen = Color(0xFF67D8E8),
    accentGreenButton = Color(0xFF42BFD2),
    accentPurple = Color(0xFF9B8CFF),
    accentCyan = Color(0xFF67D8E8),
    accentRed = Color(0xFFFF6B78),
    accentOrange = Color(0xFFFFB454),
    isDark = true
)

val LightPalette = OpenDroidColors(
    background = Color(0xFFF3F6FB),
    surface = Color(0xFFFFFFFF),
    cardBackground = Color(0xFFFFFFFF),
    borderColor = Color(0xFFDCE4EF),
    textPrimary = Color(0xFF142238),
    textSecondary = Color(0xFF65758B),
    accentNeonGreen = Color(0xFF168FA3),
    accentGreenButton = Color(0xFF168FA3),
    accentPurple = Color(0xFF7564D8),
    accentCyan = Color(0xFF168FA3),
    accentRed = Color(0xFFD94A5A),
    accentOrange = Color(0xFFB87520),
    isDark = false
)

val LocalOpenDroidColors = compositionLocalOf { DarkPalette }

object AppTheme {
    val colors: OpenDroidColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOpenDroidColors.current
}

val DarkBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.background

val DarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.surface

val CardBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.cardBackground

val BorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.borderColor

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textSecondary

val AccentNeonGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.accentNeonGreen

val AccentGreenButton: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.accentGreenButton

val AccentPurple: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.accentPurple

val AccentCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.accentCyan

val AccentRed: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.accentRed

val AccentOrange: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.accentOrange
