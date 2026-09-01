package com.koreansamjho.app.domain

import com.koreansamjho.app.domain.engine.SrsScheduler
import com.koreansamjho.app.domain.engine.SrsScheduler.State
import org.junit.Assert.*
import org.junit.Test

class SrsSchedulerTest {

    private val now = 1_700_000_000_000L
    private val day = 86_400_000L

    @Test
    fun `a correct answer moves the item up one box`() {
        val next = SrsScheduler.next(State(box = 0), correct = true, now = now)
        assertEquals(1, next.box)
        assertEquals(1, next.correctCount)
    }

    @Test
    fun `a wrong answer drops the item to box zero and makes it due immediately`() {
        val learned = State(box = 4, ease = 2.4, dueAt = now + 30 * day)
        val next = SrsScheduler.next(learned, correct = false, now = now)
        assertEquals(0, next.box)
        assertEquals(now, next.dueAt)
        assertEquals(1, next.lapses)
        assertEquals(1, next.wrongCount)
    }

    @Test
    fun `ease never falls below the floor no matter how many lapses`() {
        var s = State()
        repeat(50) { s = SrsScheduler.next(s, correct = false, now = now) }
        assertTrue("ease was ${s.ease}", s.ease >= SrsScheduler.MIN_EASE)
    }

    @Test
    fun `ease never rises above the ceiling`() {
        var s = State()
        repeat(50) { s = SrsScheduler.next(s, correct = true, confident = true, now = now) }
        assertTrue("ease was ${s.ease}", s.ease <= SrsScheduler.MAX_EASE)
    }

    @Test
    fun `box is capped at the maximum`() {
        var s = State()
        repeat(20) { s = SrsScheduler.next(s, correct = true, now = now) }
        assertEquals(SrsScheduler.MAX_BOX, s.box)
    }

    @Test
    fun `intervals grow strictly as the box rises`() {
        val intervals = (0..SrsScheduler.MAX_BOX).map { SrsScheduler.baseIntervalDays(it) }
        assertEquals(intervals.sorted(), intervals)
        assertEquals(0, intervals.first())
        assertTrue(intervals.last() > intervals[1])
    }

    @Test
    fun `box one schedules roughly one day out, not the same session`() {
        val next = SrsScheduler.next(State(box = 0), correct = true, now = now)
        assertTrue("due was ${next.dueAt - now}", next.dueAt - now >= day)
    }

    @Test
    fun `an unconfident correct answer lowers ease relative to a confident one`() {
        val confident = SrsScheduler.next(State(box = 1), true, confident = true, now = now)
        val hesitant = SrsScheduler.next(State(box = 1), true, confident = false, now = now)
        assertTrue(hesitant.ease < confident.ease)
        assertTrue("hesitant should still be due later", hesitant.dueAt < confident.dueAt)
    }

    @Test
    fun `mastery requires several successes, not one`() {
        var s = State()
        s = SrsScheduler.next(s, true, now = now)
        assertFalse(SrsScheduler.isMastered(s))
        s = SrsScheduler.next(s, true, now = now)
        s = SrsScheduler.next(s, true, now = now)
        assertTrue(SrsScheduler.isMastered(s))
    }
}
