package com.github.meypod.al_azan.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class MaghribHijriDayShiftTest {

    private val maghrib = Instant.parse("2026-06-23T18:00:00Z")
    private val beforeMaghrib = maghrib - 1.hours
    private val afterMaghrib = maghrib + 1.hours

    @Test
    fun `disabled returns 0 even after maghrib`() {
        assertEquals(0, maghribHijriDayShift(afterMaghrib, maghrib, enabled = false))
    }

    @Test
    fun `null maghrib returns 0`() {
        assertEquals(0, maghribHijriDayShift(afterMaghrib, maghrib = null, enabled = true))
    }

    @Test
    fun `before maghrib returns 0`() {
        assertEquals(0, maghribHijriDayShift(beforeMaghrib, maghrib, enabled = true))
    }

    @Test
    fun `at maghrib returns 1`() {
        assertEquals(1, maghribHijriDayShift(maghrib, maghrib, enabled = true))
    }

    @Test
    fun `after maghrib returns 1`() {
        assertEquals(1, maghribHijriDayShift(afterMaghrib, maghrib, enabled = true))
    }
}
