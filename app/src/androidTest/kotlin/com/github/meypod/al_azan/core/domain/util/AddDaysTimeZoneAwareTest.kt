package com.github.meypod.al_azan.core.domain.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone
import kotlin.time.Instant

/**
 * Regression test for [addDaysTimeZoneAware]: a 25-hour fall-back DST day made local midnight + 24h
 * land on the *same* calendar date, which previously spun forever (the nudge loop reassigned from the
 * original instant instead of accumulating). The [Test.timeout] guards against that hang.
 */
@RunWith(AndroidJUnit4::class)
class AddDaysTimeZoneAwareTest {

    private lateinit var original: TimeZone

    @Before
    fun setup() {
        original = TimeZone.getDefault()
    }

    @After
    fun restore() {
        TimeZone.setDefault(original)
    }

    @Test(timeout = 5_000)
    fun advancesAcrossFallBackMidnightDstDay() {
        // Havana falls back on 2018-11-04 (01:00 -> 00:00), making that local day 25 hours long, so
        // local midnight + 24h is still 2018-11-04 — the case that used to loop forever.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Havana"))
        val dayStart = getDayBeginning(Instant.parse("2018-11-04T16:00:00Z"))
        assertEquals(LocalDate(2018, 11, 4), dayStart.toLocalDate())
        assertEquals(LocalDate(2018, 11, 5), addDaysTimeZoneAware(dayStart, 1).toLocalDate())
        assertEquals(LocalDate(2018, 11, 3), addDaysTimeZoneAware(dayStart, -1).toLocalDate())
    }

    @Test(timeout = 5_000)
    fun advancesNormalDays() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        val dayStart = getDayBeginning(Instant.parse("2026-06-15T10:00:00Z"))
        assertEquals(LocalDate(2026, 6, 16), addDaysTimeZoneAware(dayStart, 1).toLocalDate())
        assertEquals(LocalDate(2026, 6, 10), addDaysTimeZoneAware(dayStart, -5).toLocalDate())
    }
}
