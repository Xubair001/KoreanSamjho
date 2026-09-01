package com.koreansamjho.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.koreansamjho.app.R
import com.koreansamjho.app.domain.engine.ProgressCalculator
import com.koreansamjho.app.ui.components.*
import com.koreansamjho.app.ui.localContainer
import com.koreansamjho.app.ui.theme.LocalSamjhoColors

@Composable
fun ProgressScreen(nav: NavController) {
    val container = localContainer()
    val repo = container.repository
    val learned by repo.learnedCountFlow().collectAsStateWithLifecycle(0)
    val mastered by repo.masteredCountFlow().collectAsStateWithLifecycle(0)
    val lessons by repo.lessonsCompletedFlow().collectAsStateWithLifecycle(0)
    val attempts by repo.attemptCountFlow().collectAsStateWithLifecycle(0)
    val xp by repo.totalXpFlow().collectAsStateWithLifecycle(0)
    val seconds by repo.totalSecondsFlow().collectAsStateWithLifecycle(0)
    val streak = loadContent(learned) { it.currentStreak() } ?: 0
    val longest = loadContent(learned) { it.longestStreak() } ?: 0
    val skills = loadContent(attempts) { it.skillAccuracy() } ?: emptyList()
    val weak = loadContent(attempts) { it.categoryAccuracy(2) } ?: emptyList()
    val extras = LocalSamjhoColors.current

    val level = ProgressCalculator.levelForXp(xp)
    val (into, need) = ProgressCalculator.xpIntoLevel(xp)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text(stringResource(R.string.progress_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp))
        }
        item {
            SamjhoCard(Modifier.padding(horizontal = 16.dp),
                container = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(if (need == 0) 0f else into.toFloat() / need,
                        Modifier.size(72.dp), label = "$level")
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.progress_level, level),
                            style = MaterialTheme.typography.titleLarge)
                        ForceLtr {
                            Text(stringResource(R.string.progress_xp, into, need),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (streak > 0) StreakBadge(streak)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("$learned", stringResource(R.string.progress_words_learned), Modifier.weight(1f))
                StatTile("$mastered", stringResource(R.string.progress_mastered), Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("$lessons", stringResource(R.string.progress_lessons), Modifier.weight(1f))
                StatTile("$attempts", stringResource(R.string.progress_tests), Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("$longest", stringResource(R.string.progress_longest), Modifier.weight(1f))
                StatTile("${seconds / 60}m", stringResource(R.string.progress_time), Modifier.weight(1f))
            }
        }

        if (skills.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.progress_skills)) }
            items(skills) { s ->
                val pct = ProgressCalculator.accuracyPercent(s.correct, s.total)
                SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.kind.lowercase().replace('_', ' '),
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

        if (weak.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.weak_areas)) }
            items(weak.sortedBy { ProgressCalculator.accuracyPercent(it.correct, it.total) }.take(5)) { c ->
                val pct = ProgressCalculator.accuracyPercent(c.correct, c.total)
                SamjhoCard(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.category.replace('_', ' '),
                            style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        ForceLtr { Text("$pct%", style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }
        }

        item { SectionHeader(stringResource(R.string.achievements)) }
        item {
            val list = listOf(
                Triple(R.string.ach_first_day, learned >= 1, Icons.Outlined.Flag),
                Triple(R.string.ach_streak_7, streak >= 7, Icons.Outlined.LocalFireDepartment),
                Triple(R.string.ach_streak_30, streak >= 30, Icons.Outlined.Whatshot),
                Triple(R.string.ach_100_words, learned >= 100, Icons.Outlined.Translate),
                Triple(R.string.ach_10_lessons, lessons >= 10, Icons.Outlined.MenuBook),
                Triple(R.string.ach_test_master, attempts >= 5, Icons.Outlined.EmojiEvents),
            )
            Column(Modifier.padding(horizontal = 16.dp)) {
                list.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (res, unlocked, icon) ->
                            SamjhoCard(
                                Modifier.weight(1f).padding(vertical = 5.dp),
                                container = if (unlocked) extras.successContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ) {
                                Icon(icon, null,
                                    tint = if (unlocked) extras.success else MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.height(6.dp))
                                Text(stringResource(res), style = MaterialTheme.typography.labelMedium)
                                if (!unlocked) Text(stringResource(R.string.locked),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
