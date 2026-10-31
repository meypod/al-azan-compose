package com.github.meypod.al_azan.core.domain.usecase

import android.app.Application
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSchedulingDefaults
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.MidnightMethod
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone
import kotlinx.datetime.TimeZone as KotlinxTimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GetNextShariaTimesUseCaseTest {

    private val getNext = GetNextShariaTimesUseCase(GetShariaTimesUseCase())

    // 2 November 2026 is a Monday.
    private val monday = LocalDate(2026, 11, 2)
    private lateinit var savedZone: TimeZone

    @Before
    fun pinZone() {
        savedZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tehran"))
    }

    @After
    fun restoreZone() = TimeZone.setDefault(savedZone)

    private val mondayFajrOnly = AlarmSettings(fajrNotify = PrayerAlarmSettings.ByWeekDay(mapOf(DayOfWeek.MONDAY to true)))

    private fun nextDate(
        skippedDates: Set<LocalDate>,
        searchDays: Int,
    ) = getNext(
        instant = monday.atTime(LocalTime(12, 0)).toInstant(KotlinxTimeZone.currentSystemDefault()),
        calculationParameters = CalculationMethod.TEHRAN.parameters,
        calculationAdjustments = CalculationAdjustments(),
        arabicCalendar = "islamic",
        locationDetail = CalculationLocationDetail(lat = 35.69, long = 51.39),
        midnightMethod = MidnightMethod.SunsetToFajr,
        alarmSettings = mondayFajrOnly,
        isSkipped = { prayer, time -> prayer == Prayer.Fajr && time.toLocalDate() in skippedDates },
        searchDays = searchDays,
    )?.prayerTime?.toLocalDate()

    @Test
    fun `a weekly adhan after today's has passed is next week`() {
        assertEquals(monday + DatePeriod(days = 7), nextDate(emptySet(), AlarmSchedulingDefaults.weeklySearchDays(0)))
    }

    /** Regression: today's passed plus one skip put the next weekly adhan 14 days out, past the scan. */
    @Test
    fun `skipping next week's weekly adhan finds the week after`() {
        val skipped = setOf(monday + DatePeriod(days = 7))
        assertEquals(monday + DatePeriod(days = 14), nextDate(skipped, AlarmSchedulingDefaults.weeklySearchDays(skipped.size)))
    }
}
