package com.github.meypod.al_azan.core.domain.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.data.DateComponents
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone
import kotlin.time.Instant

/**
 * Instrumented (real ICU + adhan) tests for [GetNextShariaTimesUseCase]'s prayer selection, especially
 * the cross-day path that regressed: when every remaining visible prayer today is in the past, the next
 * prayer must roll over to the following day's first non-excluded prayer (it used to return null).
 */
@RunWith(AndroidJUnit4::class)
class GetNextShariaTimesUseCaseTest {

    private val getNext = GetNextShariaTimesUseCase(GetShariaTimesUseCase())
    private val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
    private val location = CalculationLocationDetail(lat = 0.0, long = 0.0)

    private lateinit var originalTimeZone: TimeZone

    @Before
    fun fixTimeZone() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone)
    }

    private fun nextPrayer(
        instant: Instant,
        excluding: Set<Prayer>,
    ) = getNext(
        instant = instant,
        calculationParameters = params,
        calculationAdjustments = CalculationAdjustments(),
        arabicCalendar = "islamic",
        locationDetail = location,
        excluding = excluding,
    )

    @Test
    fun crossDay_skipsHiddenNightPrayers_andRollsToNextDayFajr() {
        // 23:30 UTC: every daytime prayer has passed; the only "upcoming today" times are the sunnah
        // night prayers, which are excluded. So the next prayer must be tomorrow's Fajr.
        val instant = Instant.parse("2024-01-15T23:30:00Z")

        val result = nextPrayer(instant, excluding = setOf(Prayer.Sunset, Prayer.Midnight, Prayer.Tahajjud))

        assertNotNull("cross-day next prayer must not be null", result)
        assertEquals(Prayer.Fajr, result!!.prayer)
        assertEquals(DateComponents(2024, 1, 16), result.forDate)
        assertTrue("prayer time must be in the future", result.prayerTime > instant)
    }

    @Test
    fun sameDay_skipsHiddenPrayer_andReturnsNextVisibleSameDay() {
        // 11:00 UTC: the next time is Dhuhr; hide it and the next visible one (Asr) must be returned,
        // still on the same day.
        val instant = Instant.parse("2024-01-15T11:00:00Z")

        val result = nextPrayer(instant, excluding = setOf(Prayer.Dhuhr))

        assertNotNull(result)
        assertEquals(Prayer.Asr, result!!.prayer)
        assertEquals(DateComponents(2024, 1, 15), result.forDate)
        assertTrue(result.prayerTime > instant)
    }
}
