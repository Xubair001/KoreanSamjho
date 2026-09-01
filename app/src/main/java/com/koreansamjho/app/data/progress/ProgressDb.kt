package com.koreansamjho.app.data.progress

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * User-generated data. Separate from content.db on purpose: content is replaced
 * wholesale on update, progress must survive forever. Rows reference content by
 * stable string id, so content can be regenerated without orphaning progress.
 */

@Entity(tableName = "review_item")
data class ReviewItemEntity(
    @PrimaryKey val itemId: String,
    val itemType: String,          // vocab | grammar | sentence | letter
    val box: Int = 0,
    val ease: Double = 2.0,
    val dueAt: Long = 0L,
    val lastReviewedAt: Long = 0L,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val lapses: Int = 0,
)

@Entity(tableName = "lesson_progress")
data class LessonProgressEntity(
    @PrimaryKey val lessonId: String,
    val courseId: String,
    val completedAt: Long,
    val scorePercent: Int,
)

@Entity(tableName = "test_attempt")
data class TestAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val track: String,
    val startedAt: Long,
    val finishedAt: Long,
    val totalQuestions: Int,
    val correctCount: Int,
    val scorePercent: Int,
    val durationMs: Long,
)

@Entity(tableName = "test_answer")
data class TestAnswerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attemptId: Long,
    val questionId: String,
    val selectedIndex: Int,
    val correct: Boolean,
    val category: String,
    val kind: String,
)

@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey val itemId: String,
    val itemType: String,
    val addedAt: Long,
)

@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val epochDay: Long,
    val studySeconds: Int = 0,
    val itemsReviewed: Int = 0,
    val xp: Int = 0,
)

@Entity(tableName = "achievement")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val unlockedAt: Long,
)

@Dao
interface ProgressDao {
    // --- review / SRS ---
    @Query("SELECT * FROM review_item WHERE itemId = :id")
    suspend fun review(id: String): ReviewItemEntity?

    @Query("SELECT * FROM review_item WHERE dueAt <= :now ORDER BY dueAt ASC LIMIT :limit")
    suspend fun dueItems(now: Long, limit: Int): List<ReviewItemEntity>

    @Query("SELECT COUNT(*) FROM review_item WHERE dueAt <= :now")
    fun dueCountFlow(now: Long): Flow<Int>

    @Query("SELECT * FROM review_item WHERE wrongCount > correctCount ORDER BY wrongCount DESC LIMIT :limit")
    suspend fun difficultItems(limit: Int): List<ReviewItemEntity>

    @Query("SELECT * FROM review_item ORDER BY lastReviewedAt DESC LIMIT :limit")
    suspend fun recentItems(limit: Int): List<ReviewItemEntity>

    @Query("SELECT COUNT(*) FROM review_item")
    fun learnedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM review_item WHERE box >= 3")
    fun masteredCountFlow(): Flow<Int>

    @Upsert suspend fun upsertReview(item: ReviewItemEntity)

    // --- lessons ---
    @Upsert suspend fun upsertLesson(p: LessonProgressEntity)
    @Query("SELECT * FROM lesson_progress") fun lessonProgressFlow(): Flow<List<LessonProgressEntity>>
    @Query("SELECT * FROM lesson_progress WHERE lessonId = :id") suspend fun lessonProgress(id: String): LessonProgressEntity?
    @Query("SELECT COUNT(*) FROM lesson_progress") fun lessonsCompletedFlow(): Flow<Int>

    // --- tests ---
    @Insert suspend fun insertAttempt(a: TestAttemptEntity): Long
    @Insert suspend fun insertAnswers(a: List<TestAnswerEntity>)
    @Query("SELECT * FROM test_attempt ORDER BY finishedAt DESC LIMIT :limit")
    fun attemptsFlow(limit: Int): Flow<List<TestAttemptEntity>>
    @Query("SELECT * FROM test_attempt WHERE id = :id") suspend fun attempt(id: Long): TestAttemptEntity?
    @Query("SELECT * FROM test_answer WHERE attemptId = :id") suspend fun answers(id: Long): List<TestAnswerEntity>
    @Query("SELECT COUNT(*) FROM test_attempt") fun attemptCountFlow(): Flow<Int>

    /** Accuracy per category across all attempts — drives weak-area analysis. */
    @Query("""SELECT category, COUNT(*) total, SUM(CASE WHEN correct THEN 1 ELSE 0 END) correct
              FROM test_answer GROUP BY category HAVING total >= :minAnswers""")
    suspend fun categoryAccuracy(minAnswers: Int): List<CategoryAccuracy>

    @Query("""SELECT kind, COUNT(*) total, SUM(CASE WHEN correct THEN 1 ELSE 0 END) correct
              FROM test_answer GROUP BY kind""")
    suspend fun skillAccuracy(): List<SkillAccuracy>

    // --- favourites ---
    @Upsert suspend fun addFavorite(f: FavoriteEntity)
    @Query("DELETE FROM favorite WHERE itemId = :id") suspend fun removeFavorite(id: String)
    @Query("SELECT * FROM favorite ORDER BY addedAt DESC") fun favoritesFlow(): Flow<List<FavoriteEntity>>
    @Query("SELECT itemId FROM favorite") fun favoriteIdsFlow(): Flow<List<String>>
    @Query("SELECT COUNT(*) FROM favorite WHERE itemId = :id") suspend fun isFavorite(id: String): Int

    // --- activity / streak ---
    @Upsert suspend fun upsertDay(d: DailyActivityEntity)
    @Query("SELECT * FROM daily_activity WHERE epochDay = :day") suspend fun day(day: Long): DailyActivityEntity?
    @Query("SELECT * FROM daily_activity ORDER BY epochDay DESC LIMIT :limit")
    fun recentDaysFlow(limit: Int): Flow<List<DailyActivityEntity>>
    @Query("SELECT epochDay FROM daily_activity ORDER BY epochDay DESC LIMIT 400")
    suspend fun activeDays(): List<Long>
    @Query("SELECT COALESCE(SUM(xp),0) FROM daily_activity") fun totalXpFlow(): Flow<Int>
    @Query("SELECT COALESCE(SUM(studySeconds),0) FROM daily_activity") fun totalSecondsFlow(): Flow<Int>

    // --- achievements ---
    @Upsert suspend fun unlock(a: AchievementEntity)
    @Query("SELECT * FROM achievement") fun achievementsFlow(): Flow<List<AchievementEntity>>
    @Query("SELECT id FROM achievement") suspend fun unlockedIds(): List<String>
}

data class CategoryAccuracy(val category: String, val total: Int, val correct: Int)
data class SkillAccuracy(val kind: String, val total: Int, val correct: Int)

@Database(
    entities = [ReviewItemEntity::class, LessonProgressEntity::class, TestAttemptEntity::class,
        TestAnswerEntity::class, FavoriteEntity::class, DailyActivityEntity::class,
        AchievementEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun dao(): ProgressDao
}
