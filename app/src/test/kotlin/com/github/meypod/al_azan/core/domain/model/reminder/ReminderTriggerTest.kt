package com.github.meypod.al_azan.core.domain.model.reminder

import android.app.Application
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.calendarDayOf
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone as KotlinxTimeZone

/** Real prayer times for Tehran, so fire times and dates behave as on a device there. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ReminderTriggerTest {

    private val tehran = CalculationLocationDetail(lat = 35.69, long = 51.39)
    private val getShariaTimes = GetShariaTimesUseCase()
    private val calendars = ReminderCalendars("en", CalendarSystem("islamic-umalqura", "en"))
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
            midnightMethod = MidnightMethod.SunsetToFajr,
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
    ) = nextTrigger(from, skipped, calendars, ::timesFor)?.toLocalDate()

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
        val trigger = reminder(everyDays = 2).nextTrigger(at(start, 0), emptyList(), calendars, ::timesFor)
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

    /** Date rules count the prayer day: Tahajjud of a cycle day fires just after midnight, the next civil day. */
    @Test
    fun `a night prayer follows its prayer day's cycle`() {
        val r = reminder(everyDays = 3, prayer = Prayer.Tahajjud)
        val trigger = r.nextTrigger(at(start, 12), emptyList(), calendars, ::timesFor)!!
        assertEquals(timesFor(at(start, 12))!!.tahajjud - 5.minutes, trigger)
        assertEquals(day(1), trigger.toLocalDate())
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
        assertNull(reminder(everyDays = 2).nextTrigger(at(start, 0), emptyList(), calendars) { null })
    }

    private fun monthly(
        day: Int,
        calendar: MonthlyCalendar = MonthlyCalendar.Civil(SecondaryCalendar.Gregorian),
    ) = reminder().copy(monthly = ReminderMonthly(calendar, day))

    @Test
    fun `monthly fires on that day of the next month`() {
        // After 2 November's Fajr, the 1st of the month next comes on 1 December.
        assertEquals(LocalDate(2026, 12, 1), monthly(1).nextDate(at(start, 12)))
        assertEquals(LocalDate(2026, 11, 30), monthly(ReminderMonthly.LAST_DAY).nextDate(at(start, 12)))
    }

    @Test
    fun `a day missing from a month skips that month`() {
        // November has no 31st, so the next 31st is in December.
        assertEquals(LocalDate(2026, 12, 31), monthly(31).nextDate(at(start, 12)))
    }

    @Test
    fun `skipping a monthly occurrence finds the next month's`() {
        assertEquals(LocalDate(2027, 1, 1), monthly(1).nextDate(at(start, 12), listOf(SkippedAlarm.Reminder("r", LocalDate(2026, 12, 1)))))
    }

    @Test
    fun `lunar monthly follows the hijri date and its adjustment`() {
        val r = monthly(1, MonthlyCalendar.Lunar)
        val first = r.nextDate(at(start, 12))!!
        assertEquals(1, calendarDayOf(first, calendars.lunar).day)
        // With a +1 adjustment the hijri month starts a civil day earlier.
        val shifted = calendars.copy(lunar = calendars.lunar.copy(dayShift = 1))
        val firstShifted = r.nextTrigger(at(start, 12), emptyList(), shifted, ::timesFor)!!.toLocalDate()
        assertEquals(first + DatePeriod(days = -1), firstShifted)
    }

    private fun onceOn(date: LocalDate?) = reminder().copy(once = true, days = null, onceDate = date)

    @Test
    fun `a dated one-off fires only on its date`() {
        assertEquals(day(3), onceOn(day(3)).nextDate(at(start, 12)))
        // Its date's time already passed: nothing to arm.
        assertNull(onceOn(start).nextDate(at(start, 12)))
        assertNull(onceOn(day(-1)).nextDate(at(start, 12)))
    }

    @Test
    fun `a dated one-off far ahead is still found`() {
        assertEquals(day(200), onceOn(day(200)).nextDate(at(start, 12)))
    }

    @Test
    fun `an older one-off without a date fires at its next occurrence`() {
        assertEquals(day(1), onceOn(null).nextDate(at(start, 12)))
    }

    /**
     * Reminders as v2.1.2 (and its migration from the old app) stored them, with none of the new
     * fields: each must fire exactly as it did before the update.
     */
    private fun released(
        once: Boolean?,
        days: PrayerAlarmSettings?,
    ) = Reminder(id = "r", enabled = true, prayer = Prayer.Fajr, duration = 5, durationModifier = -1, once = once, days = days)

    private val mondays = PrayerAlarmSettings.ByWeekDay(mapOf(DayOfWeek.MONDAY to true))

    @Test
    fun `released one-off fires at the next occurrence`() {
        assertEquals(day(1), released(once = true, days = null).nextDate(at(start, 12)))
    }

    @Test
    fun `released one-off limited to weekdays waits for the next selected day`() {
        // 2 November is a Monday; after its Fajr the next Monday is a week later.
        assertEquals(day(7), released(once = true, days = mondays).nextDate(at(start, 12)))
    }

    @Test
    fun `released repeating reminders keep their weekdays`() {
        assertEquals(day(7), released(once = false, days = mondays).nextDate(at(start, 12)))
        assertEquals(day(1), released(once = false, days = PrayerAlarmSettings.Bool(true)).nextDate(at(start, 12)))
    }

    /** No once flag and no days: the released scheduler fired these daily and never turned them off. */
    @Test
    fun `released reminders without a once flag fire daily`() {
        assertEquals(day(1), released(once = null, days = null).nextDate(at(start, 12)))
    }

    /** Tahajjud for a prayer day falls after 00:00; a reschedule then must not skip tonight's. */
    @Test
    fun `a reschedule after midnight keeps tonight's night prayer`() {
        val tahajjud = reminder(prayer = Prayer.Tahajjud)
        val tonight = timesFor(at(start, 12))!!.tahajjud
        assertTrue(tonight.toLocalDate() > start)
        val justAfterMidnight = at(start + DatePeriod(days = 1), 0)
        assertEquals(tonight - 5.minutes, tahajjud.nextTrigger(justAfterMidnight, emptyList(), calendars, ::timesFor))
        // A dated one-off for that prayer day is found too, rather than cancelled.
        val once = tahajjud.copy(once = true, days = null, onceDate = start)
        assertEquals(tonight - 5.minutes, once.nextTrigger(justAfterMidnight, emptyList(), calendars, ::timesFor))
    }

    /** The Persian 31st exists only in the first six months, so the next one can be most of a year away. */
    @Test
    fun `a monthly day missing from half the year is still found`() {
        val persian = calendars.copy(locale = "en")
        val r = reminder().copy(monthly = ReminderMonthly(MonthlyCalendar.Civil(SecondaryCalendar.Persian), 31))
        // 2 November 2026 is in Aban; the next 31st is 31 Farvardin 1406, 20 April 2027.
        assertEquals(LocalDate(2027, 4, 20), r.nextTrigger(at(start, 12), emptyList(), persian, ::timesFor)?.toLocalDate())
    }

    /** 24-hour steps from late evening used to jump over the date of a spring-forward change. */
    @Test
    fun `a dated one-off on a DST change day is found`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
        val dstDay = LocalDate(2026, 3, 29)
        val once = reminder().copy(once = true, days = null, onceDate = dstDay)
        val lateEvening = LocalDate(2026, 1, 10).atTime(LocalTime(23, 30)).toInstant(KotlinxTimeZone.currentSystemDefault())
        assertEquals(dstDay, once.nextTrigger(lateEvening, emptyList(), calendars, ::timesFor)?.let { it.toLocalDate() })
    }
}
