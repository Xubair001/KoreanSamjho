package com.koreansamjho.app.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.koreansamjho.app.R
import com.koreansamjho.app.data.audio.TtsStatus
import com.koreansamjho.app.data.prefs.Settings
import com.koreansamjho.app.data.repository.LessonContent
import com.koreansamjho.app.domain.model.*
import com.koreansamjho.app.ui.components.*
import com.koreansamjho.app.ui.localContainer
import com.koreansamjho.app.ui.navigation.Routes
import com.koreansamjho.app.ui.theme.SamjhoType
import com.koreansamjho.app.ui.theme.LocalFontScale
import com.koreansamjho.app.ui.theme.LocalSamjhoColors
import com.koreansamjho.app.ui.theme.LocalLang

@Composable
fun LearnScreen(nav: NavController, settings: Settings) {
    val lang = LocalLang.current
    val courses = loadContent(settings.track) { it.courses(settings.track) }
    val done = loadContent(settings.track) { it.lessonProgressFlowSnapshot() } ?: emptySet()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text(stringResource(R.string.nav_learn),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp))
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BrowseChip(stringResource(R.string.learn_vocabulary), Modifier.weight(1f)) {
                    nav.navigate(Routes.VOCAB_LIST)
                }
                BrowseChip(stringResource(R.string.learn_grammar), Modifier.weight(1f)) {
                    nav.navigate(Routes.GRAMMAR_LIST)
                }
                BrowseChip(stringResource(R.string.learn_sentences), Modifier.weight(1f)) {
                    nav.navigate(Routes.SENTENCE_LIST)
                }
            }
        }
        item { SectionHeader(stringResource(R.string.learn_courses)) }
        items(courses.orEmpty(), key = { it.id }) { course ->
            val lessons = loadContent(course.id) { it.lessons(course.id) } ?: emptyList()
            val completed = lessons.count { it.id in done }
            SamjhoCard(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                onClick = { nav.navigate(Routes.course(course.id)) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(course.title[lang], style = MaterialTheme.typography.titleMedium)
                        Text(course.subtitle[lang], style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.lessons_count, lessons.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(12.dp))
                    ProgressRing(
                        progress = if (lessons.isEmpty()) 0f else completed.toFloat() / lessons.size,
                        modifier = Modifier.size(56.dp),
                        label = "$completed/${lessons.size}"
                    )
                }
            }
        }
    }
}

@Composable
private fun BrowseChip(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    SamjhoCard(modifier, onClick = onClick) {
        Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 2)
    }
}

@Composable
fun CourseScreen(nav: NavController, courseId: String) {
    val lang = LocalLang.current
    val lessons = loadContent(courseId) { it.lessons(courseId) }
    val done = loadContent(courseId) { it.lessonProgressFlowSnapshot() } ?: emptySet()
    val course = loadContent(courseId) { repo -> repo.courses(Track.GENERAL).firstOrNull { it.id == courseId } }

    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(course?.title?.get(lang) ?: stringResource(R.string.learn_courses), onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(lessons.orEmpty(), key = { it.id }) { lesson ->
                val complete = lesson.id in done
                SamjhoCard(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    onClick = { nav.navigate(Routes.lesson(lesson.id)) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ForceLtr {
                            Text("${lesson.order}", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.widthIn(min = 28.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(lesson.title[lang], style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f))
                        if (complete) {
                            Icon(Icons.Filled.CheckCircle, stringResource(R.string.completed),
                                tint = LocalSamjhoColors.current.success)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LessonScreen(nav: NavController, lessonId: String) {
    val lang = LocalLang.current
    val container = localContainer()
    val scope = rememberCoroutineScope()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()
    val lesson = loadContent(lessonId) { it.lesson(lessonId) }
    val content = loadContent(lesson?.id) { repo -> lesson?.let { repo.lessonContent(it) } }
    var completed by remember(lessonId) { mutableStateOf(false) }
    LaunchedEffect(lessonId) { completed = container.repository.isLessonComplete(lessonId) }

    if (lesson == null || content == null) { LoadingBox(); return }
    val audioReady = ttsStatus == TtsStatus.READY

    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(lesson.title[lang], onBack = { nav.popBackStack() })
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (!audioReady) item {
                TtsUnavailableBanner(Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            }
            when (content) {
                is LessonContent.Letters -> items(content.items, key = { it.id }) { l ->
                    LetterCard(l, audioReady) { container.tts.speak(it, l.id) }
                }
                is LessonContent.Syllables -> {
                    item { SyllableGrid(content.consonants, content.vowels, audioReady) { container.tts.speak(it, it) } }
                }
                is LessonContent.Batchim -> {
                    item { BatchimIntro() }
                    items(content.examples, key = { it.id }) { v ->
                        WordRow(v, audioReady, onPlay = { container.tts.speak(v.korean, v.id) },
                            onClick = { nav.navigate(Routes.word(v.id)) })
                    }
                }
                is LessonContent.Words -> items(content.items, key = { it.id }) { v ->
                    WordRow(v, audioReady, onPlay = { container.tts.speak(v.korean, v.id) },
                        onClick = { nav.navigate(Routes.word(v.id)) })
                }
                is LessonContent.Grammars -> items(content.items, key = { it.id }) { g ->
                    SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        onClick = { nav.navigate(Routes.grammar(g.id)) }) {
                        KoreanText(g.pattern, style = SamjhoType.koreanWord(LocalFontScale.current))
                        Text(g.titleEn, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        TranslationText(g.explanation)
                    }
                }
                is LessonContent.Sentences -> items(content.items, key = { it.id }) { s ->
                    SentenceCard(s, audioReady,
                        onPlay = { container.tts.speak(s.korean, s.id) },
                        onPlaySlow = { container.tts.speak(s.korean, s.id, slow = true) })
                }
                is LessonContent.Interview -> items(content.items, key = { it.id }) { q ->
                    InterviewCard(q, audioReady) { container.tts.speak(it, q.id) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { nav.navigate(Routes.quiz("lesson", lessonId)) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.lesson_practice), maxLines = 1) }
            Button(
                onClick = {
                    scope.launch { container.repository.completeLesson(lesson, 100); completed = true }
                },
                enabled = !completed,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Text(stringResource(if (completed) R.string.completed else R.string.mark_complete), maxLines = 1)
            }
        }
    }
}

@Composable
private fun LetterCard(l: Letter, audioReady: Boolean, onPlay: (String) -> Unit) {
    SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    KoreanText(l.char, style = SamjhoType.koreanHero(LocalFontScale.current))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        ForceLtr {
                            Text(l.romanization, style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        KoreanText(l.nameKo, style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = com.koreansamjho.app.ui.theme.KoreanFont))
                    }
                }
                Spacer(Modifier.height(8.dp))
                TranslationText(l.sound, color = MaterialTheme.colorScheme.onSurface)
                if (l.finalSound != null) {
                    Spacer(Modifier.height(4.dp))
                    ForceLtr {
                        Text("initial: ${l.initialSound}   final: ${l.finalSound}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            AudioButton(onPlay = { onPlay(l.char) }, enabled = audioReady)
        }
    }
}

/** Shows how a consonant and a vowel combine into a syllable — the core Hangul insight. */
@Composable
private fun SyllableGrid(consonants: List<Letter>, vowels: List<Letter>, audioReady: Boolean, onPlay: (String) -> Unit) {
    var cIndex by remember { mutableIntStateOf(0) }
    val consonant = consonants.getOrNull(cIndex) ?: return
    Column(Modifier.padding(16.dp)) {
        Text("Choose a consonant", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        val chipScroll = rememberScrollState()
        Row(Modifier.horizontalScroll(chipScroll), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            consonants.forEachIndexed { i, c ->
                FilterChip(selected = i == cIndex, onClick = { cIndex = i },
                    label = { ForceLtr { Text(c.char) } })
            }
        }
        Spacer(Modifier.height(16.dp))
        vowels.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { v ->
                    val syllable = composeSyllable(consonant.char, v.char)
                    SamjhoCard(Modifier.weight(1f).padding(vertical = 4.dp),
                        onClick = { if (audioReady) onPlay(syllable) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                ForceLtr {
                                    Text("${consonant.char} + ${v.char}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                KoreanText(syllable, style = SamjhoType.koreanWord(LocalFontScale.current))
                                RomanizationText(consonant.romanization + v.romanization)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Builds a Hangul syllable from a leading consonant and a vowel using Unicode composition. */
private fun composeSyllable(consonant: String, vowel: String): String {
    val leads = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ"
    val vowels = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ"
    val l = leads.indexOf(consonant.first())
    val v = vowels.indexOf(vowel.first())
    if (l < 0 || v < 0) return consonant + vowel
    return ((0xAC00 + (l * 21 + v) * 28).toChar()).toString()
}

@Composable
private fun BatchimIntro() {
    SamjhoCard(Modifier.padding(16.dp)) {
        Text("Batchim (받침)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "A consonant written underneath a syllable is called batchim. Korean has many " +
                "batchim letters but only seven final sounds: ㄱ, ㄴ, ㄷ, ㄹ, ㅁ, ㅂ and ㅇ. " +
                "Several letters share one sound — ㅅ, ㅆ, ㅈ, ㅊ, ㅌ and ㅎ are all pronounced " +
                "as ㄷ at the end of a syllable.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WordRow(v: Vocab, audioReady: Boolean, onPlay: () -> Unit, onClick: () -> Unit) {
    SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                KoreanText(v.korean, style = SamjhoType.koreanWord(LocalFontScale.current))
                RomanizationText(v.romanization)
                Spacer(Modifier.height(4.dp))
                TranslationText(v.meaning, color = MaterialTheme.colorScheme.onSurface)
            }
            AudioButton(onPlay = onPlay, enabled = audioReady)
        }
    }
}

@Composable
fun SentenceCard(s: Sentence, audioReady: Boolean, onPlay: () -> Unit, onPlaySlow: () -> Unit) {
    SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                KoreanText(s.korean, style = SamjhoType.koreanSentence(LocalFontScale.current))
                RomanizationText(s.romanization)
                Spacer(Modifier.height(6.dp))
                TranslationText(s.translation, color = MaterialTheme.colorScheme.onSurface)
            }
            AudioButton(onPlay = onPlay, onPlaySlow = onPlaySlow, enabled = audioReady)
        }
    }
}

@Composable
fun InterviewCard(q: InterviewItem, audioReady: Boolean, onPlay: (String) -> Unit) {
    var showAnswer by remember(q.id) { mutableStateOf(false) }
    SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                KoreanText(q.questionKorean, style = SamjhoType.koreanSentence(LocalFontScale.current))
                RomanizationText(q.questionRomanization)
                Spacer(Modifier.height(4.dp))
                TranslationText(q.question, color = MaterialTheme.colorScheme.onSurface)
            }
            AudioButton(onPlay = { onPlay(q.questionKorean) }, enabled = audioReady)
        }
        Spacer(Modifier.height(8.dp))
        if (!showAnswer) {
            TextButton(onClick = { showAnswer = true }) { Text(stringResource(R.string.show_answer)) }
        } else {
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    KoreanText(q.answerKorean, style = SamjhoType.koreanSentence(LocalFontScale.current))
                    RomanizationText(q.answerRomanization)
                    Spacer(Modifier.height(6.dp))
                    TranslationText(q.answer, color = MaterialTheme.colorScheme.onSurface)
                    if (q.tipEn.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(q.tipEn, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                AudioButton(onPlay = { onPlay(q.answerKorean) },
                    onPlaySlow = { onPlay(q.answerKorean) }, enabled = audioReady)
            }
        }
    }
}
