package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimesResult
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.settings.NumberingSystem
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetConfig
import com.github.meypod.al_azan.core.domain.model.widget.DateCalendar
import com.github.meypod.al_azan.core.domain.model.widget.HeaderBlock
import com.github.meypod.al_azan.core.domain.model.widget.HeaderDateFormat
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.data.DateComponents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.time.Instant

class BuildCustomWidgetDataUseCaseTest {

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

    /** The following day's times, served for [tomorrowInstant] so tests can tell which day a row shows. */
    private val tomorrowInstant = at(29.0)
    private val tomorrowTimes = ShariaTimes(
        forInstant = tomorrowInstant,
        forDate = DateComponents(2026, 1, 2),
        fajr = at(29.0),
        sunrise = at(30.0),
        dhuhr = at(36.0),
        asr = at(39.0),
        sunset = at(42.0),
        maghrib = at(42.2),
        isha = at(44.0),
        midnight = at(48.5),
        tahajjud = at(51.0),
    )
    private val tomorrowFajr = ShariaTimeDetails(
        forInstant = tomorrowInstant,
        forDate = DateComponents(2026, 1, 2),
        prayer = Prayer.Fajr,
        prayerTime = at(29.0),
        notify = false,
        sound = false,
    )

    private val location = CalculationLocationDetail(lat = 0.0, long = 0.0, label = "Testville")
    private val params = CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters
    private val nextDayMillis = at(28.0).toEpochMilliseconds()

    /** Deterministic formatter: dates render "date:<cal>" or "day:<cal>" (with weekday), times "t<millis>". */
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
            withDayName: Boolean,
            format: HeaderDateFormat,
        ) = (if (withDayName) "day:$calendar" else "date:$calendar") +
            if (format == HeaderDateFormat.Full) "" else ":${format.name}"

        override fun adjustDays(
            instant: Instant,
            days: Int,
        ) = instant

        override fun isSameDay(
            a: Instant,
            b: Instant,
        ) = a.toEpochMilliseconds().floorDiv(DAY_MILLIS) == b.toEpochMilliseconds().floorDiv(DAY_MILLIS)

        override fun nextDayBeginningMillis(instant: Instant) = nextDay

        private companion object {
            const val DAY_MILLIS = 86_400_000L
        }
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
    ): BuildCustomWidgetDataUseCase {
        val getShariaTimes = mock<GetShariaTimesUseCase> {
            on { invoke(any(), any(), any(), any(), any()) } doReturn ShariaTimesResult.Available(shariaTimes)
            on { invoke(eq(tomorrowInstant), any(), any(), any(), any()) } doReturn ShariaTimesResult.Available(tomorrowTimes)
        }
        val getNext = mock<GetNextShariaTimesUseCase> {
            on { invoke(any(), any(), any(), any(), any(), anyOrNull(), any(), any()) } doReturn next
        }
        return BuildCustomWidgetDataUseCase(getShariaTimes, getNext, FakeFormatter(nextDay))
    }

    private fun settings(
        highlightCurrent: Boolean = false,
        lunarDayAtMaghrib: Boolean = false,
    ) = Settings(
        selectedLocale = "en",
        selectedArabicCalendar = "islamic",
        highlightCurrentPrayerWidget = highlightCurrent,
        widgetHijriDayStartsAtMaghrib = lunarDayAtMaghrib,
    )

    private fun calc() = CalculationSettings(parameters = params)

    @Test
    fun `returns null when parameters are missing`() {
        assertNull(
            useCase().invoke(at(9.0), settings(), CalculationSettings(parameters = null), location, CustomWidgetConfig()),
        )
    }

    @Test
    fun `returns null when location is missing`() {
        assertNull(useCase().invoke(at(9.0), settings(), calc(), null, CustomWidgetConfig()))
    }

    @Test
    fun `prayer cells keep the config order and one row by default`() {
        val config = CustomWidgetConfig(rows = listOf(listOf(Prayer.Maghrib, Prayer.Fajr, Prayer.Isha)))
        val result = useCase().invoke(at(9.0), settings(), calc(), location, config)!!
        assertEquals(1, result.prayerRows.size)
        assertEquals(listOf(Prayer.Maghrib, Prayer.Fajr, Prayer.Isha), result.prayerRows[0].map { it.prayer })
    }

    @Test
    fun `two rows split the prayers evenly preserving order`() {
        val config = CustomWidgetConfig(
            rows = listOf(
                listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr),
                listOf(Prayer.Maghrib, Prayer.Isha),
            ),
        )
        val result = useCase().invoke(at(9.0), settings(), calc(), location, config)!!
        assertEquals(2, result.prayerRows.size)
        assertEquals(listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr), result.prayerRows[0].map { it.prayer })
        assertEquals(listOf(Prayer.Maghrib, Prayer.Isha), result.prayerRows[1].map { it.prayer })
    }

    @Test
    fun `prayer rows are empty when no prayers are placed`() {
        val result = useCase().invoke(at(9.0), settings(), calc(), location, CustomWidgetConfig(rows = emptyList()))!!
        assertTrue(result.prayerRows.isEmpty())
    }

    @Test
    fun `next prayer is highlighted when it is on the displayed day`() {
        val config = CustomWidgetConfig(rows = listOf(listOf(Prayer.Dhuhr, Prayer.Asr)))
        val result = useCase(next = details(Prayer.Asr, at(15.0)))
            .invoke(at(13.0), settings(highlightCurrent = false), calc(), location, config)!!
        val cells = result.prayerRows.flatten()
        assertTrue(cells.single { it.prayer == Prayer.Asr }.isActive)
        assertTrue(cells.none { it.prayer == Prayer.Dhuhr && it.isActive })
    }

    @Test
    fun `after isha with lunar day at maghrib the table shows tomorrow and highlights fajr`() {
        val config = CustomWidgetConfig(rows = listOf(listOf(Prayer.Fajr, Prayer.Isha)))
        val result = useCase(next = tomorrowFajr)
            .invoke(at(20.5), settings(lunarDayAtMaghrib = true), calc(), location, config)!!
        val fajr = result.prayerRows.flatten().single { it.prayer == Prayer.Fajr }
        assertTrue(fajr.isActive)
        assertEquals("t${tomorrowTimes.fajr.toEpochMilliseconds()}", fajr.timeText)
    }

    @Test
    fun `after isha without lunar day at maghrib the table keeps today and highlights nothing`() {
        val config = CustomWidgetConfig(rows = listOf(listOf(Prayer.Fajr, Prayer.Isha)))
        val result = useCase(next = tomorrowFajr)
            .invoke(at(20.5), settings(), calc(), location, config)!!
        val cells = result.prayerRows.flatten()
        assertTrue(cells.none { it.isActive })
        assertEquals("t${shariaTimes.fajr.toEpochMilliseconds()}", cells.single { it.prayer == Prayer.Fajr }.timeText)
    }

    @Test
    fun `current prayer is highlighted when the user opts in`() {
        // at 13:00 the most recent passed prayer is Dhuhr (12:00).
        val config = CustomWidgetConfig(rows = listOf(listOf(Prayer.Dhuhr, Prayer.Asr)))
        val result = useCase().invoke(at(13.0), settings(highlightCurrent = true), calc(), location, config)!!
        assertTrue(result.prayerRows.flatten().single { it.prayer == Prayer.Dhuhr }.isActive)
    }

    @Test
    fun `header slots resolve date, weekday date and location name`() {
        val config = CustomWidgetConfig(
            topStart = HeaderBlock.Date(calendar = DateCalendar.Hijri, withDayName = false),
            topEnd = HeaderBlock.Date(calendar = DateCalendar.Gregorian, withDayName = true),
        )
        val result = useCase().invoke(at(9.0), settings(), calc(), location, config)!!
        assertEquals("date:islamic", result.topStartText)
        assertEquals("day:gregorian", result.topEndText)

        // Solar calendars beyond Gregorian format their fixed ICU id off the plain instant.
        val solarConfig = CustomWidgetConfig(
            topStart = HeaderBlock.Date(calendar = DateCalendar.Persian, withDayName = false),
            topEnd = HeaderBlock.Date(calendar = DateCalendar.Buddhist, withDayName = true),
        )
        val solarResult = useCase().invoke(at(9.0), settings(), calc(), location, solarConfig)!!
        assertEquals("date:persian", solarResult.topStartText)
        assertEquals("day:buddhist", solarResult.topEndText)

        val locConfig = CustomWidgetConfig(topStart = HeaderBlock.LocationName, topEnd = null)
        val locResult = useCase().invoke(at(9.0), settings(), calc(), location, locConfig)!!
        assertEquals("Testville", locResult.topStartText)
        assertNull(locResult.topEndText)
    }

    @Test
    fun `header date format applies to both date slots`() {
        val config = CustomWidgetConfig(
            topStart = HeaderBlock.Date(calendar = DateCalendar.Hijri, withDayName = true),
            topEnd = HeaderBlock.Date(calendar = DateCalendar.Gregorian, withDayName = false),
            headerDateFormat = HeaderDateFormat.Numeric,
        )
        val result = useCase().invoke(at(9.0), settings(), calc(), location, config)!!
        assertEquals("day:islamic:Numeric", result.topStartText)
        assertEquals("date:gregorian:Numeric", result.topEndText)
    }

    @Test
    fun `countdown targets the next prayer only when enabled`() {
        val on = useCase(next = details(Prayer.Asr, at(15.0)))
            .invoke(at(9.0), settings(), calc(), location, CustomWidgetConfig(showCountdown = true))!!
        assertEquals(Prayer.Asr, on.countdown?.prayer)
        assertEquals(at(15.0).toEpochMilliseconds(), on.countdown?.baseMillis)

        val off = useCase().invoke(at(9.0), settings(), calc(), location, CustomWidgetConfig(showCountdown = false))!!
        assertNull(off.countdown)
    }

    @Test
    fun `next-prayer search excludes prayers not placed on the widget`() {
        val getShariaTimes = mock<GetShariaTimesUseCase> {
            on { invoke(any(), any(), any(), any(), any()) } doReturn ShariaTimesResult.Available(shariaTimes)
        }
        val getNext = mock<GetNextShariaTimesUseCase> {
            on { invoke(any(), any(), any(), any(), any(), anyOrNull(), any(), any()) } doReturn details(Prayer.Dhuhr, at(12.0))
        }
        val config = CustomWidgetConfig(rows = listOf(listOf(Prayer.Fajr, Prayer.Dhuhr)), showCountdown = true)

        BuildCustomWidgetDataUseCase(getShariaTimes, getNext, FakeFormatter(nextDayMillis))
            .invoke(at(9.0), settings(), calc(), location, config)

        // Only placed prayers stay searchable; every other prayer (Sunset, Maghrib, Isha, …) is excluded,
        // so the countdown can never target a time the widget doesn't show.
        val excluded = argumentCaptor<Set<Prayer>>()
        verify(getNext, atLeastOnce()).invoke(any(), any(), any(), any(), any(), anyOrNull(), excluded.capture(), any())
        assertEquals(SHARIA_TIMES_IN_ORDER.toSet() - setOf(Prayer.Fajr, Prayer.Dhuhr), excluded.firstValue)
    }

    @Test
    fun `colors pass through from the config`() {
        val config = CustomWidgetConfig(bgColor = 0x11223344, textColor = null, highlightColor = 0x55667788.toInt())
        val result = useCase().invoke(at(9.0), settings(), calc(), location, config)!!
        assertEquals(0x11223344, result.bgColor)
        assertNull(result.textColor)
        assertEquals(0x55667788.toInt(), result.highlightColor)
    }

    @Test
    fun `a single location leaves pages empty for inline rendering`() {
        val result = useCase().invoke(at(9.0), settings(), calc(), location, CustomWidgetConfig())!!
        assertTrue(result.pages.isEmpty())
    }

    @Test
    fun `toggled locations produce a page each in order`() {
        val favorites = listOf(
            StaticFavoriteLocation("tehran", CalculationLocationDetail(0.0, 0.0, label = "Tehran")),
            StaticFavoriteLocation("mecca", CalculationLocationDetail(1.0, 1.0, label = "Mecca")),
        )
        val config = CustomWidgetConfig(
            rows = listOf(listOf(Prayer.Fajr, Prayer.Dhuhr)),
            locationIds = listOf("tehran", "mecca"),
        )
        val result = useCase().invoke(at(9.0), settings(), calc(), location, config, favorites)!!
        assertEquals(2, result.pages.size)
        assertEquals("Tehran", result.pages[0].name)
        assertEquals("Mecca", result.pages[1].name)
        assertEquals(
            listOf(Prayer.Fajr, Prayer.Dhuhr),
            result.pages[0].prayerRows.flatten().map { it.prayer },
        )
        // Inline rows mirror the first page so single-location code paths still have data.
        assertEquals(result.pages[0].prayerRows, result.prayerRows)
    }

    @Test
    fun `nextUpdate is the earliest of next prayer and day rollover`() {
        val result = useCase(next = details(Prayer.Asr, at(15.0)), nextDay = at(28.0).toEpochMilliseconds())
            .invoke(at(9.0), settings(), calc(), location, CustomWidgetConfig())!!
        assertEquals(at(15.0).toEpochMilliseconds(), result.nextUpdateAtMillis)
    }
}
