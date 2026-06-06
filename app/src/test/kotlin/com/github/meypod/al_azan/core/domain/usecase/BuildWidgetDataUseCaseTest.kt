package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.WidgetCityNamePos
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.data.DateComponents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.time.Instant

class BuildWidgetDataUseCaseTest {

    private val hour = 3_600_000L
    private val base = 1_700_000_000_000L
    private fun at(h: Double) = Instant.fromEpochMilliseconds(base + (h * hour).toLong())

    private val shariaTimes = ShariaTimes(
        forInstant = at(0.0),
        forDate = DateComponents(2026, 1, 1),
        fajr = at(5.0),
        sunrise = at(6.0),
        dhuhr = at(12.0),
        asr = at(15.0),
        sunset = at(18.0),
        maghrib = at(18.2),
        isha = at(20.0),
        midnight = at(24.5),
        tahajjud = at(27.0),
    )

    private val location = CalculationLocationDetail(lat = 0.0, long = 0.0, label = "Testville")

    private val params = CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters

    private val nextDayMillis = at(28.0).toEpochMilliseconds()

    /** Deterministic formatter: no ICU. Dates render as "date:<calendar>", times as "t<millis>". */
    private class FakeFormatter(
        private val nextDay: Long,
    ) : WidgetFormatter {
        override fun formatPrayerTime(
            instant: Instant,
            is24Hour: Boolean,
            numberingSystem: NumberingSystem,
            locale: String,
        ) = "t${instant.toEpochMilliseconds()}"

        override fun formatDate(
            instant: Instant,
            locale: String,
            calendar: String,
            numberingSystem: NumberingSystem,
        ) = "date:$calendar"

        override fun adjustDays(
            instant: Instant,
            days: Int,
        ) = instant

        override fun nextDayBeginningMillis(instant: Instant) = nextDay
    }

    private fun details(
        prayer: Prayer,
        time: Instant,
    ) = ShariaTimeDetails(
        forInstant = time,
        forDate = DateComponents(2026, 1, 1),
        prayer = prayer,
        prayerTime = time,
        notify = false,
        sound = false,
    )

    private fun useCase(
        next: ShariaTimeDetails? = details(Prayer.Asr, at(15.0)),
        nextDay: Long = nextDayMillis,
    ): BuildWidgetDataUseCase {
        val getShariaTimes = mock<GetShariaTimesUseCase> {
            on { invoke(any(), any(), any(), any(), any()) } doReturn shariaTimes
        }
        val getNext = mock<GetNextShariaTimesUseCase> {
            on { invoke(any(), any(), any(), any(), any(), anyOrNull(), any()) } doReturn next
        }
        return BuildWidgetDataUseCase(getShariaTimes, getNext, FakeFormatter(nextDay))
    }

    private fun settings(
        hidden: List<Prayer> = emptyList(),
        highlightCurrent: Boolean = false,
        showCountdown: Boolean = false,
        showWidget: Boolean = false,
        cityNamePos: WidgetCityNamePos = WidgetCityNamePos.None,
    ) = Settings(
        selectedLocale = "en",
        selectedArabicCalendar = "islamic",
        selectedSecondaryCalendar = SecondaryCalendar.Gregorian,
        hiddenWidgetPrayers = hidden,
        highlightCurrentPrayer = highlightCurrent,
        showWidgetCountdown = showCountdown,
        showWidget = showWidget,
        widgetCityNamePos = cityNamePos,
    )

    private fun calc() = CalculationSettings(parameters = params)

    @Test
    fun `returns null when parameters are missing`() {
        val result = useCase().invoke(at(9.0), settings(), CalculationSettings(parameters = null), location)
        assertNull(result)
    }

    @Test
    fun `returns null when location is missing`() {
        val result = useCase().invoke(at(9.0), settings(), calc(), null)
        assertNull(result)
    }

    @Test
    fun `rows exclude hidden prayers and keep canonical order`() {
        val hidden = listOf(Prayer.Sunrise, Prayer.Sunset, Prayer.Midnight, Prayer.Tahajjud)
        val result = useCase().invoke(at(9.0), settings(hidden = hidden), calc(), location)!!
        assertEquals(
            listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha),
            result.rows.map { it.prayer },
        )
    }

    @Test
    fun `highlightCurrentPrayer marks the current prayer active`() {
        // at 13:00 the most recent passed time is Dhuhr (12:00).
        val result = useCase().invoke(at(13.0), settings(highlightCurrent = true), calc(), location)!!
        assertTrue(result.rows.single { it.prayer == Prayer.Dhuhr }.isActive)
        assertFalse(result.rows.single { it.prayer == Prayer.Asr }.isActive)
    }

    @Test
    fun `highlightCurrentPrayer highlights a visible current prayer`() {
        // at 21:00 the most recent passed prayer is Isha (visible).
        val hidden = listOf(Prayer.Sunset, Prayer.Midnight, Prayer.Tahajjud)
        val result = useCase().invoke(at(21.0), settings(hidden = hidden, highlightCurrent = true), calc(), location)!!
        assertTrue(result.rows.single { it.prayer == Prayer.Isha }.isActive)
    }

    @Test
    fun `highlightCurrentPrayer stops highlighting once a hidden prayer becomes current`() {
        // at 25:00 the most recent passed prayer is the hidden Midnight (24.5), so no visible row
        // highlights — even though the countdown still targets the next visible prayer.
        val hidden = listOf(Prayer.Sunset, Prayer.Midnight, Prayer.Tahajjud)
        val result = useCase().invoke(at(25.0), settings(hidden = hidden, highlightCurrent = true), calc(), location)!!
        assertTrue(result.rows.none { it.isActive })
    }

    @Test
    fun `without highlightCurrentPrayer the next prayer is active`() {
        val result = useCase(next = details(Prayer.Asr, at(15.0)))
            .invoke(at(13.0), settings(highlightCurrent = false), calc(), location)!!
        assertTrue(result.rows.single { it.prayer == Prayer.Asr }.isActive)
        assertFalse(result.rows.single { it.prayer == Prayer.Dhuhr }.isActive)
    }

    @Test
    fun `city name pos None uses both calendar dates`() {
        val result = useCase().invoke(at(9.0), settings(cityNamePos = WidgetCityNamePos.None), calc(), location)!!
        assertEquals("date:islamic", result.topStartText)
        assertEquals("date:gregorian", result.topEndText)
    }

    @Test
    fun `city name pos TopStart replaces the start with the city`() {
        val result = useCase().invoke(at(9.0), settings(cityNamePos = WidgetCityNamePos.TopStart), calc(), location)!!
        assertEquals("Testville", result.topStartText)
        assertEquals("date:gregorian", result.topEndText)
    }

    @Test
    fun `city name pos TopEnd replaces the end with the city`() {
        val result = useCase().invoke(at(9.0), settings(cityNamePos = WidgetCityNamePos.TopEnd), calc(), location)!!
        assertEquals("date:islamic", result.topStartText)
        assertEquals("Testville", result.topEndText)
    }

    @Test
    fun `countdown targets the next prayer when enabled`() {
        val result = useCase(next = details(Prayer.Asr, at(15.0)))
            .invoke(at(9.0), settings(showCountdown = true), calc(), location)!!
        assertEquals(Prayer.Asr, result.countdown?.prayer)
        assertEquals(at(15.0).toEpochMilliseconds(), result.countdown?.baseMillis)
    }

    @Test
    fun `countdown is null when disabled`() {
        val result = useCase().invoke(at(9.0), settings(showCountdown = false), calc(), location)!!
        assertNull(result.countdown)
    }

    @Test
    fun `countdown is null when there is no next prayer even if enabled`() {
        val result = useCase(next = null).invoke(at(9.0), settings(showCountdown = true), calc(), location)!!
        assertNull(result.countdown)
    }

    @Test
    fun `nextUpdate is the next prayer when it precedes the day rollover`() {
        val result = useCase(next = details(Prayer.Asr, at(15.0)), nextDay = at(28.0).toEpochMilliseconds())
            .invoke(at(9.0), settings(), calc(), location)!!
        assertEquals(at(15.0).toEpochMilliseconds(), result.nextUpdateAtMillis)
    }

    @Test
    fun `nextUpdate is the day rollover when it precedes the next prayer`() {
        val result = useCase(next = details(Prayer.Tahajjud, at(27.0)), nextDay = at(24.0).toEpochMilliseconds())
            .invoke(at(20.0), settings(), calc(), location)!!
        assertEquals(at(24.0).toEpochMilliseconds(), result.nextUpdateAtMillis)
    }

    @Test
    fun `showNotification reflects the showWidget setting`() {
        assertTrue(useCase().invoke(at(9.0), settings(showWidget = true), calc(), location)!!.showNotification)
        assertFalse(useCase().invoke(at(9.0), settings(showWidget = false), calc(), location)!!.showNotification)
    }
}
