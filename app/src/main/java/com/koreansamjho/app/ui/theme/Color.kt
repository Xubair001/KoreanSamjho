package com.koreansamjho.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Deep indigo drawn from Korean dancheong palettes, with a restrained vermilion accent.
// Deliberately not flag colours — see docs/03-design-system.md 3.3.

val LightScheme = lightColorScheme(
    primary = Color(0xFF2B4C8C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001945),
    secondary = Color(0xFF4A5B7C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD9E2F9),
    onSecondaryContainer = Color(0xFF061A35),
    tertiary = Color(0xFF8C4A3F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD3),
    onTertiaryContainer = Color(0xFF3A0A04),
    background = Color(0xFFFBFCFF),
    onBackground = Color(0xFF1A1B1F),
    surface = Color(0xFFFBFCFF),
    onSurface = Color(0xFF1A1B1F),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44464F),
    outline = Color(0xFF757780),
    outlineVariant = Color(0xFFC5C6D0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFFA9C0F0),
    onPrimary = Color(0xFF0E2350),
    primaryContainer = Color(0xFF123468),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFB7C6E6),
    onSecondary = Color(0xFF1F2F4B),
    secondaryContainer = Color(0xFF354561),
    onSecondaryContainer = Color(0xFFD9E2F9),
    tertiary = Color(0xFFF0B4A9),
    onTertiary = Color(0xFF561F16),
    tertiaryContainer = Color(0xFF70352A),
    onTertiaryContainer = Color(0xFFFFDAD3),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44464F),
    onSurfaceVariant = Color(0xFFC5C6D0),
    outline = Color(0xFF8F9099),
    outlineVariant = Color(0xFF44464F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Semantic colours Material 3 does not define. Correct/incorrect are never signalled
 * by colour alone — every use is paired with an icon and a text label (§26).
 */
data class SamjhoColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val koreanAccent: Color,
)

val LightExtras = SamjhoColors(
    success = Color(0xFF2E6B41), onSuccess = Color(0xFFFFFFFF), successContainer = Color(0xFFB4F0C4),
    warning = Color(0xFF8A5A00), onWarning = Color(0xFFFFFFFF), warningContainer = Color(0xFFFFDEA8),
    koreanAccent = Color(0xFF2B4C8C),
)
val DarkExtras = SamjhoColors(
    success = Color(0xFF9BD5AC), onSuccess = Color(0xFF00391C), successContainer = Color(0xFF1B5130),
    warning = Color(0xFFF2C25B), onWarning = Color(0xFF422C00), warningContainer = Color(0xFF684200),
    koreanAccent = Color(0xFFA9C0F0),
)
