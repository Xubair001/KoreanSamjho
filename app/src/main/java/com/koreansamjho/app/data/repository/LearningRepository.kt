package com.koreansamjho.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.koreansamjho.app.data.content.ContentDb
import com.koreansamjho.app.data.progress.*
import com.koreansamjho.app.domain.engine.ProgressCalculator
import com.koreansamjho.app.domain.engine.SrsScheduler
import com.koreansamjho.app.domain.model.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Content shown by a lesson, resolved from its selector at read time. */
sealed interface LessonContent {
    data class Letters(val items: List<Letter>) : LessonContent
    data class Words(val items: List<Vocab>) : LessonContent
    data class Grammars(val items: List<Grammar>) : LessonContent
    data class Sentences(val items: List<Sentence>) : LessonContent
    data class Interview(val items: List<InterviewItem>) : LessonContent
    data class Syllables(val consonants: List<Letter>, val vowels: List<Letter>) : LessonContent
    data class Batchim(val letters: List<Letter>, val examples: List<Vocab>) : LessonContent
}

data class DueItem(val itemId: String, val itemType: String, val box: Int)

class LearningRepository(
    private val content: ContentDb,
    private val dao: ProgressDao,
) {
    private val io = Dispatchers.IO

    // ---------- content passthrough ----------
    suspend fun courses(track: Track) = withContext(io) { content.courses(track) }
    suspend fun lessons(courseId: String) = withContext(io) { content.lessons(courseId) }
    suspend fun lesson(id: String) = withContext(io) { content.lessonById(id) }
    suspend fun allLessons(track: Track) = withContext(io) { content.allLessons(track) }
    suspend fun letters(kind: String? = null) = withContext(io) { content.letters(kind) }
    suspend fun vocabCategories() = withContext(io) { content.vocabCategories() }
    suspend fun vocabByCategory(c: String) = withContext(io) { content.vocabByCategory(c) }
    suspend fun allVocab() = withContext(io) { content.allVocab() }
    suspend fun vocab(id: String) = withContext(io) { content.vocabById(id) }
    suspend fun allGrammar() = withContext(io) { content.allGrammar() }
    suspend fun grammar(id: String) = withContext(io) { content.grammarById(id) }
    suspend fun scenarios() = withContext(io) { content.scenarios() }
    suspend fun sentences(scenario: String) = withContext(io) { content.sentencesByScenario(scenario) }
    suspend fun interview() = withContext(io) { content.interview() }
    suspend fun exams() = withContext(io) { content.exams() }
    suspend fun sources() = withContext(io) { content.sources() }
    suspend fun passage(id: String) = withContext(io) { content.passageById(id) }
    suspend fun contentVersion() = withContext(io) { content.meta("content_version") ?: "?" }
    suspend fun contentLicence() = withContext(io) { content.meta("licence").orEmpty() }
    suspend fun search(q: String) = withContext(io) { content.search(q) }
    suspend fun warmUp() = withContext(io) { content.warmUp() }

    suspend fun dailyWord() = withContext(io) { content.randomVocab() }
    suspend fun dailySentence() = withContext(io) { content.randomSentence() }

    /** Resolves a lesson's selector into the content it should display. */
    suspend fun lessonContent(lesson: Lesson): LessonContent = withContext(io) {
        val sel = runCatching { JSONObject(lesson.selector) }.getOrDefault(JSONObject())
        when (lesson.kind) {
            LessonKind.HANGUL -> LessonContent.Letters(content.letters(sel.optString("kind").ifBlank { null }))
            LessonKind.SYLLABLE -> LessonContent.Syllables(
                content.letters("consonant").take(8), content.letters("vowel")
            )
            LessonKind.BATCHIM -> LessonContent.Batchim(
                content.letters("consonant"),
                content.allVocab().filter { hasBatchim(it.korean) }.take(12)
            )
            LessonKind.VOCAB -> {
                val cat = sel.optString("category").takeIf { it.isNotBlank() }
                val level = if (sel.has("level")) sel.optInt("level") else null
                val limit = if (sel.has("limit")) sel.optInt("limit") else 200
                val items = when {
                    cat != null -> content.vocabByCategory(cat)
                    level != null -> content.vocabByLevel(level, limit)
                    else -> content.vocabByLevel(1, limit)
                }
                LessonContent.Words(items.take(limit))
            }
            LessonKind.GRAMMAR -> {
                val ids = sel.optJSONArray("ids")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
                LessonContent.Grammars(content.grammarByIds(ids))
            }
            LessonKind.SENTENCE -> LessonContent.Sentences(content.sentencesByScenario(sel.optString("scenario")))
            LessonKind.INTERVIEW -> LessonContent.Interview(content.interview())
        }
    }

    private fun hasBatchim(word: String): Boolean = word.any { ch ->
        val c = ch.code
        c in 0xAC00..0xD7A3 && (c - 0xAC00) % 28 != 0
    }

    // ---------- SRS ----------
    suspend fun recordAnswer(itemId: String, itemType: String, correct: Boolean, confident: Boolean = true) =
        withContext(io) {
            val now = System.currentTimeMillis()
            val existing = dao.review(itemId)
            val state = existing?.let {
                SrsScheduler.State(it.box, it.ease, it.dueAt, it.correctCount, it.wrongCount, it.lapses)
            } ?: SrsScheduler.State()
            val next = SrsScheduler.next(state, correct, confident, now)
            dao.upsertReview(
                ReviewItemEntity(itemId, itemType, next.box, next.ease, next.dueAt, now,
                    next.correctCount, next.wrongCount, next.lapses)
            )
            recordActivity(itemsReviewed = 1, xp = if (correct) 10 else 3)
        }

    suspend fun dueItems(limit: Int = 30): List<DueItem> = withContext(io) {
        dao.dueItems(System.currentTimeMillis(), limit).map { DueItem(it.itemId, it.itemType, it.box) }
    }
    suspend fun difficultItems(limit: Int = 30): List<DueItem> = withContext(io) {
        dao.difficultItems(limit).map { DueItem(it.itemId, it.itemType, it.box) }
    }
    suspend fun recentItems(limit: Int = 30): List<DueItem> = withContext(io) {
        dao.recentItems(limit).map { DueItem(it.itemId, it.itemType, it.box) }
    }
    fun dueCountFlow(): Flow<Int> = dao.dueCountFlow(System.currentTimeMillis())
    fun learnedCountFlow(): Flow<Int> = dao.learnedCountFlow()
    fun masteredCountFlow(): Flow<Int> = dao.masteredCountFlow()

    /** Resolves due review ids back into practice questions. */
    suspend fun questionsForDue(limit: Int = 15): List<Question> = withContext(io) {
        val due = dueItems(limit).filter { it.itemType == "vocab" }.map { it.itemId }
        val qs = content.questionsForVocab(due)
        if (qs.size >= 5) qs else qs + content.questionsForTrack(Track.GENERAL,
            listOf("vocab_ko_to_meaning"), limit - qs.size)
    }

    suspend fun questionsByIds(ids: List<String>): List<Question> = withContext(io) {
        content.questionsForIds(ids)
    }

    suspend fun questionsForVocabIds(vocabIds: List<String>): List<Question> = withContext(io) {
        content.questionsForVocab(vocabIds)
    }

    /** Practice questions drawn from exactly the content a lesson teaches. */
    suspend fun questionsForLesson(lessonId: String, limit: Int): List<Question> = withContext(io) {
        val lesson = content.lessonById(lessonId) ?: return@withContext emptyList<Question>()
        when (val c = lessonContent(lesson)) {
            is LessonContent.Words -> content.questionsForVocab(c.items.map { it.id }).take(limit)
            is LessonContent.Batchim -> content.questionsForVocab(c.examples.map { it.id }).take(limit)
            is LessonContent.Grammars -> content.questionsByKind("grammar", limit)
            else -> emptyList()
        }
    }

    // ---------- lessons ----------
    suspend fun completeLesson(lesson: Lesson, scorePercent: Int) = withContext(io) {
        dao.upsertLesson(LessonProgressEntity(lesson.id, lesson.courseId, System.currentTimeMillis(), scorePercent))
        recordActivity(xp = 25)
    }
    fun lessonProgressFlow(): Flow<List<LessonProgressEntity>> = dao.lessonProgressFlow()
    /** One-shot set of completed lesson ids, for "what should I do next". */
    suspend fun lessonProgressFlowSnapshot(): Set<String> = withContext(io) {
        dao.lessonProgressFlow().first().map { e -> e.lessonId }.toSet()
    }
    suspend fun isLessonComplete(id: String): Boolean = withContext(io) { dao.lessonProgress(id) != null }
    fun lessonsCompletedFlow(): Flow<Int> = dao.lessonsCompletedFlow()

    // ---------- tests ----------
    suspend fun buildTest(track: Track, kinds: List<String>, count: Int, maxLevel: Int = 4): List<Question> =
        withContext(io) { content.questionsForTrack(track, kinds, count, maxLevel) }

    suspend fun saveAttempt(
        kind: String, track: Track, startedAt: Long, finishedAt: Long,
        questions: List<Question>, selected: Map<String, Int>,
    ): Long = withContext(io) {
        val answers = questions.map { q ->
            val sel = selected[q.id] ?: -1
            Triple(q, sel, sel == q.correctIndex)
        }
        val correct = answers.count { it.third }
        val attemptId = dao.insertAttempt(
            TestAttemptEntity(
                kind = kind, track = track.name, startedAt = startedAt, finishedAt = finishedAt,
                totalQuestions = questions.size, correctCount = correct,
                scorePercent = ProgressCalculator.accuracyPercent(correct, questions.size),
                durationMs = finishedAt - startedAt
            )
        )
        dao.insertAnswers(answers.map { (q, sel, ok) ->
            TestAnswerEntity(attemptId = attemptId, questionId = q.id, selectedIndex = sel,
                correct = ok, category = q.category, kind = q.kind.name)
        })
        recordActivity(xp = correct * 5, studySeconds = ((finishedAt - startedAt) / 1000).toInt())
        attemptId
    }

    suspend fun attempt(id: Long) = withContext(io) { dao.attempt(id) }
    suspend fun attemptAnswers(id: Long) = withContext(io) { dao.answers(id) }
    fun attemptsFlow(limit: Int = 30) = dao.attemptsFlow(limit)
    fun attemptCountFlow() = dao.attemptCountFlow()
    suspend fun categoryAccuracy(min: Int = 3) = withContext(io) { dao.categoryAccuracy(min) }
    suspend fun skillAccuracy() = withContext(io) { dao.skillAccuracy() }

    // ---------- favourites ----------
    suspend fun toggleFavorite(itemId: String, itemType: String): Boolean = withContext(io) {
        if (dao.isFavorite(itemId) > 0) { dao.removeFavorite(itemId); false }
        else { dao.addFavorite(FavoriteEntity(itemId, itemType, System.currentTimeMillis())); true }
    }
    fun favoritesFlow() = dao.favoritesFlow()
    fun favoriteIdsFlow() = dao.favoriteIdsFlow()

    // ---------- activity, streak, XP ----------
    suspend fun recordActivity(studySeconds: Int = 0, itemsReviewed: Int = 0, xp: Int = 0) = withContext(io) {
        val day = todayEpochDay()
        val existing = dao.day(day)
        dao.upsertDay(
            DailyActivityEntity(
                epochDay = day,
                studySeconds = (existing?.studySeconds ?: 0) + studySeconds,
                itemsReviewed = (existing?.itemsReviewed ?: 0) + itemsReviewed,
                xp = (existing?.xp ?: 0) + xp
            )
        )
    }
    suspend fun currentStreak(): Int = withContext(io) {
        ProgressCalculator.currentStreak(dao.activeDays(), todayEpochDay())
    }
    suspend fun longestStreak(): Int = withContext(io) { ProgressCalculator.longestStreak(dao.activeDays()) }
    suspend fun todayActivity(): DailyActivityEntity? = withContext(io) { dao.day(todayEpochDay()) }
    fun recentDaysFlow(limit: Int = 7) = dao.recentDaysFlow(limit)
    fun totalXpFlow() = dao.totalXpFlow()
    fun totalSecondsFlow() = dao.totalSecondsFlow()

    // ---------- achievements ----------
    fun achievementsFlow() = dao.achievementsFlow()
    suspend fun evaluateAchievements(): List<String> = withContext(io) {
        val unlocked = dao.unlockedIds().toSet()
        val newly = mutableListOf<String>()
        suspend fun grant(id: String, condition: Boolean) {
            if (condition && id !in unlocked) {
                dao.unlock(AchievementEntity(id, System.currentTimeMillis())); newly.add(id)
            }
        }
        val streak = currentStreak()
        val learnedWords = dao.learnedCountFlow().first()
        val lessonsDone = dao.lessonsCompletedFlow().first()
        val tests = dao.attemptCountFlow().first()
        grant("ach.firstday", dao.activeDays().isNotEmpty())
        grant("ach.streak7", streak >= 7)
        grant("ach.streak30", streak >= 30)
        grant("ach.words100", learnedWords >= 100)
        grant("ach.lessons10", lessonsDone >= 10)
        grant("ach.testmaster", tests >= 5)
        newly
    }

    private fun todayEpochDay(): Long =
        TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() + java.util.TimeZone.getDefault().rawOffset)
}
