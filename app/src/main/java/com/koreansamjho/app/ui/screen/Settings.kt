package com.koreansamjho.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.koreansamjho.app.BuildConfig
import com.koreansamjho.app.R
import com.koreansamjho.app.data.audio.TtsStatus
import com.koreansamjho.app.data.prefs.Settings
import com.koreansamjho.app.data.prefs.ThemeMode
import com.koreansamjho.app.domain.model.Lang
import com.koreansamjho.app.domain.model.Track
import com.koreansamjho.app.ui.AppViewModel
import com.koreansamjho.app.ui.components.*
import com.koreansamjho.app.ui.localContainer
import com.koreansamjho.app.ui.navigation.Routes
import com.koreansamjho.app.ui.theme.LocalLang
import com.koreansamjho.app.ui.theme.UrduFont

@Composable
fun SettingsScreen(nav: NavController, vm: AppViewModel, settings: Settings) {
    val container = localContainer()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.settings), onBack = { nav.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {

            SectionHeader(stringResource(R.string.settings_language))
            Column(Modifier.padding(horizontal = 16.dp)) {
                // Each language is labelled in its own script so it is findable by a
                // learner who cannot read the current interface language.
                RadioRow("English", settings.lang == Lang.EN) { vm.setLang(Lang.EN) }
                RadioRow("اردو", settings.lang == Lang.UR, labelFont = UrduFont) { vm.setLang(Lang.UR) }
                RadioRow("हिन्दी", settings.lang == Lang.HI) { vm.setLang(Lang.HI) }
            }

            SectionHeader(stringResource(R.string.settings_goal))
            Column(Modifier.padding(horizontal = 16.dp)) {
                RadioRow(stringResource(R.string.goal_work), settings.track == Track.EPS_EMPLOYMENT) {
                    vm.setTrack(Track.EPS_EMPLOYMENT)
                }
                RadioRow(stringResource(R.string.goal_exam), settings.track == Track.TOPIK_ACADEMIC) {
                    vm.setTrack(Track.TOPIK_ACADEMIC)
                }
                RadioRow(stringResource(R.string.goal_general), settings.track == Track.GENERAL) {
                    vm.setTrack(Track.GENERAL)
                }
            }

            SectionHeader(stringResource(R.string.settings_appearance))
            Column(Modifier.padding(horizontal = 16.dp)) {
                RadioRow(stringResource(R.string.theme_system), settings.themeMode == ThemeMode.SYSTEM) {
                    vm.setTheme(ThemeMode.SYSTEM)
                }
                RadioRow(stringResource(R.string.theme_light), settings.themeMode == ThemeMode.LIGHT) {
                    vm.setTheme(ThemeMode.LIGHT)
                }
                RadioRow(stringResource(R.string.theme_dark), settings.themeMode == ThemeMode.DARK) {
                    vm.setTheme(ThemeMode.DARK)
                }
            }

            SectionHeader(stringResource(R.string.settings_text_size))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Slider(
                    value = settings.fontScale,
                    onValueChange = { vm.setFontScale(it) },
                    valueRange = 0.85f..1.4f,
                    steps = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("가나다 ABC اردو हिन्दी", style = MaterialTheme.typography.bodyLarge)
            }

            SwitchRow(stringResource(R.string.settings_romanization),
                stringResource(R.string.settings_romanization_sub),
                settings.showRomanization) { vm.setShowRomanization(it) }
            SwitchRow(stringResource(R.string.settings_reduced_motion),
                stringResource(R.string.settings_reduced_motion_sub),
                settings.reducedMotion) { vm.setReducedMotion(it) }

            SectionHeader(stringResource(R.string.settings_daily_goal))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 30).forEach { m ->
                    FilterChip(
                        selected = settings.dailyGoalMinutes == m,
                        onClick = { vm.setDailyGoal(m) },
                        label = { Text(stringResource(R.string.minutes, m), maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SectionHeader(stringResource(R.string.settings_audio))
            when (ttsStatus) {
                TtsStatus.READY -> LinkRow(stringResource(R.string.settings_tts), "OK") {}
                TtsStatus.MISSING_KOREAN_VOICE -> Box(Modifier.padding(16.dp)) { TtsMissingCard() }
                TtsStatus.NO_ENGINE -> Box(Modifier.padding(16.dp)) {
                    SamjhoCard { Text(stringResource(R.string.tts_none_body),
                        style = MaterialTheme.typography.bodyMedium) }
                }
                TtsStatus.INITIALISING -> LinkRow(stringResource(R.string.settings_tts),
                    stringResource(R.string.loading)) {}
            }

            SectionHeader(stringResource(R.string.settings_about))
            LinkRow(stringResource(R.string.about_sources), null) { nav.navigate(Routes.ABOUT) }
            LinkRow(stringResource(R.string.exam_info), null) { nav.navigate(Routes.EXAM_INFO) }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.about_version, BuildConfig.CONTENT_VERSION.let { "1.0.0" },
                    BuildConfig.CONTENT_VERSION.toString()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    labelFont: androidx.compose.ui.text.font.FontFamily? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge.let {
                if (labelFont != null) it.copy(fontFamily = labelFont, lineHeight = it.fontSize * 1.9f) else it
            }
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LinkRow(title: String, value: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick)
            .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null,
            tint = MaterialTheme.colorScheme.outline)
    }
}



@Composable
fun AboutScreen(nav: NavController) {
    val sources = loadContent(Unit) { it.sources() }
    val licence = loadContent(Unit) { it.contentLicence() }
    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.settings_about), onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                SamjhoCard(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.about_privacy),
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.privacy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { SectionHeader(stringResource(R.string.about_sources)) }
            items(sources.orEmpty(), key = { it.id }) { s ->
                SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                    Text(s.title, style = MaterialTheme.typography.titleMedium)
                    Text(s.publisher, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(s.licence, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    if (s.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(s.note, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (s.url.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        ForceLtr {
                            Text(s.url, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.about_licences)) }
            item {
                SamjhoCard(Modifier.padding(horizontal = 16.dp)) {
                    Text(licence.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Fonts: Noto Sans KR and Noto Nastaliq Urdu, SIL Open Font License 1.1.\n" +
                            "Icons: Material Symbols, Apache License 2.0.\n" +
                            "Application code: Apache License 2.0.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Exam facts. Anything not confirmed from an official source is labelled as such,
 * and the EPS card carries an explicit correction to the widespread "fixed pass mark"
 * myth. See docs/01-research.md 1.2.
 */
@Composable
fun ExamInfoScreen(nav: NavController, settings: Settings) {
    val lang = LocalLang.current
    val exams = loadContent(Unit) { it.exams() }
    val relevant = exams.orEmpty().filter {
        it.track == settings.track.name || settings.track == Track.GENERAL
    }.ifEmpty { exams.orEmpty() }

    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.exam_info), onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(relevant, key = { it.id }) { e ->
                SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ForceLtr {
                        Text(e.code, style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(4.dp))
                    TranslationText(e.name, color = MaterialTheme.colorScheme.onSurface)

                    if (e.confidence == "corroborated") {
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.exam_confidence_corroborated),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error)
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.exam_who),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    TranslationText(e.who, color = MaterialTheme.colorScheme.onSurface)

                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.exam_structure),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    e.sections.forEach { s ->
                        ForceLtr {
                            Text("${s.name} — ${s.questions} Q · ${s.points} pts" +
                                (s.minutes?.let { " · $it min" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    ForceLtr {
                        Text("Total: ${e.totalQuestions} questions · ${e.totalPoints} points",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(e.delivery, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.exam_scoring),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    TranslationText(e.scoring, color = MaterialTheme.colorScheme.onSurface)

                    e.caution?.let { c ->
                        Spacer(Modifier.height(14.dp))
                        SamjhoCard(container = com.koreansamjho.app.ui.theme.LocalSamjhoColors.current.warningContainer) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Outlined.WarningAmber, null,
                                    tint = com.koreansamjho.app.ui.theme.LocalSamjhoColors.current.warning)
                                Spacer(Modifier.width(10.dp))
                                TranslationText(c, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    if (e.officialUrl.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.exam_official),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        ForceLtr {
                            Text(e.officialUrl, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
