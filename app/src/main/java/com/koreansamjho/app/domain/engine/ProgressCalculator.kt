package com.koreansamjho.app.domain.engine

/** Pure functions for streaks, levels and XP. No Android dependency, fully unit-tested. */
object ProgressCalculator {

    /**
     * Current streak in days. [activeDays] are epoch-day numbers, any order.
     * A streak survives until the day *after* the last active day has passed, so a
     * learner who has not studied yet today has not lost their streak.
     */
    fun currentStreak(activeDays: Collection<Long>, today: Long): Int {
        if (activeDays.isEmpty()) return 0
        val days = activeDays.toHashSet()
        val start = when {
            days.contains(today) -> today
            days.contains(today - 1) -> today - 1
            else -> return 0
        }
        var streak = 0
        var d = start
        while (days.contains(d)) { streak++; d-- }
        return streak
    }

    fun longestStreak(activeDays: Collection<Long>): Int {
        if (activeDays.isEmpty()) return 0
        val sorted = activeDays.toSortedSet().toList()
        var best = 1; var run = 1
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1] + 1) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /** XP thresholds grow gently so early progress feels fast and later levels still mean something. */
    fun levelForXp(xp: Int): Int {
        var level = 1; var need = 100; var remaining = xp
        while (remaining >= need && level < 99) { remaining -= need; level++; need = (need * 1.25).toInt() }
        return level
    }

    fun xpIntoLevel(xp: Int): Pair<Int, Int> {
        var level = 1; var need = 100; var remaining = xp
        while (remaining >= need && level < 99) { remaining -= need; level++; need = (need * 1.25).toInt() }
        return remaining to need
    }

    fun accuracyPercent(correct: Int, total: Int): Int =
        if (total <= 0) 0 else ((correct.toDouble() / total) * 100).toInt()
}
