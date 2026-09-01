package com.koreansamjho.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.koreansamjho.app.R
import com.koreansamjho.app.data.audio.TtsStatus
import com.koreansamjho.app.data.prefs.Settings
import com.koreansamjho.app.data.repository.LearningRepository
import com.koreansamjho.app.domain.model.Question
import com.koreansamjho.app.domain.model.Track
import com.koreansamjho.app.ui.components.*
import com.koreansamjho.app.ui.samjhoFactory
import com.koreansamjho.app.ui.localContainer
import com.koreansamjho.app.ui.navigation.Routes
import com.koreansamjho.app.ui.theme.LocalSamjhoColors
import com.koreansamjho.app.ui.theme.LocalLang

@Composable
fun TestsScreen(nav: NavController, settings: Settings) {
    val container = localContainer()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()
    val audioReady = ttsStatus == TtsStatus.READY
    var askInstall by remember { mutableStateOf(false) }
    if (askInstall) {
        TtsInstallDialog(
            onDismiss = { askInstall = false },
            extraMessage = stringResource(R.string.tts_needed_listening)
        )
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text(stringResource(R.string.nav_tests),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp))
        }
        // Stated up front, not buried: these are practice questions, not an official exam.
        item { Box(Modifier.padding(horizontal = 16.dp)) { PracticeNotice(expanded = true) } }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            TestTile(stringResource(R.string.tests_quick), stringResource(R.string.tests_quick_sub),
                Icons.Outlined.Bolt) { nav.navigate(Routes.test("quick", 0)) }
        }
        item {
            TestTile(stringResource(R.string.tests_vocab), null, Icons.Outlined.Translate) {
                nav.navigate(Routes.test("vocab", 10))
            }
        }
        item {
            TestTile(stringResource(R.string.tests_grammar), null, Icons.Outlined.Rule) {
                nav.navigate(Routes.test("grammar", 10))
            }
        }
        // A listening paper is useless without audio, so explain before entering it.
        item {
            TestTile(
                stringResource(R.string.tests_listening),
                if (audioReady) null else stringResource(
                    if (ttsStatus == TtsStatus.NO_ENGINE) R.string.tts_none_title
                    else R.string.tts_banner_short
                ),
                Icons.Outlined.Headphones
            ) {
                if (audioReady) nav.navigate(Routes.test("listening", 10)) else askInstall = true
            }
        }
        item {
            TestTile(stringResource(R.string.tests_reading), null, Icons.Outlined.MenuBook) {
                nav.navigate(Routes.test("reading", 15))
            }
        }
        item {
            TestTile(stringResource(R.string.tests_mock),
                if (settings.track == Track.EPS_EMPLOYMENT) "EPS-TOPIK style" else "TOPIK style",
                Icons.Outlined.Assignment) { nav.navigate(Routes.test("mock", 40)) }
        }
        item { HorizontalDivider(Modifier.padding(16.dp)) }
        item {
            TestTile(stringResource(R.string.exam_info), null, Icons.Outlined.Info) {
                nav.navigate(Routes.EXAM_INFO)
            }
        }
        item {
            TestTile(stringResource(R.string.tests_history), null, Icons.Outlined.History) {
                nav.navigate(Routes.TEST_HISTORY)
            }
        }
    }
}

@Composable
private fun TestTile(title: String, subtitle: String?, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
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

data class TestState(
    val loading: Boolean = true,
    val questions: List<Question> = emptyList(),
    val index: Int = 0,
    val answers: Map<String, Int> = emptyMap(),
    val marked: Set<String> = emptySet(),
    val startedAt: Long = 0L,
    val submittedAttemptId: Long? = null,
    val showNavigator: Boolean = false,
)

class TestViewModel(private val repo: LearningRepository) : ViewModel() {
    private val _state = MutableStateFlow(TestState())
    val state: StateFlow<TestState> = _state.asStateFlow()
    private var kind: String = "quick"
    private var track: Track = Track.GENERAL

    fun load(kind: String, track: Track) = viewModelScope.launch {
        this@TestViewModel.kind = kind
        this@TestViewModel.track = track
        val qs = when (kind) {
            "vocab" -> repo.buildTest(track, listOf("vocab_ko_to_meaning", "vocab_meaning_to_ko"), 20)
            "grammar" -> repo.buildTest(track, listOf("grammar"), 15)
            "listening" -> repo.buildTest(track, listOf("listening"), 15)
            "reading" -> repo.buildTest(track, listOf("reading"), 8)
            "mock" -> buildMock(track)
            else -> repo.buildTest(track,
                listOf("vocab_ko_to_meaning", "vocab_meaning_to_ko", "grammar"), 10)
        }
        _state.value = TestState(loading = false, questions = qs, startedAt = System.currentTimeMillis())
    }

    /**
     * A mock paper mirrors the real exam's *shape* — a listening half and a reading half —
     * without reproducing any real exam material. Section sizes follow the learner's track.
     */
    private suspend fun buildMock(track: Track): List<Question> {
        val listening = repo.buildTest(track, listOf("listening"), 20)
        val reading = repo.buildTest(track,
            listOf("vocab_ko_to_meaning", "vocab_meaning_to_ko", "grammar", "reading"), 20)
        return listening + reading
    }

    fun answer(questionId: String, index: Int) {
        _state.value = _state.value.let { it.copy(answers = it.answers + (questionId to index)) }
    }
    fun toggleMark(questionId: String) {
        _state.value = _state.value.let {
            it.copy(marked = if (questionId in it.marked) it.marked - questionId else it.marked + questionId)
        }
    }
    fun goTo(i: Int) { _state.value = _state.value.copy(index = i, showNavigator = false) }
    fun next() { _state.value = _state.value.let { it.copy(index = (it.index + 1).coerceAtMost(it.questions.lastIndex)) } }
    fun previous() { _state.value = _state.value.let { it.copy(index = (it.index - 1).coerceAtLeast(0)) } }
    fun showNavigator(show: Boolean) { _state.value = _state.value.copy(showNavigator = show) }

    fun submit() = viewModelScope.launch {
        val s = _state.value
        val id = repo.saveAttempt(kind, track, s.startedAt, System.currentTimeMillis(), s.questions, s.answers)
        // Feed every answer back into spaced repetition, not just the wrong ones.
        s.questions.forEach { q ->
            val vid = when {
                q.id.startsWith("q.vocab.km.") -> "vocab." + q.id.removePrefix("q.vocab.km.")
                q.id.startsWith("q.vocab.mk.") -> "vocab." + q.id.removePrefix("q.vocab.mk.")
                q.id.startsWith("q.listen.") -> "vocab." + q.id.removePrefix("q.listen.")
                else -> null
            }
            if (vid != null) repo.recordAnswer(vid, "vocab", s.answers[q.id] == q.correctIndex)
        }
        _state.value = s.copy(submittedAttemptId = id)
    }
}

@Composable
fun TestRunnerScreen(nav: NavController, settings: Settings, kind: String, minutes: Int) {
    val container = localContainer()
    val vm: TestViewModel = viewModel(factory = samjhoFactory { TestViewModel(it.repository) })
    val state by vm.state.collectAsStateWithLifecycle()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()
    var remaining by remember { mutableLongStateOf(minutes * 60L) }
    var confirmSubmit by remember { mutableStateOf(false) }

    LaunchedEffect(kind, settings.track) { vm.load(kind, settings.track) }

    // Timer. Only runs for timed papers; a quick quiz has no clock by design.
    LaunchedEffect(state.loading, minutes) {
        if (minutes > 0 && !state.loading) {
            while (remaining > 0 && state.submittedAttemptId == null) { delay(1000); remaining-- }
            if (remaining <= 0L) vm.submit()
        }
    }
    LaunchedEffect(state.submittedAttemptId) {
        state.submittedAttemptId?.let {
            nav.navigate(Routes.result(it)) { popUpTo(Routes.TESTS) }
        }
    }

    if (state.loading) { LoadingBox(); return }
    if (state.questions.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            SamjhoTopBar(stringResource(R.string.nav_tests), onBack = { nav.popBackStack() })
            EmptyState(stringResource(R.string.empty_nothing_here))
        }
        return
    }

    val q = state.questions[state.index]
    val answered = state.answers.size
    val unanswered = state.questions.size - answered

    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(
            stringResource(R.string.question_of, state.index + 1, state.questions.size),
            onBack = { nav.popBackStack() },
            actions = {
                if (minutes > 0) {
                    ForceLtr {
                        Text(
                            "%d:%02d".format(remaining / 60, remaining % 60),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (remaining < 60) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                IconButton(onClick = { vm.showNavigator(true) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Outlined.GridView, "Question navigator")
                }
            }
        )
        LinearProgressIndicator(
            progress = { (state.index + 1).toFloat() / state.questions.size },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (ttsStatus != TtsStatus.READY && (kind == "listening" || kind == "mock")) {
                TtsUnavailableBanner(message = stringResource(R.string.tts_needed_listening))
                Spacer(Modifier.height(16.dp))
            }
            QuestionPrompt(q, ttsStatus == TtsStatus.READY) { container.tts.speak(it, q.id) }
            Spacer(Modifier.height(20.dp))
            q.options.forEachIndexed { i, opt ->
                // During the test no correctness is revealed — only after submission.
                val optState = if (state.answers[q.id] == i) OptionState.SELECTED else OptionState.IDLE
                AnswerOption(opt.korean, opt.text, optState, true) { vm.answer(q.id, i) }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.toggleMark(q.id) }) {
                Icon(if (q.id in state.marked) Icons.Filled.Flag else Icons.Outlined.Flag, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (q.id in state.marked) R.string.marked_review else R.string.mark_review))
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { vm.previous() }, enabled = state.index > 0,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.previous), maxLines = 1) }
            if (state.index < state.questions.lastIndex) {
                Button(onClick = { vm.next() }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.next), maxLines = 1)
                }
            } else {
                Button(onClick = { confirmSubmit = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.submit_test), maxLines = 1)
                }
            }
        }
    }

    if (state.showNavigator) {
        QuestionNavigator(state, onDismiss = { vm.showNavigator(false) }, onGo = { vm.goTo(it) },
            onSubmit = { vm.showNavigator(false); confirmSubmit = true })
    }
    if (confirmSubmit) {
        AlertDialog(
            onDismissRequest = { confirmSubmit = false },
            title = { Text(stringResource(R.string.submit_confirm)) },
            text = {
                if (unanswered > 0) Text(stringResource(R.string.submit_unanswered, unanswered))
            },
            confirmButton = {
                TextButton(onClick = { confirmSubmit = false; vm.submit() }) {
                    Text(stringResource(R.string.submit_test))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmSubmit = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionNavigator(state: TestState, onDismiss: () -> Unit, onGo: (Int) -> Unit, onSubmit: () -> Unit) {
    val extras = LocalSamjhoColors.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.nav_tests), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(52.dp),
                modifier = Modifier.heightIn(max = 320.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.questions.size) { i ->
                    val qq = state.questions[i]
                    val bg = when {
                        qq.id in state.marked -> extras.warningContainer
                        state.answers.containsKey(qq.id) -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                            .background(bg).clickable { onGo(i) },
                        contentAlignment = Alignment.Center
                    ) { ForceLtr { Text("${i + 1}", style = MaterialTheme.typography.labelLarge) } }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.submit_test))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun TestResultScreen(nav: NavController, attemptId: Long) {
    val lang = LocalLang.current
    val attempt = loadContent(attemptId) { it.attempt(attemptId) }
    val answers = loadContent(attemptId) { it.attemptAnswers(attemptId) } ?: emptyList()
    val weak = loadContent(attemptId) { it.categoryAccuracy(2) } ?: emptyList()
    // Load the questions behind this attempt so the review can show the real answers.
    val questions = loadContent(answers.size) { repo ->
        repo.questionsByIds(answers.map { it.questionId })
    } ?: emptyList()
    val questionsById = remember(questions) { questions.associateBy { it.id } }
    val extras = LocalSamjhoColors.current

    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.your_score), onBack = {
            nav.navigate(Routes.TESTS) { popUpTo(Routes.TESTS) { inclusive = true } }
        })
        if (attempt == null) { LoadingBox(); return }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    ProgressRing(attempt.scorePercent / 100f, Modifier.size(140.dp),
                        stroke = 14f, label = "${attempt.scorePercent}%")
                    Spacer(Modifier.height(16.dp))
                    ForceLtr {
                        Text(stringResource(R.string.correct_answers,
                            attempt.correctCount, attempt.totalQuestions),
                            style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    ForceLtr {
                        Text("%d:%02d".format(attempt.durationMs / 60000, (attempt.durationMs / 1000) % 60),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Box(Modifier.padding(horizontal = 16.dp)) { PracticeNotice() } }
            if (weak.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.weak_areas)) }
                items(weak.sortedBy { it.correct * 100 / maxOf(it.total, 1) }.take(5)) { c ->
                    val pct = c.correct * 100 / maxOf(c.total, 1)
                    SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.category.replace('_', ' '),
                                style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            ForceLtr {
                                Text("$pct%", style = MaterialTheme.typography.labelLarge,
                                    color = if (pct < 60) MaterialTheme.colorScheme.error else extras.success)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.review_answers)) }
            items(answers, key = { it.id }) { a ->
                val q = questionsById[a.questionId]
                AnswerReviewRow(q, a.correct, a.selectedIndex)
            }
        }
    }
}

/** Shows the question, what the learner chose, the right answer, and why. */
@Composable
private fun AnswerReviewRow(q: Question?, correct: Boolean, selectedIndex: Int) {
    val extras = LocalSamjhoColors.current
    var expanded by remember(q?.id) { mutableStateOf(false) }
    SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (correct) Icons.Filled.CheckCircle else Icons.Filled.Cancel, null,
                tint = if (correct) extras.success else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (!q?.promptKorean.isNullOrBlank()) {
                    KoreanText(q!!.promptKorean!!,
                        style = com.koreansamjho.app.ui.theme.SamjhoType.koreanOption(
                            com.koreansamjho.app.ui.theme.LocalFontScale.current))
                } else if (q != null) {
                    TranslationText(q.prompt, color = MaterialTheme.colorScheme.onSurface)
                }
                Text(
                    stringResource(if (correct) R.string.correct else R.string.incorrect),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (correct) extras.success else MaterialTheme.colorScheme.error
                )
            }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
        }
        if (expanded && q != null) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            if (selectedIndex < 0) {
                Text(stringResource(R.string.not_answered),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (!correct) {
                Text(stringResource(R.string.your_answer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error)
                q.options.getOrNull(selectedIndex)?.let { OptionLine(it) }
                Spacer(Modifier.height(8.dp))
            }
            Text(stringResource(R.string.right_answer),
                style = MaterialTheme.typography.labelMedium, color = extras.success)
            q.options.getOrNull(q.correctIndex)?.let { OptionLine(it) }
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.explanation),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary)
            TranslationText(q.explanation, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun OptionLine(opt: com.koreansamjho.app.domain.model.QuestionOption) {
    if (!opt.korean.isNullOrBlank()) {
        KoreanText(opt.korean, style = com.koreansamjho.app.ui.theme.SamjhoType.koreanOption(
            com.koreansamjho.app.ui.theme.LocalFontScale.current))
    }
    if (!opt.text.isBlank()) TranslationText(opt.text, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
fun TestHistoryScreen(nav: NavController) {
    val container = localContainer()
    val attempts by container.repository.attemptsFlow(50).collectAsStateWithLifecycle(emptyList())
    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.tests_history), onBack = { nav.popBackStack() })
        if (attempts.isEmpty()) {
            EmptyState(stringResource(R.string.no_attempts), Icons.Outlined.Assignment)
            return
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(attempts, key = { it.id }) { a ->
                SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    onClick = { nav.navigate(Routes.result(a.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(a.kind.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium)
                            ForceLtr {
                                Text(stringResource(R.string.correct_answers, a.correctCount, a.totalQuestions),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        ForceLtr {
                            Text("${a.scorePercent}%", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
