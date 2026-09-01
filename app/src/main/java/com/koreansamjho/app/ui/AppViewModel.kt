package com.koreansamjho.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.koreansamjho.app.data.prefs.Settings
import com.koreansamjho.app.data.prefs.SettingsStore
import com.koreansamjho.app.data.prefs.ThemeMode
import com.koreansamjho.app.domain.model.Country
import com.koreansamjho.app.domain.model.Lang
import com.koreansamjho.app.domain.model.Track

class AppViewModel(private val store: SettingsStore) : ViewModel() {

    val settings: StateFlow<Settings> =
        store.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    fun setLang(l: Lang) = viewModelScope.launch { store.setLang(l) }
    fun setTrack(t: Track) = viewModelScope.launch { store.setTrack(t) }
    fun setCountry(c: Country) = viewModelScope.launch { store.setCountry(c) }
    fun setTheme(m: ThemeMode) = viewModelScope.launch { store.setTheme(m) }
    fun setFontScale(v: Float) = viewModelScope.launch { store.setFontScale(v) }
    fun setReducedMotion(v: Boolean) = viewModelScope.launch { store.setReducedMotion(v) }
    fun setShowRomanization(v: Boolean) = viewModelScope.launch { store.setShowRomanization(v) }
    fun setDailyGoal(v: Int) = viewModelScope.launch { store.setDailyGoal(v) }
    fun completeOnboarding() = viewModelScope.launch { store.setOnboarded(true) }
}
