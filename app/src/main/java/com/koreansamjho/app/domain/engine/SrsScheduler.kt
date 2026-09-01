package com.koreansamjho.app.domain.engine

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Spaced repetition scheduler.
 *
 * A Leitner box system with an SM-2 style ease modifier. Chosen over full SM-2
 * because the behaviour is explainable to a learner ("this word moves up a box")
 * and because it is trivially unit-testable with no floating-point drift in the
 * common path. Pure Kotlin, no Android dependency.
 */
object SrsScheduler {

    const val MAX_BOX = 5
    const val MIN_EASE = 1.3
    const val MAX_EASE = 2.6
    private const val DAY_MS = 86_400_000L

    /** Base interval in days for each box. Box 0 is "again this session". */
    fun baseIntervalDays(box: Int): Int = when (box.coerceIn(0, MAX_BOX)) {
        0 -> 0; 1 -> 1; 2 -> 3; 3 -> 7; 4 -> 16; else -> 35
    }

    data class State(
        val box: Int = 0,
        val ease: Double = 2.0,
        val dueAt: Long = 0L,
        val correctCount: Int = 0,
        val wrongCount: Int = 0,
        val lapses: Int = 0,
    )

    /**
     * @param confident true when the learner answered without hesitation; nudges ease up.
     * Returns the next state. A wrong answer always drops the item to box 0 so it is
     * seen again in the same session — forgetting is the signal the whole system exists for.
     */
    fun next(state: State, correct: Boolean, confident: Boolean = true, now: Long): State {
        if (!correct) {
            val ease = max(MIN_EASE, state.ease - 0.20)
            return state.copy(
                box = 0, ease = ease, dueAt = now,
                wrongCount = state.wrongCount + 1, lapses = state.lapses + 1
            )
        }
        val box = min(MAX_BOX, state.box + 1)
        val ease = min(MAX_EASE, state.ease + if (confident) 0.05 else -0.05)
        val days = baseIntervalDays(box)
        val due = if (days == 0) now else now + (days * ease * DAY_MS).roundToLong()
        return state.copy(box = box, ease = ease, dueAt = due, correctCount = state.correctCount + 1)
    }

    /** True when the item has been answered correctly enough times to count as learned. */
    fun isMastered(state: State) = state.box >= 3
}
