package com.koreansamjho.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.koreansamjho.app.R
import com.koreansamjho.app.data.audio.TtsStatus
import com.koreansamjho.app.domain.model.Localized
import com.koreansamjho.app.ui.components.*
import com.koreansamjho.app.ui.localContainer
import com.koreansamjho.app.ui.navigation.Routes
import com.koreansamjho.app.ui.theme.*

@Composable
fun VocabCategoriesScreen(nav: NavController) {
    val cats = loadContent(Unit) { it.vocabCategories() }
    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.learn_vocabulary), onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(cats.orEmpty(), key = { it.first }) { (cat, n) ->
                SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    onClick = { nav.navigate(Routes.vocabCategory(cat)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(cat.replaceFirstChar { it.uppercase() }.replace('_', ' '),
                            style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.words_count, n),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun VocabListScreen(nav: NavController, category: String) {
    val container = localContainer()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()
    val words = loadContent(category) { it.vocabByCategory(category) }
    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(category.replaceFirstChar { it.uppercase() }.replace('_', ' '),
            onBack = { nav.popBackStack() })
        if (words == null) { LoadingBox(); return }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(words, key = { it.id }) { v ->
                WordRow(v, ttsStatus == TtsStatus.READY,
                    onPlay = { container.tts.speak(v.korean, v.id) },
                    onClick = { nav.navigate(Routes.word(v.id)) })
            }
        }
    }
}

@Composable
fun WordDetailScreen(nav: NavController, id: String) {
    val container = localContainer()
    val scope = rememberCoroutineScope()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()
    val favIds by container.repository.favoriteIdsFlow().collectAsStateWithLifecycle(emptyList())
    val v = loadContent(id) { it.vocab(id) }
    val ready = ttsStatus == TtsStatus.READY

    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.learn_vocabulary), onBack = { nav.popBackStack() }, actions = {
            if (v != null) FavouriteButton(v.id in favIds) {
                scope.launch { container.repository.toggleFavorite(v.id, "vocab") }
            }
        })
        if (v == null) { LoadingBox(); return }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            if (!ready) {
                TtsUnavailableBanner()
                Spacer(Modifier.height(16.dp))
            }
            KoreanText(v.korean, style = SamjhoType.koreanHero(LocalFontScale.current))
            RomanizationText(v.romanization)
            Spacer(Modifier.height(12.dp))
            AudioButton(
                onPlay = { container.tts.speak(v.korean, v.id) },
                onPlaySlow = { container.tts.speak(v.korean, v.id, slow = true) },
                enabled = ready
            )
            Spacer(Modifier.height(20.dp))
            LabelledBlock(stringResource(R.string.meaning)) {
                TranslationText(v.meaning, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(v.pos) }, enabled = false)
                LevelChip(v.level)
                AssistChip(onClick = { nav.navigate(Routes.vocabCategory(v.category)) },
                    label = { Text(v.category.replace('_', ' ')) })
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            LabelledBlock(stringResource(R.string.example)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        KoreanText(v.exampleKorean, style = SamjhoType.koreanSentence(LocalFontScale.current))
                        RomanizationText(v.exampleRomanization)
                        Spacer(Modifier.height(6.dp))
                        TranslationText(v.exampleTranslation, color = MaterialTheme.colorScheme.onSurface)
                    }
                    AudioButton(
                        onPlay = { container.tts.speak(v.exampleKorean, v.id + ".ex") },
                        onPlaySlow = { container.tts.speak(v.exampleKorean, v.id + ".ex", slow = true) },
                        enabled = ready
                    )
                }
            }
            if (v.reviewStatus != com.koreansamjho.app.domain.model.ReviewStatus.REVIEWED_FULL) {
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.review_pending),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LabelledBlock(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
fun GrammarListScreen(nav: NavController) {
    val grammar = loadContent(Unit) { it.allGrammar() }
    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.learn_grammar), onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(grammar.orEmpty(), key = { it.id }) { g ->
                SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    onClick = { nav.navigate(Routes.grammar(g.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            KoreanText(g.pattern, style = SamjhoType.koreanWord(LocalFontScale.current))
                            Text(g.titleEn, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LevelChip(g.level)
                    }
                }
            }
        }
    }
}

@Composable
fun GrammarDetailScreen(nav: NavController, id: String) {
    val container = localContainer()
    val scope = rememberCoroutineScope()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()
    val favIds by container.repository.favoriteIdsFlow().collectAsStateWithLifecycle(emptyList())
    val g = loadContent(id) { it.grammar(id) }
    val ready = ttsStatus == TtsStatus.READY

    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.learn_grammar), onBack = { nav.popBackStack() }, actions = {
            if (g != null) FavouriteButton(g.id in favIds) {
                scope.launch { container.repository.toggleFavorite(g.id, "grammar") }
            }
        })
        if (g == null) { LoadingBox(); return }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            KoreanText(g.pattern, style = SamjhoType.koreanHero(LocalFontScale.current))
            Text(g.titleEn, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LevelChip(g.level)
                AssistChip(onClick = {}, label = { Text(g.formality) }, enabled = false)
            }
            Spacer(Modifier.height(20.dp))
            LabelledBlock(stringResource(R.string.grammar_structure)) {
                ForceLtr {
                    Text(g.structure, style = SamjhoType.koreanOption(LocalFontScale.current))
                }
            }
            Spacer(Modifier.height(20.dp))
            LabelledBlock(stringResource(R.string.grammar_when)) {
                TranslationText(g.explanation, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(20.dp))
            LabelledBlock(stringResource(R.string.grammar_examples)) {
                Column {
                    g.examples.forEach { ex ->
                        SamjhoCard(Modifier.padding(vertical = 5.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    KoreanText(ex.korean, style = SamjhoType.koreanSentence(LocalFontScale.current))
                                    RomanizationText(ex.romanization)
                                    Spacer(Modifier.height(6.dp))
                                    TranslationText(ex.translation, color = MaterialTheme.colorScheme.onSurface)
                                }
                                AudioButton(
                                    onPlay = { container.tts.speak(ex.korean, ex.korean) },
                                    onPlaySlow = { container.tts.speak(ex.korean, ex.korean, slow = true) },
                                    enabled = ready
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            SamjhoCard(container = LocalSamjhoColors.current.warningContainer) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.ErrorOutline, null, tint = LocalSamjhoColors.current.warning)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(stringResource(R.string.grammar_mistake),
                            style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        TranslationText(g.commonMistake, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ScenarioListScreen(nav: NavController) {
    val lang = LocalLang.current
    val scenarios = loadContent(Unit) { it.scenarios() }
    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.learn_sentences), onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(scenarios.orEmpty(), key = { it.id }) { s ->
                SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    onClick = { nav.navigate(Routes.scenario(s.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.title[lang], style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f))
                        Text("${s.count}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun SentenceListScreen(nav: NavController, scenario: String) {
    val container = localContainer()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()
    val items = loadContent(scenario) { it.sentences(scenario) }
    val lang = LocalLang.current
    val scenarios = loadContent(Unit) { it.scenarios() }
    val title = scenarios?.firstOrNull { it.id == scenario }?.title?.get(lang)
    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(title ?: stringResource(R.string.learn_sentences), onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(items.orEmpty(), key = { it.id }) { s ->
                SentenceCard(s, ttsStatus == TtsStatus.READY,
                    onPlay = { container.tts.speak(s.korean, s.id) },
                    onPlaySlow = { container.tts.speak(s.korean, s.id, slow = true) })
            }
        }
    }
}

@Composable
fun InterviewScreen(nav: NavController) {
    val container = localContainer()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()
    val items = loadContent(Unit) { it.interview() }
    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.practice_interview), onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(items.orEmpty(), key = { it.id }) { q ->
                InterviewCard(q, ttsStatus == TtsStatus.READY) { container.tts.speak(it, q.id) }
            }
        }
    }
}

@Composable
fun FavouritesScreen(nav: NavController) {
    val container = localContainer()
    val ttsStatus by container.tts.status.collectAsStateWithLifecycle()
    val favs by container.repository.favoritesFlow().collectAsStateWithLifecycle(emptyList())
    val words = loadContent(favs.size) { repo ->
        favs.filter { it.itemType == "vocab" }.mapNotNull { repo.vocab(it.itemId) }
    }
    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.favourites), onBack = { nav.popBackStack() })
        if (words.isNullOrEmpty()) {
            EmptyState(stringResource(R.string.empty_nothing_here), Icons.Outlined.StarBorder)
            return
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            items(words, key = { it.id }) { v ->
                WordRow(v, ttsStatus == TtsStatus.READY,
                    onPlay = { container.tts.speak(v.korean, v.id) },
                    onClick = { nav.navigate(Routes.word(v.id)) })
            }
        }
    }
}

/** Offline search across Korean, romanization, English, Urdu and Hindi in one query. */
@Composable
fun SearchScreen(nav: NavController) {
    val container = localContainer()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<com.koreansamjho.app.domain.model.SearchHit>>(emptyList()) }
    LaunchedEffect(query) {
        results = if (query.isBlank()) emptyList() else container.repository.search(query)
    }
    Column(Modifier.fillMaxSize()) {
        SamjhoTopBar(stringResource(R.string.search), onBack = { nav.popBackStack() })
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        Spacer(Modifier.height(8.dp))
        when {
            query.isBlank() -> EmptyState(stringResource(R.string.search_empty), Icons.Outlined.Search)
            results.isEmpty() -> EmptyState(stringResource(R.string.search_none, query), Icons.Outlined.SearchOff)
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(results, key = { it.entityType + it.entityId }) { hit ->
                    SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 5.dp), onClick = {
                        when (hit.entityType) {
                            "vocab" -> nav.navigate(Routes.word(hit.entityId))
                            "grammar" -> nav.navigate(Routes.grammar(hit.entityId))
                            else -> {}
                        }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                KoreanText(hit.korean, style = SamjhoType.koreanWord(LocalFontScale.current))
                                RomanizationText(hit.romanization)
                                Spacer(Modifier.height(4.dp))
                                TranslationText(hit.translation, color = MaterialTheme.colorScheme.onSurface)
                            }
                            AssistChip(onClick = {}, enabled = false,
                                label = { Text(hit.entityType, style = MaterialTheme.typography.labelMedium) })
                        }
                    }
                }
            }
        }
    }
}
