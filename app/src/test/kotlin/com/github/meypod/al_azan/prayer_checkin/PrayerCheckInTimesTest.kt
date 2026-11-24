package com.github.meypod.al_azan.prayer_checkin

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import io.github.meypod.adhan_kotlin.data.DateComponents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Instant

class PrayerCheckInTimesTest {

    private val hour = 3_600_000L
    private val base = 1_700_000_000_000L

    private fun at(h: Double) = Instant.fromEpochMilliseconds(base + (h * hour).toLong())

    private fun sampleTimes() = ShariaTimes(
        forInstant = at(0.0),
        forDate = DateComponents(2026, 11, 23),
        fajr = at(5.0),
        sunrise = at(6.5),
        dhuhr = at(12.0),
        asr = at(15.0),
        sunset = at(17.5),
        maghrib = at(17.7),
        isha = at(19.2),
        midnight = at(23.8),
        tahajjud = at(26.5),
    )

    @Test
    fun `obligatory prayers map to expected start and end boundaries`() {
        val times = sampleTimes()
        assertEquals(PrayerWindow(times.fajr, times.sunrise), prayerWindow(Prayer.Fajr, times))
        assertEquals(PrayerWindow(times.dhuhr, times.asr), prayerWindow(Prayer.Dhuhr, times))
        assertEquals(PrayerWindow(times.asr, times.maghrib), prayerWindow(Prayer.Asr, times))
        assertEquals(PrayerWindow(times.maghrib, times.isha), prayerWindow(Prayer.Maghrib, times))
        assertEquals(PrayerWindow(times.isha, times.midnight), prayerWindow(Prayer.Isha, times))
    }

    @Test
    fun `non-obligatory times return null window`() {
        val times = sampleTimes()
        assertNull(prayerWindow(Prayer.Sunrise, times))
        assertNull(prayerWindow(Prayer.Sunset, times))
        assertNull(prayerWindow(Prayer.Midnight, times))
        assertNull(prayerWindow(Prayer.Tahajjud, times))
    }
}
