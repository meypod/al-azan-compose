package com.github.meypod.al_azan.main.home.components

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.usecase.ShariaTimeDetails
import io.github.meypod.adhan_kotlin.data.DateComponents
import junit.framework.TestCase.assertEquals
import org.junit.Test
import kotlin.time.Instant

class ShariaTimesBoxHighlightTest {

    private val dayMillis = 86_400_000L
    private val day0 = Instant.fromEpochMilliseconds(1_700_000_000_000L)
    private val day1 = Instant.fromEpochMilliseconds(1_700_000_000_000L + dayMillis)
    private val day2 = Instant.fromEpochMilliseconds(1_700_000_000_000L + 2 * dayMillis)

    private fun shariaTimesOn(instant: Instant, date: DateComponents) =
        ShariaTimes(
            forInstant = instant,
            forDate = date,
            fajr = instant,
            sunrise = instant,
            dhuhr = instant,
            asr = instant,
            sunset = instant,
            maghrib = instant,
            isha = instant,
            midnight = instant,
            tahajjud = instant,
        )

    private fun highlight(prayer: Prayer, instant: Instant, date: DateComponents) =
        ShariaTimeDetails(
            forInstant = instant,
            forDate = date,
            prayer = prayer,
            prayerTime = instant,
            notify = false,
            sound = false,
        )

    private val dc1 = DateComponents(2026, 1, 1)
    private val dc2 = DateComponents(2026, 1, 2)
    private val dc0 = DateComponents(2025, 12, 31)

    // --- same day: pivot on the highlighted prayer ---

    @Test
    fun `prayer before highlighted is BeforeHighlight`() {
        val state = getHighlightState(
            Prayer.Fajr,
            shariaTimesOn(day1, dc1),
            highlight(Prayer.Asr, day1, dc1),
        )
        assertEquals(HighlightState.BeforeHighlight, state)
    }

    @Test
    fun `the highlighted prayer is Highlighted`() {
        val state = getHighlightState(
            Prayer.Asr,
            shariaTimesOn(day1, dc1),
            highlight(Prayer.Asr, day1, dc1),
        )
        assertEquals(HighlightState.Highlighted, state)
    }

    @Test
    fun `prayer after highlighted is AfterHighlight`() {
        val state = getHighlightState(
            Prayer.Isha,
            shariaTimesOn(day1, dc1),
            highlight(Prayer.Asr, day1, dc1),
        )
        assertEquals(HighlightState.AfterHighlight, state)
    }

    // --- cross-day ---

    @Test
    fun `viewing a past day greys all rows`() {
        // highlighted is on day1, viewing day0 -> everything already passed
        val state = getHighlightState(
            Prayer.Isha,
            shariaTimesOn(day0, dc0),
            highlight(Prayer.Asr, day1, dc1),
        )
        assertEquals(HighlightState.BeforeHighlight, state)
    }

    @Test
    fun `viewing a future day marks all rows AfterHighlight`() {
        val state = getHighlightState(
            Prayer.Fajr,
            shariaTimesOn(day2, dc2),
            highlight(Prayer.Asr, day1, dc1),
        )
        assertEquals(HighlightState.AfterHighlight, state)
    }

    // --- null inputs default to AfterHighlight (no highlight) ---

    @Test
    fun `null sharia times defaults to AfterHighlight`() {
        val state = getHighlightState(
            Prayer.Asr,
            null,
            highlight(Prayer.Asr, day1, dc1),
        )
        assertEquals(HighlightState.AfterHighlight, state)
    }

    @Test
    fun `null highlighted time defaults to AfterHighlight`() {
        val state = getHighlightState(
            Prayer.Asr,
            shariaTimesOn(day1, dc1),
            null,
        )
        assertEquals(HighlightState.AfterHighlight, state)
    }
}
