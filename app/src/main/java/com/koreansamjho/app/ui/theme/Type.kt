package com.koreansamjho.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.koreansamjho.app.R
import com.koreansamjho.app.domain.model.Lang

/** Bundled so Korean never falls back to a system font that may not exist on cheap devices. */
val KoreanFont = FontFamily(
    Font(R.font.noto_sans_kr_regular, FontWeight.Normal),
    Font(R.font.noto_sans_kr_bold, FontWeight.Bold),
)

/** Urdu readers expect Nastaliq; Android's default Naskh reads as foreign. */
val UrduFont = FontFamily(Font(R.font.noto_nastaliq_urdu, FontWeight.Normal))

object SamjhoType {
    /** Korean is always the visually dominant element in anything that teaches Korean. */
    fun koreanHero(scale: Float) = TextStyle(
        fontFamily = KoreanFont, fontWeight = FontWeight.Bold,
        fontSize = (40 * scale).sp, lineHeight = (52 * scale).sp
    )
    fun koreanWord(scale: Float) = TextStyle(
        fontFamily = KoreanFont, fontWeight = FontWeight.SemiBold,
        fontSize = (24 * scale).sp, lineHeight = (34 * scale).sp
    )
    fun koreanSentence(scale: Float) = TextStyle(
        fontFamily = KoreanFont, fontWeight = FontWeight.Medium,
        fontSize = (20 * scale).sp, lineHeight = (32 * scale).sp
    )
    fun koreanOption(scale: Float) = TextStyle(
        fontFamily = KoreanFont, fontWeight = FontWeight.Medium,
        fontSize = (18 * scale).sp, lineHeight = (28 * scale).sp
    )
    fun romanization(scale: Float) = TextStyle(
        fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp
    )

    /**
     * Nastaliq has a steeply descending baseline and clips at normal line heights.
     * Extra leading here is the single most common way apps get Urdu visibly wrong.
     */
    fun translation(lang: Lang, scale: Float): TextStyle = when (lang) {
        Lang.UR -> TextStyle(fontFamily = UrduFont, fontSize = (18 * scale).sp, lineHeight = (34 * scale).sp)
        Lang.HI -> TextStyle(fontSize = (16 * scale).sp, lineHeight = (24 * scale).sp)
        Lang.EN -> TextStyle(fontSize = (16 * scale).sp, lineHeight = (22 * scale).sp)
    }
    fun body(lang: Lang, scale: Float): TextStyle = translation(lang, scale)
}

fun appTypography(scale: Float) = Typography().let { t ->
    Typography(
        displaySmall = t.displaySmall.copy(fontSize = (32 * scale).sp),
        headlineMedium = t.headlineMedium.copy(fontSize = (26 * scale).sp),
        headlineSmall = t.headlineSmall.copy(fontSize = (22 * scale).sp, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontSize = (20 * scale).sp, fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontSize = (16 * scale).sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.copy(fontSize = (16 * scale).sp),
        bodyMedium = t.bodyMedium.copy(fontSize = (14 * scale).sp),
        labelLarge = t.labelLarge.copy(fontSize = (15 * scale).sp, fontWeight = FontWeight.SemiBold),
        labelMedium = t.labelMedium.copy(fontSize = (13 * scale).sp),
    )
}
