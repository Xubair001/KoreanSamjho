package com.koreansamjho.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.koreansamjho.app.R
import com.koreansamjho.app.domain.model.Localized
import com.koreansamjho.app.ui.theme.*

/**
 * Korean, romanization and numerals must stay left-to-right even inside an Urdu
 * right-to-left layout. Inheriting the layout direction is where naive RTL
 * implementations visibly break. See docs/04-technical-architecture.md 4.5.
 */
@Composable
fun ForceLtr(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { content() }
}

@Composable
fun KoreanText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = SamjhoType.koreanWord(LocalFontScale.current),
    color: Color = MaterialTheme.colorScheme.onSurface,
    align: TextAlign = TextAlign.Start,
) {
    ForceLtr {
        Text(text = text, modifier = modifier, style = style, color = color, textAlign = align)
    }
}

@Composable
fun RomanizationText(text: String?, modifier: Modifier = Modifier) {
    if (text.isNullOrBlank() || !LocalShowRomanization.current) return
    ForceLtr {
        Text(
            text = text, modifier = modifier,
            style = SamjhoType.romanization(LocalFontScale.current),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Translation in the learner's language, with the correct script, font and direction. */
@Composable
fun TranslationText(
    text: Localized,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val lang = LocalLang.current
    val value = text[lang]
    if (value.isBlank()) return
    Text(
        text = value, modifier = modifier,
        style = SamjhoType.translation(lang, LocalFontScale.current), color = color
    )
}

@Composable
fun SamjhoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/**
 * Audio control.
 *
 * When the Korean voice is not installed the button is NOT disabled — a dead control
 * reads as a broken app. It stays tappable, shows a muted icon, and tapping it opens
 * the voice-data download directly. Real-device testing was what surfaced this: the
 * engine was healthy, only the Korean voice was missing, and the disabled buttons
 * made it look like audio was simply broken.
 */
@Composable
fun AudioButton(
    onPlay: () -> Unit,
    onPlaySlow: (() -> Unit)? = null,
    enabled: Boolean = true,
    speaking: Boolean = false,
) {
    var askInstall by remember { mutableStateOf(false) }
    val installVoice = { askInstall = true }

    if (askInstall) TtsInstallDialog(onDismiss = { askInstall = false })

    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(
            onClick = if (enabled) onPlay else installVoice,
            modifier = Modifier.size(48.dp).semantics {
                contentDescription = if (enabled) "Play audio" else "Korean voice not installed. Tap to install."
            }
        ) {
            Icon(
                when {
                    !enabled -> Icons.Outlined.VolumeOff
                    speaking -> Icons.Filled.VolumeUp
                    else -> Icons.Outlined.VolumeUp
                },
                contentDescription = null,
                tint = if (enabled) LocalContentColor.current
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onPlaySlow != null) {
            Spacer(Modifier.width(4.dp))
            FilledTonalIconButton(
                onClick = if (enabled) onPlaySlow else installVoice,
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = if (enabled) "Play slowly" else "Korean voice not installed. Tap to install."
                }
            ) {
                Icon(
                    Icons.Outlined.SlowMotionVideo, contentDescription = null,
                    tint = if (enabled) LocalContentColor.current
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FavouriteButton(isFavourite: Boolean, onToggle: () -> Unit) {
    val desc = stringResource(if (isFavourite) R.string.favourite_remove else R.string.favourite_add)
    IconButton(onClick = onToggle, modifier = Modifier.size(48.dp).semantics { contentDescription = desc }) {
        Icon(
            if (isFavourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = null,
            tint = if (isFavourite) LocalSamjhoColors.current.warning else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Answer state is signalled by icon + label as well as colour, never colour alone (§26). */
enum class OptionState { IDLE, SELECTED, CORRECT, INCORRECT }

@Composable
fun AnswerOption(
    korean: String?,
    translation: Localized,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val extras = LocalSamjhoColors.current
    val reduced = LocalReducedMotion.current
    val target = when (state) {
        OptionState.IDLE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        OptionState.SELECTED -> MaterialTheme.colorScheme.primaryContainer
        OptionState.CORRECT -> extras.successContainer
        OptionState.INCORRECT -> MaterialTheme.colorScheme.errorContainer
    }
    val bg by animateColorAsState(target, tween(if (reduced) 0 else 200), label = "optionBg")
    val borderColor = when (state) {
        OptionState.CORRECT -> extras.success
        OptionState.INCORRECT -> MaterialTheme.colorScheme.error
        OptionState.SELECTED -> MaterialTheme.colorScheme.primary
        OptionState.IDLE -> Color.Transparent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(if (state == OptionState.IDLE) 0.dp else 2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (!korean.isNullOrBlank()) {
                KoreanText(korean, style = SamjhoType.koreanOption(LocalFontScale.current))
            }
            if (!translation.isBlank()) TranslationText(translation, color = MaterialTheme.colorScheme.onSurface)
        }
        when (state) {
            OptionState.CORRECT -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = extras.success)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.correct), style = MaterialTheme.typography.labelMedium, color = extras.success)
            }
            OptionState.INCORRECT -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Cancel, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.incorrect), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error)
            }
            else -> {}
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier.size(64.dp),
    stroke: Float = 8f,
    label: String? = null,
) {
    val reduced = LocalReducedMotion.current
    val animated by animateFloatAsState(
        progress.coerceIn(0f, 1f), tween(if (reduced) 0 else 600), label = "ring"
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val bar = MaterialTheme.colorScheme.primary
    Box(modifier, contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val d = Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2)
            drawArc(track, 0f, 360f, false, topLeft, d, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(bar, -90f, 360f * animated, false, topLeft, d, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        if (label != null) {
            ForceLtr { Text(label, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
fun StreakBadge(days: Int) {
    val extras = LocalSamjhoColors.current
    Row(
        Modifier
            .clip(CircleShape)
            .background(extras.warningContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.LocalFireDepartment, null, tint = extras.warning, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.home_streak, days),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    SamjhoCard(modifier) {
        ForceLtr { Text(value, style = MaterialTheme.typography.headlineSmall) }
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LevelChip(level: Int) {
    AssistChip(onClick = {}, label = { ForceLtr { Text("L$level") } }, enabled = false)
}

@Composable
fun EmptyState(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.Inbox) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Used wherever practice content could be mistaken for an official exam (§9). */
@Composable
fun PracticeNotice(expanded: Boolean = false) {
    val extras = LocalSamjhoColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(extras.warningContainer)
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Info, null, tint = extras.warning, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(stringResource(R.string.tests_practice_notice),
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            if (expanded) {
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.tests_practice_explain),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 3.dp)
    }
}

/** Explains a missing Korean voice and links to the system setting, instead of a dead button. */
@Composable
fun TtsMissingCard() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    SamjhoCard(container = LocalSamjhoColors.current.warningContainer) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.VolumeOff, null, tint = LocalSamjhoColors.current.warning)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tts_missing_title),
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.tts_missing_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                val container = com.koreansamjho.app.ui.localContainer()
                Button(onClick = { container.tts.installKoreanVoice(ctx) }) {
                    Text(stringResource(R.string.tts_install))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamjhoTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    androidx.compose.material3.TopAppBar(
        title = { Text(title, maxLines = 1) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = actions
    )
}

/**
 * Asks the user to install the Korean voice, and explains why, before sending them
 * into the system voice-data screen. Jumping straight to an unfamiliar Android
 * settings page with no explanation is disorienting — especially for a first-time
 * smartphone user, who is a core persona for this app.
 */
@Composable
fun TtsInstallDialog(onDismiss: () -> Unit, extraMessage: String? = null) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val container = com.koreansamjho.app.ui.localContainer()
    val status by container.tts.status.collectAsStateWithLifecycle()
    val extras = LocalSamjhoColors.current

    // A device with no TTS engine at all cannot be fixed by installing a voice, so
    // do not offer an action that would lead nowhere — say what is actually wrong.
    val noEngine = status == com.koreansamjho.app.data.audio.TtsStatus.NO_ENGINE

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.VolumeOff, null, tint = extras.warning) },
        title = {
            Text(stringResource(
                if (noEngine) R.string.tts_none_title else R.string.tts_missing_title
            ))
        },
        text = {
            Column {
                if (extraMessage != null) {
                    Text(extraMessage, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                }
                Text(
                    stringResource(if (noEngine) R.string.tts_none_body else R.string.tts_missing_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!noEngine) {
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.tts_how_to),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            if (noEngine) {
                Button(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
            } else {
                Button(onClick = { container.tts.installKoreanVoice(ctx); onDismiss() }) {
                    Text(stringResource(R.string.tts_install))
                }
            }
        },
        dismissButton = {
            if (!noEngine) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.tts_not_now)) }
            }
        }
    )
}

/**
 * Compact inline notice for any screen where audio is part of the point.
 * Tapping it opens the same explain-then-install flow as a muted audio button.
 */
@Composable
fun TtsUnavailableBanner(modifier: Modifier = Modifier, message: String? = null) {
    var ask by remember { mutableStateOf(false) }
    val extras = LocalSamjhoColors.current
    val container = com.koreansamjho.app.ui.localContainer()
    val status by container.tts.status.collectAsStateWithLifecycle()
    val bannerNoEngine = status == com.koreansamjho.app.data.audio.TtsStatus.NO_ENGINE
    if (ask) TtsInstallDialog(onDismiss = { ask = false }, extraMessage = message)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(extras.warningContainer)
            .clickable { ask = true }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.VolumeOff, null, tint = extras.warning, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            message ?: stringResource(
                if (bannerNoEngine) R.string.tts_none_title else R.string.tts_banner_short
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
