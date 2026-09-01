package com.koreansamjho.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.koreansamjho.app.domain.model.Country
import com.koreansamjho.app.domain.model.Lang
import com.koreansamjho.app.domain.model.Track

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("samjho_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val lang: Lang = Lang.EN,
    val track: Track = Track.GENERAL,
    val country: Country = Country.OTHER,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontScale: Float = 1.0f,
    val reducedMotion: Boolean = false,
    val showRomanization: Boolean = true,
    val dailyGoalMinutes: Int = 15,
    val onboarded: Boolean = false,
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val LANG = stringPreferencesKey("lang")
        val TRACK = stringPreferencesKey("track")
        val COUNTRY = stringPreferencesKey("country")
        val THEME = stringPreferencesKey("theme")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val ROMANIZATION = booleanPreferencesKey("romanization")
        val GOAL = intPreferencesKey("daily_goal")
        val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            lang = Lang.from(p[Keys.LANG]),
            track = Track.from(p[Keys.TRACK]),
            country = Country.from(p[Keys.COUNTRY]),
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            fontScale = p[Keys.FONT_SCALE] ?: 1.0f,
            reducedMotion = p[Keys.REDUCED_MOTION] ?: false,
            showRomanization = p[Keys.ROMANIZATION] ?: true,
            dailyGoalMinutes = p[Keys.GOAL] ?: 15,
            onboarded = p[Keys.ONBOARDED] ?: false,
        )
    }

    suspend fun setLang(l: Lang) = edit { it[Keys.LANG] = l.code }
    suspend fun setTrack(t: Track) = edit { it[Keys.TRACK] = t.name }
    suspend fun setCountry(c: Country) = edit { it[Keys.COUNTRY] = c.name }
    suspend fun setTheme(m: ThemeMode) = edit { it[Keys.THEME] = m.name }
    suspend fun setFontScale(v: Float) = edit { it[Keys.FONT_SCALE] = v }
    suspend fun setReducedMotion(v: Boolean) = edit { it[Keys.REDUCED_MOTION] = v }
    suspend fun setShowRomanization(v: Boolean) = edit { it[Keys.ROMANIZATION] = v }
    suspend fun setDailyGoal(v: Int) = edit { it[Keys.GOAL] = v }
    suspend fun setOnboarded(v: Boolean) = edit { it[Keys.ONBOARDED] = v }

    private suspend fun edit(block: (MutablePreferences) -> Unit) { context.dataStore.edit(block) }
}
