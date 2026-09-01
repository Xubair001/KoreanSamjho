package com.koreansamjho.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.koreansamjho.app.data.audio.TtsStatus
import com.koreansamjho.app.data.repository.LearningRepository
import com.koreansamjho.app.domain.model.Question
import com.koreansamjho.app.domain.model.QuestionKind
import com.koreansamjho.app.domain.model.Track
import com.koreansamjho.app.ui.components.*
import com.koreansamjho.app.ui.samjhoFactory
import com.koreansamjho.app.ui.localContainer
import com.koreansamjho.app.ui.navigation.Routes
import com.koreansamjho.app.ui.theme.SamjhoType
import com.koreansamjho.app.ui.theme.LocalFontScale
import com.koreansamjho.app.ui.theme.LocalSamjhoColors

@Composable
fun PracticeScreen(nav: NavController) {
    val container = localContainer()
    val dueCount by container.repository.dueCountFlow().collectAsStateWithLifecycle(0)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text(stringResource(R.string.nav_practice),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp))
        }
        item {
            PracticeTile(
                stringResource(R.string.practice_review),
                if (dueCount > 0) stringResource(R.string.home_review_count, dueCount)
                else stringResource(R.string.home_nothing_due),
                Icons.Outlined.Refresh, dueCount > 0
            ) { nav.navigate(Routes.quiz("due")) }
        }
        item {
            PracticeTile(stringResource(R.string.practice_quick), stringResource(R.string.tests_quick_sub),
                Icons.Outlined.Bolt, true) { nav.navigate(Routes.quiz("mixed")) }
        }
        item {
            PracticeTile(stringResource(R.string.practice_difficult), null,
                Icons.Outlined.TrendingDown, true) { nav.navigate(Routes.quiz("difficult")) }
        }
        item {
            PracticeTile(stringResource(R.string.practice_listening), null,
                Icons.Outlined.Headphones, true) { nav.navigate(Routes.quiz("listening")) }
        }
        item {
            PracticeTile(stringResource(R.string.practice_recent), null,
                Icons.Outlined.History, true) { nav.navigate(Routes.quiz("recent")) }
        }
        item {
            PracticeTile(stringResource(R.string.practice_interview), null,
                Icons.Outlined.RecordVoiceOver, true) { nav.navigate(Routes.INTERVIEW) }
        }
        item {
            PracticeTile(stringResource(R.string.practice_favourites), null,
                Icons.Outlined.StarBorder, true) { nav.navigate(Routes.FAVOURITES) }
        }
    }
}

@Composable
private fun PracticeTile(title: String, subtitle: String?, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
        onClick = if (enabled) onClick else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------------------------------------------------------------------------

data class QuizState(
    val loading: Boolean = true,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val selected: Int? = null,
    val revealed: Boolean = false,
    val correctCount: Int = 0,
    val finished: Boolean = false,
)

class QuizViewModel(private val repo: LearningRepository) : ViewModel() {
    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state.asStateFlow()

    fun load(source: String, arg: String, track: Track) = viewModelScope.launch {
        val qs = when (source) {
            "due" -> repo.questionsForDue(15)
            "difficult" -> repo.difficultItems(15).map { it.itemId }
                .let { ids -> repo.questionsForVocabIds(ids) }
                .ifEmpty { repo.buildTest(track, listOf("vocab_ko_to_meaning"), 10) }
            "recent" -> repo.recentItems(15).map { it.itemId }
                .let { ids -> repo.questionsForVocabIds(ids) }
                .ifEmpty { repo.buildTest(track, listOf("vocab_ko_to_meaning"), 10) }
            "listening" -> repo.buildTest(track, listOf("listening"), 10)
            "lesson" -> repo.questionsForLesson(arg, 12)
                .ifEmpty { repo.buildTest(track, listOf("vocab_ko_to_meaning"), 10) }
            else -> repo.buildTest(track,
                listOf("vocab_ko_to_meaning", "vocab_meaning_to_ko", "grammar"), 10)
        }
        _state.value = QuizState(loading = false, questions = qs)
    }

    fun select(i: Int) {
        val s = _state.value
        if (s.revealed) return
        val q = s.questions.getOrNull(s.index) ?: return
        val correct = i == q.correctIndex
        _state.value = s.copy(selected = i, revealed = true,
            correctCount = s.correctCount + if (correct) 1 else 0)
        // Feed the result back into the spaced-repetition scheduler.
        val vocabId = vocabIdFor(q)
        if (vocabId != null) viewModelScope.launch { repo.recordAnswer(vocabId, "vocab", correct) }
    }

    fun next() {
        val s = _state.value
        if (s.index + 1 >= s.questions.size) _state.value = s.copy(finished = true)
        else _state.value = s.copy(index = s.index + 1, selected = null, revealed = false)
    }

    /** Question ids embed the vocabulary id they were generated from. */
    private fun vocabIdFor(q: Question): String? = when (q.kind) {
        QuestionKind.VOCAB_KO_TO_MEANING -> "vocab." + q.id.removePrefix("q.vocab.km.")
        QuestionKind.VOCAB_MEANING_TO_KO -> "vocab." + q.id.removePrefix("q.vocab.mk.")
        QuestionKind.LISTENING -> "vocab." + q.id.removePrefix("q.listen.")
        else -> null
    }
}

@Composable
fun QuizScreen(nav: NavController, source: String, arg: String) {
    val container = localContainer()
    val settings by container.settings.settings.collectAsStateWithLifecycle(
        com.koreansamjho.app.data.prefs.Settings()
    )
    val vm: QuizViewModel = viewModel(factory = samjhoFactory { QuizViewModel(it.repository) })
    val state by vm.state.collectAsStateWithLifecycle()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()

    LaunchedEffect(source, arg, settings.track) { vm.load(source, arg, settings.track) }

    if (state.loading) { LoadingBox(); return }
    if (state.questions.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            SamjhoTopBar(stringResource(R.string.nav_practice), onBack = { nav.popBackStack() })
            EmptyState(stringResource(R.string.empty_nothing_here))
        }
        return
    }
    if (state.finished) {
        QuizSummary(state, onDone = { nav.popBackStack() })
        return
    }

    val q = state.questions[state.index]
    val extras = LocalSamjhoColors.current

    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(
            stringResource(R.string.question_of, state.index + 1, state.questions.size),
            onBack = { nav.popBackStack() }
        )
        LinearProgressIndicator(
            progress = { (state.index + 1).toFloat() / state.questions.size },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (ttsStatus != TtsStatus.READY) {
                TtsUnavailableBanner()
                Spacer(Modifier.height(16.dp))
            }
            QuestionPrompt(q, ttsStatus == TtsStatus.READY) { text ->
                container.tts.speak(text, q.id)
            }
            Spacer(Modifier.height(20.dp))
            q.options.forEachIndexed { i, opt ->
                val optionState = when {
                    !state.revealed && state.selected == i -> OptionState.SELECTED
                    state.revealed && i == q.correctIndex -> OptionState.CORRECT
                    state.revealed && i == state.selected -> OptionState.INCORRECT
                    else -> OptionState.IDLE
                }
                AnswerOption(opt.korean, opt.text, optionState, !state.revealed) { vm.select(i) }
                Spacer(Modifier.height(10.dp))
            }
            AnimatedVisibility(state.revealed) {
                SamjhoCard(container = if (state.selected == q.correctIndex)
                    extras.successContainer else MaterialTheme.colorScheme.errorContainer) {
                    Text(stringResource(R.string.explanation),
                        style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    TranslationText(q.explanation, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Button(
            onClick = { vm.next() },
            enabled = state.revealed,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp)
        ) {
            Text(stringResource(
                if (state.index + 1 >= state.questions.size) R.string.finish else R.string.next
            ))
        }
    }
}

@Composable
fun QuestionPrompt(q: Question, audioReady: Boolean, onPlay: (String) -> Unit) {
    val passage = if (q.passageId != null) loadContent(q.passageId) { it.passage(q.passageId) } else null
    if (passage != null) {
        SamjhoCard {
            KoreanText(passage.korean, style = SamjhoType.koreanSentence(LocalFontScale.current))
        }
        Spacer(Modifier.height(16.dp))
    }
    if (q.kind == QuestionKind.LISTENING && q.audioText != null) {
        // The Korean is deliberately hidden — this is a listening item.
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TranslationText(q.prompt, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))
            AudioButton(
                onPlay = { onPlay(q.audioText) },
                onPlaySlow = { onPlay(q.audioText) },
                enabled = audioReady
            )
            if (!audioReady) {
                Spacer(Modifier.height(12.dp))
                TtsUnavailableBanner(message = stringResource(R.string.tts_needed_listening))
            }
        }
    } else {
        if (!q.promptKorean.isNullOrBlank()) {
            KoreanText(q.promptKorean, style = SamjhoType.koreanHero(LocalFontScale.current))
            RomanizationText(q.promptRomanization)
            Spacer(Modifier.height(10.dp))
        }
        TranslationText(q.prompt, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun QuizSummary(state: QuizState, onDone: () -> Unit) {
    val pct = if (state.questions.isEmpty()) 0
    else state.correctCount * 100 / state.questions.size
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        ProgressRing(pct / 100f, Modifier.size(140.dp), stroke = 14f, label = "$pct%")
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.your_score), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        ForceLtr {
            Text(stringResource(R.string.correct_answers, state.correctCount, state.questions.size),
                style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.finish))
        }
    }
}
