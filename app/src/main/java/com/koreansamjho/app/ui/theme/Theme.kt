package com.koreansamjho.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.koreansamjho.app.data.prefs.ThemeMode
import com.koreansamjho.app.domain.model.Lang

val LocalSamjhoColors = staticCompositionLocalOf { LightExtras }
val LocalLang = staticCompositionLocalOf { Lang.EN }
val LocalFontScale = staticCompositionLocalOf { 1.0f }
val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalShowRomanization = staticCompositionLocalOf { true }

@Composable
fun SamjhoTheme(
    lang: Lang,
    themeMode: ThemeMode,
    fontScale: Float = 1.0f,
    reducedMotion: Boolean = false,
    showRomanization: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // Dynamic colour is deliberately not used: a wallpaper-derived palette would break the
    // contrast guarantees we verify and make the brand unrecognisable. See 03-design-system.md.
    val scheme = if (dark) DarkScheme else LightScheme
    val extras = if (dark) DarkExtras else LightExtras

    CompositionLocalProvider(
        LocalSamjhoColors provides extras,
        LocalLang provides lang,
        LocalFontScale provides fontScale,
        LocalReducedMotion provides reducedMotion,
        LocalShowRomanization provides showRomanization,
        LocalLayoutDirection provides if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        MaterialTheme(colorScheme = scheme, typography = appTypography(fontScale), content = content)
    }
}
