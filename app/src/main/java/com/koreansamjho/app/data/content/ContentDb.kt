package com.koreansamjho.app.data.content

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.koreansamjho.app.BuildConfig
import com.koreansamjho.app.domain.model.*
import java.io.File

/**
 * Read-only access to the pre-built content database shipped in assets.
 *
 * Deliberately NOT Room: the content DB is read-only with a fixed schema, so Room's
 * migration machinery buys nothing, while its identity-hash check would couple the
 * content pipeline to the compiled app. Keeping it plain means a new content pack can
 * ship without recompiling. See docs/04-technical-architecture.md 4.1.
 *
 * The file is versioned by name, so "is the bundled content newer?" is a file-existence
 * check rather than a schema query, and old packs are removed on upgrade.
 */
class ContentDb(private val context: Context) {

    private val fileName = "content_v${BuildConfig.CONTENT_VERSION}.db"

    private val db: SQLiteDatabase by lazy {
        val target = File(context.filesDir, fileName)
        if (!target.exists()) {
            context.filesDir.listFiles { f -> f.name.startsWith("content_v") }?.forEach { it.delete() }
            context.assets.open("content.db").use { input ->
                target.outputStream().use { output -> input.copyTo(output, DEFAULT_BUFFER_SIZE) }
            }
        }
        SQLiteDatabase.openDatabase(target.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    /** Touching [db] forces the one-time asset copy; called off the main thread at startup. */
    fun warmUp() { db.rawQuery("SELECT value FROM meta WHERE key='content_version'", null).use { it.moveToFirst() } }

    // ---------- cursor helpers ----------
    private fun <T> query(sql: String, args: Array<String> = emptyArray(), map: (Cursor) -> T): List<T> {
        val out = ArrayList<T>()
        db.rawQuery(sql, args).use { c -> while (c.moveToNext()) out.add(map(c)) }
        return out
    }
    private fun Cursor.s(name: String): String = getString(getColumnIndexOrThrow(name)).orEmpty()
    private fun Cursor.sn(name: String): String? {
        val i = getColumnIndexOrThrow(name); return if (isNull(i)) null else getString(i)
    }
    private fun Cursor.i(name: String): Int = getInt(getColumnIndexOrThrow(name))
    private fun Cursor.inn(name: String): Int? {
        val i = getColumnIndexOrThrow(name); return if (isNull(i)) null else getInt(i)
    }
    private fun Cursor.loc(p: String) = Localized(sn("${p}en"), sn("${p}ur"), sn("${p}hi"))

    // ---------- meta ----------
    fun meta(key: String): String? =
        query("SELECT value FROM meta WHERE key=?", arrayOf(key)) { it.s("value") }.firstOrNull()

    fun sources(): List<SourceRef> = query("SELECT * FROM source ORDER BY id") {
        SourceRef(it.s("id"), it.s("title"), it.s("publisher"), it.s("url"), it.s("licence"), it.s("note"))
    }

    // ---------- hangul ----------
    private fun letterOf(c: Cursor) = Letter(
        c.s("id"), c.s("ch"), c.s("kind"), c.i("ord_"), c.s("romanization"),
        c.s("name_ko"), c.s("name_rr"), c.sn("initial_sound"), c.sn("final_sound"), c.loc("sound_")
    )
    fun letters(kind: String? = null): List<Letter> =
        if (kind == null) query("SELECT * FROM letter ORDER BY kind, ord_") { letterOf(it) }
        else query("SELECT * FROM letter WHERE kind=? ORDER BY ord_", arrayOf(kind)) { letterOf(it) }

    // ---------- vocabulary ----------
    private fun vocabOf(c: Cursor) = Vocab(
        c.s("id"), c.s("korean"), c.s("romanization"), c.s("pos"), c.i("level"), c.s("category"),
        c.loc("m_"), c.s("ex_ko"), c.s("ex_rr"), c.loc("ex_"), ReviewStatus.from(c.sn("review_status"))
    )
    fun vocabByCategory(category: String) =
        query("SELECT * FROM vocab WHERE category=? ORDER BY level, id", arrayOf(category)) { vocabOf(it) }
    fun vocabByLevel(level: Int, limit: Int = 500) =
        query("SELECT * FROM vocab WHERE level=? ORDER BY id LIMIT $limit", arrayOf(level.toString())) { vocabOf(it) }
    fun vocabByIds(ids: List<String>): List<Vocab> {
        if (ids.isEmpty()) return emptyList()
        val ph = ids.joinToString(",") { "?" }
        return query("SELECT * FROM vocab WHERE id IN ($ph)", ids.toTypedArray()) { vocabOf(it) }
    }
    fun vocabById(id: String) = vocabByIds(listOf(id)).firstOrNull()
    fun allVocab() = query("SELECT * FROM vocab ORDER BY level, category, id") { vocabOf(it) }
    fun vocabCategories(): List<Pair<String, Int>> =
        query("SELECT category, COUNT(*) n FROM vocab GROUP BY category ORDER BY category") {
            it.s("category") to it.i("n")
        }
    fun randomVocab(): Vocab? = query("SELECT * FROM vocab ORDER BY RANDOM() LIMIT 1") { vocabOf(it) }.firstOrNull()

    // ---------- grammar ----------
    private fun grammarOf(c: Cursor, examples: List<GrammarExample>) = Grammar(
        c.s("id"), c.i("ord_"), c.s("pattern"), c.s("title_en"), c.i("level"),
        c.s("formality"), c.s("structure"), c.loc("e_"), c.loc("mis_"), examples
    )
    private fun examplesFor(grammarId: String) =
        query("SELECT * FROM grammar_example WHERE grammar_id=? ORDER BY ord_", arrayOf(grammarId)) {
            GrammarExample(it.s("korean"), it.s("romanization"), it.loc(""))
        }
    fun allGrammar(): List<Grammar> =
        query("SELECT * FROM grammar ORDER BY ord_") { c -> grammarOf(c, examplesFor(c.s("id"))) }
    fun grammarById(id: String): Grammar? =
        query("SELECT * FROM grammar WHERE id=?", arrayOf(id)) { grammarOf(it, examplesFor(id)) }.firstOrNull()
    fun grammarByIds(ids: List<String>): List<Grammar> = ids.mapNotNull { grammarById(it) }

    // ---------- sentences ----------
    private fun sentenceOf(c: Cursor) = Sentence(
        c.s("id"), c.s("scenario"), c.i("ord_"), c.i("level"),
        c.s("korean"), c.s("romanization"), c.loc("t_")
    )
    fun scenarios(): List<Scenario> = query(
        "SELECT s.*, (SELECT COUNT(*) FROM sentence x WHERE x.scenario=s.id) n FROM scenario s ORDER BY level, id"
    ) { Scenario(it.s("id"), Localized(it.sn("title_en"), it.sn("title_ur"), it.sn("title_hi")), it.i("level"), it.i("n")) }
    fun sentencesByScenario(scenario: String) =
        query("SELECT * FROM sentence WHERE scenario=? ORDER BY ord_", arrayOf(scenario)) { sentenceOf(it) }
    fun sentenceById(id: String) =
        query("SELECT * FROM sentence WHERE id=?", arrayOf(id)) { sentenceOf(it) }.firstOrNull()
    fun allSentences() = query("SELECT * FROM sentence ORDER BY level, scenario, ord_") { sentenceOf(it) }
    fun randomSentence(): Sentence? =
        query("SELECT * FROM sentence ORDER BY RANDOM() LIMIT 1") { sentenceOf(it) }.firstOrNull()

    // ---------- interview ----------
    fun interview(): List<InterviewItem> = query("SELECT * FROM interview ORDER BY ord_") {
        InterviewItem(it.s("id"), it.i("ord_"), it.s("category"),
            it.s("q_ko"), it.s("q_rr"), it.loc("q_"),
            it.s("a_ko"), it.s("a_rr"), it.loc("a_"), it.s("tip_en"))
    }

    // ---------- exams ----------
    fun exams(): List<ExamInfo> = query("SELECT * FROM exam ORDER BY id") { c ->
        val id = c.s("id")
        val sections = query("SELECT * FROM exam_section WHERE exam_id=? ORDER BY ord_", arrayOf(id)) { s ->
            ExamSection(s.s("name"), s.i("questions"), s.i("points"), s.inn("minutes"))
        }
        val caution = Localized(c.sn("c_en"), c.sn("c_ur"), c.sn("c_hi")).takeIf { !it.isBlank() }
        ExamInfo(id, c.s("code"), c.s("track"), c.loc("n_"), c.loc("w_"), sections,
            c.i("total_questions"), c.i("total_points"), c.s("delivery"), c.loc("s_"),
            c.s("confidence"), c.s("official_url"), caution)
    }

    // ---------- curriculum ----------
    fun courses(track: Track): List<Course> = query(
        "SELECT * FROM course WHERE track='ALL' OR track=? ORDER BY ord_, level",
        arrayOf(track.name)
    ) { Course(it.s("id"), it.s("track"), it.i("level"), it.i("ord_"), it.loc("t_"), it.loc("s_")) }

    fun lessons(courseId: String): List<Lesson> =
        query("SELECT * FROM lesson WHERE course_id=? ORDER BY ord_", arrayOf(courseId)) {
            Lesson(it.s("id"), it.s("course_id"), it.i("ord_"), it.loc("t_"),
                LessonKind.from(it.s("kind")), it.s("selector"), it.i("level"))
        }
    fun lessonById(id: String): Lesson? =
        query("SELECT * FROM lesson WHERE id=?", arrayOf(id)) {
            Lesson(it.s("id"), it.s("course_id"), it.i("ord_"), it.loc("t_"),
                LessonKind.from(it.s("kind")), it.s("selector"), it.i("level"))
        }.firstOrNull()
    fun allLessons(track: Track): List<Lesson> = courses(track).flatMap { lessons(it.id) }

    // ---------- questions ----------
    private fun optionsFor(qid: String) =
        query("SELECT * FROM question_option WHERE question_id=? ORDER BY ord_", arrayOf(qid)) {
            QuestionOption(it.sn("korean"), it.loc(""))
        }
    private fun questionOf(c: Cursor) = Question(
        c.s("id"), QuestionKind.from(c.s("kind")), c.i("level"), c.s("track"), c.s("category"),
        c.sn("p_ko"), c.sn("p_rr"), c.loc("p_"), c.sn("passage_id"),
        optionsFor(c.s("id")), c.i("correct_index"), c.loc("e_"), c.sn("audio_text")
    )
    fun questionsByKind(kind: String, limit: Int) =
        query("SELECT * FROM question WHERE kind=? ORDER BY RANDOM() LIMIT $limit", arrayOf(kind)) { questionOf(it) }
    fun questionsForIds(ids: List<String>): List<Question> {
        if (ids.isEmpty()) return emptyList()
        val ph = ids.joinToString(",") { "?" }
        return query("SELECT * FROM question WHERE id IN ($ph)", ids.toTypedArray()) { questionOf(it) }
    }
    /** Questions matching a track ('ALL' always included) and optional kinds, randomised. */
    fun questionsForTrack(track: Track, kinds: List<String>, limit: Int, maxLevel: Int = 4): List<Question> {
        val kindPh = kinds.joinToString(",") { "?" }
        val args = (kinds + listOf(track.name, maxLevel.toString())).toTypedArray()
        return query(
            "SELECT * FROM question WHERE kind IN ($kindPh) AND (track='ALL' OR track=?) " +
                "AND level<=? ORDER BY RANDOM() LIMIT $limit", args
        ) { questionOf(it) }
    }
    fun questionsForVocab(vocabIds: List<String>): List<Question> {
        if (vocabIds.isEmpty()) return emptyList()
        val qids = vocabIds.map { "q.vocab.km." + it.removePrefix("vocab.") }
        return questionsForIds(qids)
    }
    fun passageById(id: String): Passage? =
        query("SELECT * FROM passage WHERE id=?", arrayOf(id)) {
            Passage(it.s("id"), it.i("level"), it.s("korean"), it.s("romanization"))
        }.firstOrNull()

    // ---------- search ----------
    /** Offline full-text search across Korean, romanization, English, Urdu and Hindi at once. */
    fun search(raw: String, limit: Int = 40): List<SearchHit> {
        val term = raw.trim().replace("\"", " ").replace("*", " ").trim()
        if (term.length < 1) return emptyList()
        val match = term.split(Regex("\\s+")).joinToString(" ") { "$it*" }
        return runCatching {
            query(
                "SELECT entity_type, entity_id, korean, romanization, en, ur, hi FROM search_fts " +
                    "WHERE search_fts MATCH ? LIMIT $limit", arrayOf(match)
            ) {
                SearchHit(it.s("entity_type"), it.s("entity_id"), it.s("korean"),
                    it.s("romanization"), Localized(it.sn("en"), it.sn("ur"), it.sn("hi")))
            }
        }.getOrDefault(emptyList())
    }
}
