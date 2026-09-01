package com.koreansamjho.app.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LEARN = "learn"
    const val PRACTICE = "practice"
    const val TESTS = "tests"
    const val PROGRESS = "progress"

    const val COURSE = "course/{courseId}"
    fun course(id: String) = "course/$id"
    const val LESSON = "lesson/{lessonId}"
    fun lesson(id: String) = "lesson/$id"

    const val VOCAB_LIST = "vocab"
    const val VOCAB_CATEGORY = "vocab/{category}"
    fun vocabCategory(c: String) = "vocab/$c"
    const val VOCAB_DETAIL = "word/{id}"
    fun word(id: String) = "word/$id"

    const val GRAMMAR_LIST = "grammar"
    const val GRAMMAR_DETAIL = "grammar/{id}"
    fun grammar(id: String) = "grammar/$id"

    const val SENTENCE_LIST = "sentences"
    const val SENTENCE_SCENARIO = "sentences/{scenario}"
    fun scenario(s: String) = "sentences/$s"

    const val INTERVIEW = "interview"
    const val FAVOURITES = "favourites"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val EXAM_INFO = "exam_info"

    // quiz/{source}/{arg} — source: due | difficult | recent | favourites | lesson | category
    const val QUIZ = "quiz/{source}/{arg}"
    fun quiz(source: String, arg: String = "-") = "quiz/$source/$arg"

    // test/{kind}/{minutes}
    const val TEST = "test/{kind}/{minutes}"
    fun test(kind: String, minutes: Int) = "test/$kind/$minutes"

    const val TEST_RESULT = "result/{attemptId}"
    fun result(id: Long) = "result/$id"
    const val TEST_HISTORY = "history"
}
