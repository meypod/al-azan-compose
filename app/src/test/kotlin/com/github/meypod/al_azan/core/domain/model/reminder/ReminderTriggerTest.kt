package com.github.meypod.al_azan.core.domain.model.reminder

import android.app.Application
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import io.github.meypod.adhan_kotlin.CalculationMethod
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone
import kotlin.time.Instant
import kotlinx.datetime.TimeZone as KotlinxTimeZone

/** Real prayer times for Tehran, so fire times and dates behave as on a device there. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReminderTriggerTest {

    private val tehran = CalculationLocationDetail(lat = 35.69, long = 51.39)
    private val getShariaTimes = GetShariaTimesUseCase()
    private val start = LocalDate(2026, 11, 2)
    private lateinit var savedZone: TimeZone

    @Before
    fun pinZone() {
        savedZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tehran"))
    }

    @After
    fun restoreZone() = TimeZone.setDefault(savedZone)

    private fun timesFor(instant: Instant) =
        getShariaTimes(
            instant = instant,
            calculationParameters = CalculationMethod.TEHRAN.parameters,
            calculationAdjustments = CalculationAdjustments(),
            arabicCalendar = "islamic",
            locationDetail = tehran,
        ).timesOrNull

    private fun day(offset: Int) = start + DatePeriod(days = offset)

    private fun at(
        date: LocalDate,
        hour: Int,
    ) = date.atTime(LocalTime(hour, 0)).toInstant(KotlinxTimeZone.currentSystemDefault())

    private fun reminder(
        everyDays: Int? = null,
        prayer: Prayer = Prayer.Fajr,
        days: PrayerAlarmSettings? = PrayerAlarmSettings.Bool(true),
    ) = Reminder(
        id = "r",
        prayer = prayer,
        duration = 5,
        durationModifier = -1,
        once = false,
        days = days,
        interval = everyDays?.let { ReminderInterval(it, start) },
    )

    private fun skip(vararg offsets: Int) = offsets.map { SkippedAlarm.Reminder("r", day(it)) }

    private fun Reminder.nextDate(
        from: Instant,
        skipped: List<SkippedAlarm> = emptyList(),
    ) = nextTrigger(from, skipped, ::timesFor)?.toLocalDate()

    @Test
    fun `fires on the start day before its prayer time`() {
        assertEquals(start, reminder(everyDays = 2).nextDate(at(start, 0)))
    }

    @Test
    fun `after the day's fire time it moves to the next cycle day`() {
        assertEquals(day(2), reminder(everyDays = 2).nextDate(at(start, 12)))
    }

    @Test
    fun `fire time is the prayer time minus the offset`() {
        val trigger = reminder(everyDays = 2).nextTrigger(at(start, 0), emptyList(), ::timesFor)
        assertEquals(timesFor(at(start, 12))!!.fajr.toEpochMilliseconds() - 5 * 60_000, trigger?.toEpochMilliseconds())
    }

    /** Regression: today's occurrence passed plus one skip used to fall outside the scan window. */
    @Test
    fun `skipping the next occurrence of a long interval still finds the one after`() {
        assertEquals(day(12), reminder(everyDays = 6).nextDate(at(start, 12), skip(6)))
    }

    @Test
    fun `several skipped occurrences are passed over`() {
        assertEquals(day(18), reminder(everyDays = 6).nextDate(at(start, 12), skip(6, 12)))
    }

    @Test
    fun `the longest interval is found a year ahead`() {
        assertEquals(day(ReminderInterval.MAX_DAYS), reminder(everyDays = ReminderInterval.MAX_DAYS).nextDate(at(start, 12)))
    }

    @Test
    fun `a start date in the future waits for it`() {
        val later = reminder(everyDays = 3).copy(interval = ReminderInterval(3, day(20)))
        assertEquals(day(20), later.nextDate(at(start, 12)))
    }

    /** Tahajjud falls after midnight, so the fire date is the day after the one it is computed for. */
    @Test
    fun `a prayer after midnight fires only on cycle days`() {
        val r = reminder(everyDays = 3, prayer = Prayer.Tahajjud)
        val trigger = r.nextTrigger(at(start, 12), emptyList(), ::timesFor)!!
        assertTrue(r.interval!!.firesOn(trigger.toLocalDate()))
        assertTrue(trigger.toLocalDate() > start)
    }

    @Test
    fun `weekday reminders are unchanged`() {
        val mondays = reminder(days = PrayerAlarmSettings.ByWeekDay(mapOf(DayOfWeek.MONDAY to true)))
        // 2 November 2026 is a Monday; after its Fajr the next Monday is a week later.
        assertEquals(day(7), mondays.nextDate(at(start, 12)))
        assertEquals(day(14), mondays.nextDate(at(start, 12), skip(7)))
    }

    @Test
    fun `no computable prayer times means no trigger`() {
        assertNull(reminder(everyDays = 2).nextTrigger(at(start, 0), emptyList()) { null })
    }
}
