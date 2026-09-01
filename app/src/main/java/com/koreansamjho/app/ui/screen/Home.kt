package com.koreansamjho.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.koreansamjho.app.R
import com.koreansamjho.app.data.prefs.Settings
import com.koreansamjho.app.data.audio.TtsStatus
import com.koreansamjho.app.data.repository.LearningRepository
import com.koreansamjho.app.domain.model.*
import com.koreansamjho.app.ui.components.*
import com.koreansamjho.app.ui.samjhoFactory
import com.koreansamjho.app.ui.navigation.Routes
import com.koreansamjho.app.ui.theme.SamjhoType
import com.koreansamjho.app.ui.theme.LocalFontScale

data class HomeState(
    val loading: Boolean = true,
    val continueLesson: Lesson? = null,
    val continueCourse: Course? = null,
    val dueCount: Int = 0,
    val dailyWord: Vocab? = null,
    val dailySentence: Sentence? = null,
    val streak: Int = 0,
    val todayMinutes: Int = 0,
    val lessonsDone: Int = 0,
    val totalLessons: Int = 0,
)

class HomeViewModel(private val repo: LearningRepository) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun load(track: Track) = viewModelScope.launch {
        val courses = repo.courses(track)
        val done = repo.lessonProgressFlowSnapshot()
        val all = repo.allLessons(track)
        val next = all.firstOrNull { it.id !in done }
        _state.value = HomeState(
            loading = false,
            continueLesson = next,
            continueCourse = courses.firstOrNull { it.id == next?.courseId },
            dueCount = repo.dueItems(100).size,
            dailyWord = repo.dailyWord(),
            dailySentence = repo.dailySentence(),
            streak = repo.currentStreak(),
            todayMinutes = ((repo.todayActivity()?.studySeconds ?: 0) / 60),
            lessonsDone = done.size,
            totalLessons = all.size,
        )
    }
}

@Composable
fun HomeScreen(nav: NavController, settings: Settings) {
    val vm: HomeViewModel = viewModel(factory = samjhoFactory { HomeViewModel(it.repository) })
    val state by vm.state.collectAsStateWithLifecycle()
    val container = com.koreansamjho.app.ui.localContainer()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()

    LaunchedEffect(settings.track) { vm.load(settings.track) }

    if (state.loading) { LoadingBox(); return }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_greeting),
                        style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.home_goal_today, settings.dailyGoalMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { nav.navigate(Routes.SEARCH) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Search, stringResource(R.string.search))
                }
                IconButton(onClick = { nav.navigate(Routes.SETTINGS) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                }
            }
        }

        if (state.streak > 0) item {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { StreakBadge(state.streak) }
        }

        // Continue learning
        item {
            val lesson = state.continueLesson
            SamjhoCard(
                Modifier.padding(16.dp),
                onClick = { lesson?.let { nav.navigate(Routes.lesson(it.id)) } },
                container = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    stringResource(if (state.lessonsDone == 0) R.string.home_start_here else R.string.home_continue),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(6.dp))
                if (lesson != null) {
                    Text(lesson.title[com.koreansamjho.app.ui.theme.LocalLang.current],
                        style = MaterialTheme.typography.titleLarge)
                    state.continueCourse?.let {
                        Text(it.title[com.koreansamjho.app.ui.theme.LocalLang.current],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (state.totalLessons == 0) 0f
                            else state.lessonsDone.toFloat() / state.totalLessons
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    ForceLtr {
                        Text("${state.lessonsDone} / ${state.totalLessons}",
                            style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Text(stringResource(R.string.completed), style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        // Review due
        item {
            SamjhoCard(
                Modifier.padding(horizontal = 16.dp),
                onClick = { if (state.dueCount > 0) nav.navigate(Routes.quiz("due")) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.home_review_today),
                            style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (state.dueCount > 0)
                                stringResource(R.string.home_review_count, state.dueCount)
                            else stringResource(R.string.home_nothing_due),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (state.dueCount > 0) Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                }
            }
        }

        // Audio availability is surfaced once, here, rather than as dead buttons later.
        // Any not-ready state qualifies: a missing engine is just as worth explaining
        // as a missing Korean voice, and the banner adapts its wording to which it is.
        if (ttsStatus != TtsStatus.READY && ttsStatus != TtsStatus.INITIALISING) {
            item { Box(Modifier.padding(16.dp)) { TtsUnavailableBanner() } }
        }

        item { SectionHeader(stringResource(R.string.home_daily_word)) }
        item {
            state.dailyWord?.let { w ->
                SamjhoCard(Modifier.padding(horizontal = 16.dp), onClick = { nav.navigate(Routes.word(w.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            KoreanText(w.korean, style = SamjhoType.koreanHero(LocalFontScale.current))
                            RomanizationText(w.romanization)
                            Spacer(Modifier.height(6.dp))
                            TranslationText(w.meaning, color = MaterialTheme.colorScheme.onSurface)
                        }
                        AudioButton(
                            onPlay = { container.tts.speak(w.korean, w.id) },
                            onPlaySlow = { container.tts.speak(w.korean, w.id, slow = true) },
                            enabled = ttsStatus == TtsStatus.READY
                        )
                    }
                }
            }
        }

        item { SectionHeader(stringResource(R.string.home_daily_sentence)) }
        item {
            state.dailySentence?.let { s ->
                SamjhoCard(Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            KoreanText(s.korean, style = SamjhoType.koreanSentence(LocalFontScale.current))
                            RomanizationText(s.romanization)
                            Spacer(Modifier.height(6.dp))
                            TranslationText(s.translation, color = MaterialTheme.colorScheme.onSurface)
                        }
                        AudioButton(
                            onPlay = { container.tts.speak(s.korean, s.id) },
                            onPlaySlow = { container.tts.speak(s.korean, s.id, slow = true) },
                            enabled = ttsStatus == TtsStatus.READY
                        )
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { nav.navigate(Routes.quiz("mixed")) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.home_quick_practice), maxLines = 1) }
                OutlinedButton(
                    onClick = { nav.navigate(Routes.TESTS) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.home_mock_test), maxLines = 1) }
            }
        }
    }
}
