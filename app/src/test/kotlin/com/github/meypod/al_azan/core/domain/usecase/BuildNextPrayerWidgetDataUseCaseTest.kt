package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.data.DateComponents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.time.Instant

class BuildNextPrayerWidgetDataUseCaseTest {

    private val hour = 3_600_000L
    private val base = 1_700_000_000_000L
    private fun at(h: Double) = Instant.fromEpochMilliseconds(base + (h * hour).toLong())

    private val location = CalculationLocationDetail(lat = 0.0, long = 0.0, label = "Testville")
    private val params = CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters

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

    private val getNext = mock<GetNextShariaTimesUseCase> {
        on { invoke(any(), any(), any(), any(), any(), anyOrNull(), any(), any()) } doReturn details(Prayer.Asr, at(15.0))
    }

    private fun useCase(next: ShariaTimeDetails? = details(Prayer.Asr, at(15.0))): BuildNextPrayerWidgetDataUseCase {
        val mock = mock<GetNextShariaTimesUseCase> {
            on { invoke(any(), any(), any(), any(), any(), anyOrNull(), any(), any()) } doReturn next
        }
        return BuildNextPrayerWidgetDataUseCase(mock)
    }

    private fun settings(countdownPrayers: List<Prayer> = listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr, Prayer.Maghrib, Prayer.Isha)) =
        Settings(
            selectedLocale = "en",
            selectedArabicCalendar = "islamic",
            selectedSecondaryCalendar = SecondaryCalendar.Gregorian,
            countdownWidgetPrayers = countdownPrayers,
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
    fun `returns null when no prayer is selected`() {
        val result = useCase().invoke(at(9.0), settings(countdownPrayers = emptyList()), calc(), location)
        assertNull(result)
    }

    @Test
    fun `returns null when there is no upcoming prayer`() {
        val result = useCase(next = null).invoke(at(9.0), settings(), calc(), location)
        assertNull(result)
    }

    @Test
    fun `targets the next prayer among the selected set`() {
        val result = useCase(next = details(Prayer.Asr, at(15.0))).invoke(at(9.0), settings(), calc(), location)!!
        assertEquals(Prayer.Asr, result.prayer)
        assertEquals(at(15.0).toEpochMilliseconds(), result.countdownBaseMillis)
        assertEquals(at(15.0).toEpochMilliseconds(), result.nextUpdateAtMillis)
    }

    @Test
    fun `excludes the unselected prayers from the search`() {
        // Only Fajr + Maghrib selected -> every other prayer must be excluded.
        val selected = listOf(Prayer.Fajr, Prayer.Maghrib)
        val expectedExcluded = Prayer.entries.toSet() - selected.toSet()
        BuildNextPrayerWidgetDataUseCase(getNext).invoke(at(9.0), settings(countdownPrayers = selected), calc(), location)

        val captor = argumentCaptor<Set<Prayer>>()
        verify(getNext).invoke(any(), any(), any(), any(), any(), anyOrNull(), captor.capture(), any())
        assertEquals(expectedExcluded, captor.firstValue)
    }

    @Test
    fun `nextUpdate is null for a past target`() {
        val result = useCase(next = details(Prayer.Asr, at(8.0))).invoke(at(9.0), settings(), calc(), location)!!
        assertNull(result.nextUpdateAtMillis)
    }

    @Test
    fun `locale carries the selected locale for the renderer`() {
        val result = useCase().invoke(at(9.0), settings(), calc(), location)!!
        assertEquals("en", result.locale)
    }
}
