package com.koreansamjho.app.domain.model

/** The learner's chosen support language. Korean is always the subject, never the support. */
enum class Lang(val code: String) { EN("en"), UR("ur"), HI("hi");
    val isRtl: Boolean get() = this == UR
    companion object { fun from(code: String?) = entries.firstOrNull { it.code == code } ?: EN }
}

/**
 * Goal track. Phase 1 research: Pakistan is an EPS partner country and India is not,
 * so the exam layer must differ by learner goal. See docs/01-research.md 1.1.
 */
enum class Track { EPS_EMPLOYMENT, TOPIK_ACADEMIC, GENERAL;
    companion object { fun from(v: String?) = entries.firstOrNull { it.name == v } ?: GENERAL }
}

enum class Country { PAKISTAN, INDIA, OTHER;
    companion object { fun from(v: String?) = entries.firstOrNull { it.name == v } ?: OTHER }
}

/** A string carried in all three support languages. */
data class Localized(val en: String?, val ur: String?, val hi: String?) {
    operator fun get(lang: Lang): String = when (lang) {
        Lang.EN -> en; Lang.UR -> ur ?: en; Lang.HI -> hi ?: en
    }.orEmpty()
    fun isBlank() = en.isNullOrBlank() && ur.isNullOrBlank() && hi.isNullOrBlank()
}

enum class ReviewStatus { DRAFT, REVIEWED_KO, REVIEWED_FULL;
    companion object { fun from(v: String?) = when (v) {
        "reviewed_full" -> REVIEWED_FULL; "reviewed_ko" -> REVIEWED_KO; else -> DRAFT } }
}

data class Letter(
    val id: String, val char: String, val kind: String, val order: Int,
    val romanization: String, val nameKo: String, val nameRr: String,
    val initialSound: String?, val finalSound: String?, val sound: Localized,
)

data class Vocab(
    val id: String, val korean: String, val romanization: String, val pos: String,
    val level: Int, val category: String, val meaning: Localized,
    val exampleKorean: String, val exampleRomanization: String, val exampleTranslation: Localized,
    val reviewStatus: ReviewStatus,
)

data class GrammarExample(
    val korean: String, val romanization: String, val translation: Localized,
)

data class Grammar(
    val id: String, val order: Int, val pattern: String, val titleEn: String,
    val level: Int, val formality: String, val structure: String,
    val explanation: Localized, val commonMistake: Localized,
    val examples: List<GrammarExample>,
)

data class Sentence(
    val id: String, val scenario: String, val order: Int, val level: Int,
    val korean: String, val romanization: String, val translation: Localized,
)

data class Scenario(val id: String, val title: Localized, val level: Int, val count: Int)

data class InterviewItem(
    val id: String, val order: Int, val category: String,
    val questionKorean: String, val questionRomanization: String, val question: Localized,
    val answerKorean: String, val answerRomanization: String, val answer: Localized,
    val tipEn: String,
)

data class ExamSection(val name: String, val questions: Int, val points: Int, val minutes: Int?)

/** Exam facts. [confidence] and [caution] exist so the app never states an unverified claim flatly. */
data class ExamInfo(
    val id: String, val code: String, val track: String, val name: Localized, val who: Localized,
    val sections: List<ExamSection>, val totalQuestions: Int, val totalPoints: Int,
    val delivery: String, val scoring: Localized, val confidence: String,
    val officialUrl: String, val caution: Localized?,
)

data class Course(
    val id: String, val track: String, val level: Int, val order: Int,
    val title: Localized, val subtitle: Localized,
)

enum class LessonKind { HANGUL, SYLLABLE, BATCHIM, VOCAB, GRAMMAR, SENTENCE, INTERVIEW;
    companion object { fun from(v: String) = when (v) {
        "hangul" -> HANGUL; "syllable" -> SYLLABLE; "batchim" -> BATCHIM
        "grammar" -> GRAMMAR; "sentence" -> SENTENCE; "interview" -> INTERVIEW; else -> VOCAB } }
}

data class Lesson(
    val id: String, val courseId: String, val order: Int, val title: Localized,
    val kind: LessonKind, val selector: String, val level: Int,
)

data class QuestionOption(val korean: String?, val text: Localized)

enum class QuestionKind { VOCAB_KO_TO_MEANING, VOCAB_MEANING_TO_KO, LISTENING, GRAMMAR, READING;
    companion object { fun from(v: String) = when (v) {
        "vocab_meaning_to_ko" -> VOCAB_MEANING_TO_KO; "listening" -> LISTENING
        "grammar" -> GRAMMAR; "reading" -> READING; else -> VOCAB_KO_TO_MEANING } }
}

data class Question(
    val id: String, val kind: QuestionKind, val level: Int, val track: String, val category: String,
    val promptKorean: String?, val promptRomanization: String?, val prompt: Localized,
    val passageId: String?, val options: List<QuestionOption>, val correctIndex: Int,
    val explanation: Localized, val audioText: String?,
)

data class Passage(val id: String, val level: Int, val korean: String, val romanization: String)

data class SearchHit(
    val entityType: String, val entityId: String,
    val korean: String, val romanization: String, val translation: Localized,
)

data class SourceRef(
    val id: String, val title: String, val publisher: String,
    val url: String, val licence: String, val note: String,
)
