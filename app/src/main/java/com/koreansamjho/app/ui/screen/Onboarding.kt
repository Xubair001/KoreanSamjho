package com.koreansamjho.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koreansamjho.app.R
import com.koreansamjho.app.domain.model.Country
import com.koreansamjho.app.domain.model.Lang
import com.koreansamjho.app.domain.model.Track
import com.koreansamjho.app.ui.AppViewModel
import com.koreansamjho.app.ui.components.ForceLtr
import com.koreansamjho.app.ui.components.SamjhoCard
import com.koreansamjho.app.ui.theme.LocalSamjhoColors
import com.koreansamjho.app.ui.theme.UrduFont

/**
 * Three steps: language, country, goal. Country matters because EPS is open to
 * Pakistan and not to India — routing an Indian learner to EPS prep would be
 * preparing them for a visa route that does not exist for them (docs/01-research.md 1.1).
 */
@Composable
fun OnboardingScreen(vm: AppViewModel, onDone: () -> Unit) {
    // Choosing a language calls setApplicationLocales, which recreates the activity.
    // These must be saveable or the learner is thrown back to step 1 the moment they
    // pick Urdu or Hindi. Enums are stored by name because they are not Parcelable.
    var step by rememberSaveable { mutableIntStateOf(0) }
    var langName by rememberSaveable { mutableStateOf(Lang.EN.name) }
    var countryName by rememberSaveable { mutableStateOf<String?>(null) }
    var trackName by rememberSaveable { mutableStateOf<String?>(null) }
    val lang = Lang.valueOf(langName)
    val country = countryName?.let { Country.valueOf(it) }
    val track = trackName?.let { Track.valueOf(it) }

    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(24.dp))
        ForceLtr {
            Text("한", style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        LinearProgressIndicator(
            progress = { (step + 1) / 3f },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        when (step) {
            0 -> {
                Text(stringResource(R.string.onb_lang_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.onb_lang_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                // Each option is written in its own language and script.
                ChoiceRow("English", "English", lang == Lang.EN) { langName = Lang.EN.name; vm.setLang(Lang.EN) }
                ChoiceRow("اردو", "Urdu", lang == Lang.UR, titleFont = UrduFont) {
                    langName = Lang.UR.name; vm.setLang(Lang.UR)
                }
                ChoiceRow("हिन्दी", "Hindi", lang == Lang.HI) { langName = Lang.HI.name; vm.setLang(Lang.HI) }
            }
            1 -> {
                Text(stringResource(R.string.onb_country_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.onb_country_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                ChoiceRow(stringResource(R.string.country_pakistan), null, country == Country.PAKISTAN) {
                    countryName = Country.PAKISTAN.name; vm.setCountry(Country.PAKISTAN)
                }
                ChoiceRow(stringResource(R.string.country_india), null, country == Country.INDIA) {
                    countryName = Country.INDIA.name; vm.setCountry(Country.INDIA)
                }
                ChoiceRow(stringResource(R.string.country_other), null, country == Country.OTHER) {
                    countryName = Country.OTHER.name; vm.setCountry(Country.OTHER)
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Outlined.Info, null, tint = LocalSamjhoColors.current.warning,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.onb_eps_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                Text(stringResource(R.string.onb_goal_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.onb_goal_sub),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                // EPS is only offered where it is actually available.
                if (country != Country.INDIA) {
                    ChoiceRow(stringResource(R.string.goal_work), stringResource(R.string.goal_work_sub),
                        track == Track.EPS_EMPLOYMENT) {
                        trackName = Track.EPS_EMPLOYMENT.name; vm.setTrack(Track.EPS_EMPLOYMENT)
                    }
                }
                ChoiceRow(stringResource(R.string.goal_exam), stringResource(R.string.goal_exam_sub),
                    track == Track.TOPIK_ACADEMIC) {
                    trackName = Track.TOPIK_ACADEMIC.name; vm.setTrack(Track.TOPIK_ACADEMIC)
                }
                ChoiceRow(stringResource(R.string.goal_general), stringResource(R.string.goal_general_sub),
                    track == Track.GENERAL) {
                    trackName = Track.GENERAL.name; vm.setTrack(Track.GENERAL)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.back)) }
            else Spacer(Modifier.width(1.dp))
            Button(
                onClick = {
                    if (step < 2) step++
                    else { vm.completeOnboarding(); onDone() }
                },
                enabled = when (step) { 0 -> true; 1 -> country != null; else -> track != null },
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(stringResource(if (step < 2) R.string.continue_ else R.string.start_learning))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * [titleFont] lets a language name render in its own script's face. The language
 * picker is exactly where an Urdu reader who cannot read English must recognise
 * their language, so "اردو" has to look like Urdu, not like fallback Naskh.
 */
@Composable
private fun ChoiceRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    titleFont: androidx.compose.ui.text.font.FontFamily? = null,
    onClick: () -> Unit,
) {
    SamjhoCard(
        modifier = Modifier.padding(bottom = 10.dp),
        onClick = onClick,
        container = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.let {
                        if (titleFont != null) it.copy(fontFamily = titleFont, lineHeight = it.fontSize * 1.9f) else it
                    }
                )
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
