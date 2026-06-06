package com.github.meypod.al_azan.core.domain.model.adhan

import io.github.meypod.adhan_kotlin.data.DateComponents
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test
import kotlin.time.Instant

class ShariaTimesTest {

    private val hour = 3_600_000L
    private val base = 1_700_000_000_000L // arbitrary day start

    /** Build ShariaTimes with each time at the given hour offset from [base]. */
    private fun times(
        fajr: Double = 5.0,
        sunrise: Double = 6.0,
        dhuhr: Double = 12.0,
        asr: Double = 15.0,
        sunset: Double = 18.0,
        maghrib: Double = 18.2,
        isha: Double = 20.0,
        midnight: Double = 24.5,
        tahajjud: Double = 27.0,
    ): ShariaTimes {
        fun at(h: Double) = Instant.fromEpochMilliseconds(base + (h * hour).toLong())
        return ShariaTimes(
            forInstant = at(0.0),
            forDate = DateComponents(2026, 1, 1),
            fajr = at(fajr),
            sunrise = at(sunrise),
            dhuhr = at(dhuhr),
            asr = at(asr),
            sunset = at(sunset),
            maghrib = at(maghrib),
            isha = at(isha),
            midnight = at(midnight),
            tahajjud = at(tahajjud),
        )
    }

    private fun at(h: Double) = Instant.fromEpochMilliseconds(base + (h * hour).toLong())

    // --- currentPrayer: latest sharia time whose moment has passed ---

    @Test
    fun `currentPrayer is null before first time of day`() {
        assertNull(times().currentPrayer(at(4.0)))
    }

    @Test
    fun `currentPrayer is inclusive at the exact time`() {
        assertEquals(Prayer.Fajr, times().currentPrayer(at(5.0)))
    }

    @Test
    fun `currentPrayer returns the most recent passed time`() {
        assertEquals(Prayer.Dhuhr, times().currentPrayer(at(13.0)))
        assertEquals(Prayer.Sunrise, times().currentPrayer(at(6.5)))
    }

    @Test
    fun `currentPrayer returns last time after all have passed`() {
        assertEquals(Prayer.Tahajjud, times().currentPrayer(at(28.0)))
    }

    // --- nextPrayer: earliest sharia time not yet passed ---

    @Test
    fun `nextPrayer is Fajr before the day starts`() {
        assertEquals(Prayer.Fajr, times().nextPrayer(at(4.0)))
    }

    @Test
    fun `nextPrayer is inclusive at the exact time`() {
        assertEquals(Prayer.Fajr, times().nextPrayer(at(5.0)))
    }

    @Test
    fun `nextPrayer returns the upcoming time`() {
        assertEquals(Prayer.Asr, times().nextPrayer(at(13.0)))
    }

    @Test
    fun `nextPrayer is null after the last time`() {
        assertNull(times().nextPrayer(at(28.0)))
    }

    // --- excluding (hidden prayers) skips to the next/previous visible one ---

    @Test
    fun `nextPrayer skips excluded prayers`() {
        // at 13:00 next is Asr, but Asr hidden -> Sunset
        assertEquals(
            Prayer.Sunset,
            times().nextPrayer(at(13.0), excluding = setOf(Prayer.Asr)),
        )
    }

    @Test
    fun `currentPrayer skips excluded prayers`() {
        // at 18:30 current is Sunset/Maghrib region; hide Maghrib and Sunset -> Asr
        assertEquals(
            Prayer.Asr,
            times().currentPrayer(at(18.5), excluding = setOf(Prayer.Sunset, Prayer.Maghrib)),
        )
    }
}
