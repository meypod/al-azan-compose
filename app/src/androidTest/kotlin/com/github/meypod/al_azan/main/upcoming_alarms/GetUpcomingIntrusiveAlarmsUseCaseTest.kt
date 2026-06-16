package com.github.meypod.al_azan.main.upcoming_alarms

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.meypod.al_azan.core.data.audio.AudioDurationProbe
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import io.github.meypod.adhan_kotlin.CalculationMethod
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone
import kotlin.time.Instant

/**
 * Instrumented (real ICU + adhan) tests for [GetUpcomingIntrusiveAlarmsUseCase]: the derived today +
 * tomorrow schedule that backs the Upcoming-alarms screen. Intrusiveness is forced via continuous
 * vibration so these don't depend on probing real audio files.
 */
@RunWith(AndroidJUnit4::class)
class GetUpcomingIntrusiveAlarmsUseCaseTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val useCase = GetUpcomingIntrusiveAlarmsUseCase(GetShariaTimesUseCase(), AudioDurationProbe(context))

    private val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
    private val location = CalculationLocationDetail(lat = 0.0, long = 0.0)
    private val calc = CalculationSettings(parameters = params)

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

    private fun run(
        nowIso: String,
        settings: Settings,
        alarmSettings: AlarmSettings,
        reminders: List<Reminder> = emptyList(),
    ) = runBlocking {
        useCase(
            nowMs = Instant.parse(nowIso).toEpochMilliseconds(),
            settings = settings,
            alarmSettings = alarmSettings,
            calc = calc,
            location = location,
            reminders = reminders,
        )
    }

    private fun UpcomingOccurrence.fireDate(): LocalDate = Instant.fromEpochMilliseconds(fireTimeMs).toLocalDate()

    private fun settings(skipped: List<SkippedAlarm> = emptyList()) = Settings(selectedLocale = "en-US", skippedOccurrences = skipped)

    @Test
    fun tahajjud_bothNightsAppear_anchorDayNotFireTimeCutoff() {
        // Tahajjud fires after midnight. A fire-time "end of tomorrow" cutoff would keep today's tahajjud
        // (fires the 16th) but drop tomorrow's (fires the 17th). Anchor-day scoping keeps both nights.
        val alarmSettings = AlarmSettings(
            tahajjudNotify = PrayerAlarmSettings.Bool(true),
            tahajjudSound = PrayerAlarmSettings.Bool(true),
            tahajjudVibration = VibrationMode.Continuous,
        )
        val result = run("2024-01-15T12:00:00Z", settings(), alarmSettings)

        val tahajjudDates = result.filter { it.prayer == Prayer.Tahajjud }.map { it.fireDate() }.toSet()
        assertTrue("today's tahajjud (fires 16th) must be present", LocalDate(2024, 1, 16) in tahajjudDates)
        assertTrue("tomorrow's tahajjud (fires 17th) must be present", LocalDate(2024, 1, 17) in tahajjudDates)
        assertTrue(result.all { it.isAdhan })
    }

    @Test
    fun earlyMorning_yesterdaysTahajjudStillUpcoming_isIncluded() {
        // Just after midnight (00:30 on the 16th), the tahajjud still ahead is computed from the *15th's*
        // times yet fires later that morning (last-third-of-night, ~01:20 at the equator). Only the
        // dayOffset -1 scan reaches it; dropping that would miss it (the 16th's own tahajjud fires the 17th).
        val alarmSettings = AlarmSettings(
            tahajjudNotify = PrayerAlarmSettings.Bool(true),
            tahajjudSound = PrayerAlarmSettings.Bool(true),
            tahajjudVibration = VibrationMode.Continuous,
        )
        val result = run("2024-01-16T00:30:00Z", settings(), alarmSettings)

        val tahajjudDates = result.filter { it.prayer == Prayer.Tahajjud }.map { it.fireDate() }.toSet()
        assertTrue("yesterday-anchored tahajjud firing the 16th must be present", LocalDate(2024, 1, 16) in tahajjudDates)
    }

    @Test
    fun skippedOccurrence_isFlagged_onlyForItsDate() {
        val alarmSettings = AlarmSettings(
            tahajjudNotify = PrayerAlarmSettings.Bool(true),
            tahajjudSound = PrayerAlarmSettings.Bool(true),
            tahajjudVibration = VibrationMode.Continuous,
        )
        val skipped = listOf(SkippedAlarm.Adhan(Prayer.Tahajjud, LocalDate(2024, 1, 16)))
        val result = run("2024-01-15T12:00:00Z", settings(skipped), alarmSettings)

        val byDate = result.filter { it.prayer == Prayer.Tahajjud }.associateBy { it.fireDate() }
        assertTrue("the skipped night is flagged", byDate.getValue(LocalDate(2024, 1, 16)).skipped)
        assertFalse("the other night is not", byDate.getValue(LocalDate(2024, 1, 17)).skipped)
    }

    @Test
    fun adhan_pastOccurrenceExcluded_tomorrowIncluded() {
        // At 14:00 the 15th's Dhuhr (~12:00) has passed. Only tomorrow's (16th) should remain; the 17th
        // is out of the today+tomorrow scope.
        val alarmSettings = AlarmSettings(
            dhuhrNotify = PrayerAlarmSettings.Bool(true),
            dhuhrSound = PrayerAlarmSettings.Bool(true),
            dhuhrVibration = VibrationMode.Continuous,
        )
        val result = run("2024-01-15T14:00:00Z", settings(), alarmSettings)

        val dhuhrDates = result.filter { it.prayer == Prayer.Dhuhr }.map { it.fireDate() }.toSet()
        assertEquals(setOf(LocalDate(2024, 1, 16)), dhuhrDates)
    }

    @Test
    fun reminder_occurrencesAppear_withIdentityAndSkipFlag() {
        // Reminder 10 min before Dhuhr, daily, intrusive via continuous vibration. At 09:00 both today's
        // (~11:50 on the 15th) and tomorrow's (16th) are upcoming; skipping the 15th flags only it.
        val reminder = Reminder(
            id = "r1",
            enabled = true,
            prayer = Prayer.Dhuhr,
            duration = 10,
            durationModifier = -1,
            vibration = VibrationMode.Continuous,
        )
        val skipped = listOf(SkippedAlarm.Reminder("r1", LocalDate(2024, 1, 15)))
        val result = run("2024-01-15T09:00:00Z", settings(skipped), AlarmSettings(), listOf(reminder))

        val reminders = result.filter { !it.isAdhan }
        assertEquals("r1", (reminders.first().occurrence as SkippedAlarm.Reminder).reminderId)
        val byDate = reminders.associateBy { it.fireDate() }
        assertTrue(LocalDate(2024, 1, 16) in byDate.keys)
        assertTrue("the skipped day is flagged", byDate.getValue(LocalDate(2024, 1, 15)).skipped)
        assertFalse(byDate.getValue(LocalDate(2024, 1, 16)).skipped)
    }
}
