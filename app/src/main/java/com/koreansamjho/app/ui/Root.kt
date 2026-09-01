package com.koreansamjho.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.koreansamjho.app.R
import com.koreansamjho.app.data.prefs.Settings
import com.koreansamjho.app.ui.navigation.Routes
import com.koreansamjho.app.ui.screen.*
import com.koreansamjho.app.ui.theme.LocalReducedMotion

private data class Tab(val route: String, val labelRes: Int, val on: ImageVector, val off: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    Tab(Routes.LEARN, R.string.nav_learn, Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    Tab(Routes.PRACTICE, R.string.nav_practice, Icons.Filled.Psychology, Icons.Outlined.Psychology),
    Tab(Routes.TESTS, R.string.nav_tests, Icons.Filled.Assignment, Icons.Outlined.Assignment),
    Tab(Routes.PROGRESS, R.string.nav_progress, Icons.Filled.Insights, Icons.Outlined.Insights),
)

@Composable
fun SamjhoRoot(appVm: AppViewModel, settings: Settings) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination
    val onTab = TABS.any { t -> current?.hierarchy?.any { it.route == t.route } == true }
    val reduced = LocalReducedMotion.current
    val dur = if (reduced) 0 else 250

    Scaffold(
        bottomBar = {
            if (onTab) NavigationBar {
                TABS.forEach { tab ->
                    val selected = current?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) tab.on else tab.off, null) },
                        label = { Text(stringResource(tab.labelRes), maxLines = 1) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = if (settings.onboarded) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.padding(inner),
            enterTransition = { fadeIn(tween(dur)) },
            exitTransition = { fadeOut(tween(dur)) },
            popEnterTransition = { fadeIn(tween(dur)) },
            popExitTransition = { fadeOut(tween(dur)) },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(appVm) {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                }
            }
            composable(Routes.HOME) { HomeScreen(nav, settings) }
            composable(Routes.LEARN) { LearnScreen(nav, settings) }
            composable(Routes.PRACTICE) { PracticeScreen(nav) }
            composable(Routes.TESTS) { TestsScreen(nav, settings) }
            composable(Routes.PROGRESS) { ProgressScreen(nav) }

            composable(Routes.COURSE, listOf(navArgument("courseId") { type = NavType.StringType })) {
                CourseScreen(nav, it.arguments?.getString("courseId").orEmpty())
            }
            composable(Routes.LESSON, listOf(navArgument("lessonId") { type = NavType.StringType })) {
                LessonScreen(nav, it.arguments?.getString("lessonId").orEmpty())
            }
            composable(Routes.VOCAB_LIST) { VocabCategoriesScreen(nav) }
            composable(Routes.VOCAB_CATEGORY, listOf(navArgument("category") { type = NavType.StringType })) {
                VocabListScreen(nav, it.arguments?.getString("category").orEmpty())
            }
            composable(Routes.VOCAB_DETAIL, listOf(navArgument("id") { type = NavType.StringType })) {
                WordDetailScreen(nav, it.arguments?.getString("id").orEmpty())
            }
            composable(Routes.GRAMMAR_LIST) { GrammarListScreen(nav) }
            composable(Routes.GRAMMAR_DETAIL, listOf(navArgument("id") { type = NavType.StringType })) {
                GrammarDetailScreen(nav, it.arguments?.getString("id").orEmpty())
            }
            composable(Routes.SENTENCE_LIST) { ScenarioListScreen(nav) }
            composable(Routes.SENTENCE_SCENARIO, listOf(navArgument("scenario") { type = NavType.StringType })) {
                SentenceListScreen(nav, it.arguments?.getString("scenario").orEmpty())
            }
            composable(Routes.INTERVIEW) { InterviewScreen(nav) }
            composable(Routes.FAVOURITES) { FavouritesScreen(nav) }
            composable(Routes.SEARCH) { SearchScreen(nav) }
            composable(Routes.SETTINGS) { SettingsScreen(nav, appVm, settings) }
            composable(Routes.ABOUT) { AboutScreen(nav) }
            composable(Routes.EXAM_INFO) { ExamInfoScreen(nav, settings) }
            composable(Routes.TEST_HISTORY) { TestHistoryScreen(nav) }

            composable(
                Routes.QUIZ,
                listOf(navArgument("source") { type = NavType.StringType },
                    navArgument("arg") { type = NavType.StringType })
            ) {
                QuizScreen(nav,
                    it.arguments?.getString("source").orEmpty(),
                    it.arguments?.getString("arg").orEmpty())
            }
            composable(
                Routes.TEST,
                listOf(navArgument("kind") { type = NavType.StringType },
                    navArgument("minutes") { type = NavType.IntType })
            ) {
                TestRunnerScreen(nav, settings,
                    it.arguments?.getString("kind").orEmpty(),
                    it.arguments?.getInt("minutes") ?: 0)
            }
            composable(Routes.TEST_RESULT, listOf(navArgument("attemptId") { type = NavType.LongType })) {
                TestResultScreen(nav, it.arguments?.getLong("attemptId") ?: 0L)
            }
        }
    }
}
