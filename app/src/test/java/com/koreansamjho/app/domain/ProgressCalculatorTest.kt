package com.koreansamjho.app.domain

import com.koreansamjho.app.domain.engine.ProgressCalculator
import org.junit.Assert.*
import org.junit.Test

class ProgressCalculatorTest {

    private val today = 20_000L

    @Test
    fun `no activity means no streak`() {
        assertEquals(0, ProgressCalculator.currentStreak(emptyList(), today))
    }

    @Test
    fun `studying today counts as a streak of one`() {
        assertEquals(1, ProgressCalculator.currentStreak(listOf(today), today))
    }

    @Test
    fun `consecutive days accumulate`() {
        val days = listOf(today, today - 1, today - 2, today - 3)
        assertEquals(4, ProgressCalculator.currentStreak(days, today))
    }

    @Test
    fun `a streak survives until the day after the last study day`() {
        // Studied yesterday but not yet today — the streak should not be lost.
        val days = listOf(today - 1, today - 2)
        assertEquals(2, ProgressCalculator.currentStreak(days, today))
    }

    @Test
    fun `a gap of two days breaks the streak`() {
        val days = listOf(today - 2, today - 3)
        assertEquals(0, ProgressCalculator.currentStreak(days, today))
    }

    @Test
    fun `a gap in the middle only counts the recent run`() {
        val days = listOf(today, today - 1, today - 5, today - 6)
        assertEquals(2, ProgressCalculator.currentStreak(days, today))
    }

    @Test
    fun `longest streak finds the best historical run`() {
        val days = listOf(today, today - 1, today - 10, today - 11, today - 12, today - 13)
        assertEquals(4, ProgressCalculator.longestStreak(days))
    }

    @Test
    fun `duplicate days do not inflate a streak`() {
        val days = listOf(today, today, today - 1, today - 1)
        assertEquals(2, ProgressCalculator.currentStreak(days, today))
    }

    @Test
    fun `levels start at one and rise with xp`() {
        assertEquals(1, ProgressCalculator.levelForXp(0))
        assertEquals(1, ProgressCalculator.levelForXp(99))
        assertEquals(2, ProgressCalculator.levelForXp(100))
        assertTrue(ProgressCalculator.levelForXp(10_000) > ProgressCalculator.levelForXp(1_000))
    }

    @Test
    fun `xp into level is always below the requirement for the level`() {
        listOf(0, 50, 100, 275, 1000, 50_000).forEach { xp ->
            val (into, need) = ProgressCalculator.xpIntoLevel(xp)
            assertTrue("xp=$xp into=$into need=$need", into < need)
            assertTrue(into >= 0)
        }
    }

    @Test
    fun `accuracy handles the empty case without dividing by zero`() {
        assertEquals(0, ProgressCalculator.accuracyPercent(0, 0))
        assertEquals(100, ProgressCalculator.accuracyPercent(10, 10))
        assertEquals(50, ProgressCalculator.accuracyPercent(5, 10))
    }
}
