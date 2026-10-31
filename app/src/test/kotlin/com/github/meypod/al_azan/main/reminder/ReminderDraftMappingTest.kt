package com.github.meypod.al_azan.main.reminder

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderInterval
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderDraftMappingTest {

    private val interval = ReminderInterval(everyDays = 3, startDate = LocalDate(2026, 11, 2))
    private val mondayWednesday = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)

    private fun roundTrip(draft: ReminderEditDraft) = draft.toReminder("r").toDraft().copy(id = null)

    @Test
    fun `each repeat mode survives a save and reopen`() {
        listOf(
            ReminderEditDraft(repeat = ReminderRepeat.Once),
            ReminderEditDraft(repeat = ReminderRepeat.Weekdays, days = mondayWednesday),
            ReminderEditDraft(repeat = ReminderRepeat.Every(interval)),
        ).forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `once stores no day rule or interval`() {
        val r = ReminderEditDraft(repeat = ReminderRepeat.Once, days = mondayWednesday).toReminder("r")
        assertEquals(true, r.once)
        assertNull(r.days)
        assertNull(r.interval)
    }

    @Test
    fun `interval keeps every day as the fallback for older app versions`() {
        val r = ReminderEditDraft(repeat = ReminderRepeat.Every(interval)).toReminder("r")
        assertEquals(false, r.once)
        assertEquals(PrayerAlarmSettings.Bool(true), r.days)
        assertEquals(interval, r.interval)
    }

    @Test
    fun `weekday reminders store only the selected days`() {
        val r = ReminderEditDraft(repeat = ReminderRepeat.Weekdays, days = mondayWednesday).toReminder("r")
        assertNull(r.interval)
        assertEquals(mondayWednesday, r.days?.selectedDays())
    }

    /** The alarm side repeats these (it only disables once == true), so the editor must not show Once. */
    @Test
    fun `legacy reminders without a once flag open as repeating`() {
        val legacy = Reminder(id = "r", prayer = Prayer.Fajr, duration = 5, durationModifier = -1)
        assertEquals(ReminderRepeat.Weekdays, legacy.toDraft().repeat)
        assertEquals(DayOfWeek.entries.toSet(), legacy.toDraft().days)

        val legacyWeekly = legacy.copy(days = PrayerAlarmSettings.ByWeekDay(mondayWednesday.associateWith { true }))
        val saved = legacyWeekly.toDraft().toReminder("r")
        assertEquals(false, saved.once)
        assertEquals(mondayWednesday, saved.days?.selectedDays())
    }
}
