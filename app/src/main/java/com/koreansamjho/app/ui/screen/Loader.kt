package com.koreansamjho.app.ui.screen

import androidx.compose.runtime.*
import com.koreansamjho.app.data.repository.LearningRepository
import com.koreansamjho.app.ui.localContainer

/**
 * Read-only screens load through this rather than each owning a near-empty ViewModel.
 * The repository already moves work to the IO dispatcher, and the domain engines stay
 * in the domain layer — screens with real state machinery (quiz, test runner, home,
 * progress) still use proper ViewModels.
 */
@Composable
fun <T> loadContent(key: Any?, block: suspend (LearningRepository) -> T): T? {
    val repo = localContainer().repository
    val state = remember(key) { mutableStateOf<T?>(null) }
    LaunchedEffect(key) { state.value = block(repo) }
    return state.value
}
