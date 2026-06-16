package com.github.meypod.al_azan.core.domain.model.alarm

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic coverage for the skip/reschedule policy: logical (prayer/reminder + date) membership,
 * past-day pruning per stream, and the idempotent upsert/without set-math.
 */
class SkippedAlarmPolicyTest {

    private val d1 = LocalDate(2026, 1, 10)
    private val d2 = LocalDate(2026, 1, 11)
    private val reminderId = "rem_x"

    @Test
    fun `isAdhanSkipped matches only the same prayer and date`() {
        val entries = listOf(SkippedAlarm.Adhan(Prayer.Dhuhr, d1))
        assertTrue(entries.isAdhanSkipped(Prayer.Dhuhr, d1))
        assertFalse(entries.isAdhanSkipped(Prayer.Dhuhr, d2))
        assertFalse(entries.isAdhanSkipped(Prayer.Asr, d1))
    }

    @Test
    fun `isReminderSkipped matches only the same reminder and date`() {
        val entries = listOf(SkippedAlarm.Reminder(reminderId, d1))
        assertTrue(entries.isReminderSkipped(reminderId, d1))
        assertFalse(entries.isReminderSkipped(reminderId, d2))
        assertFalse(entries.isReminderSkipped("other", d1))
    }

    @Test
    fun `prunePastDays drops own past-day entries including nothing on today`() {
        val entries = listOf(
            SkippedAlarm.Adhan(Prayer.Fajr, d1),
            SkippedAlarm.Adhan(Prayer.Isha, d2),
        )
        // today == d2: the d1 entry is in the past and dropped, the d2 entry stays.
        assertEquals(listOf(SkippedAlarm.Adhan(Prayer.Isha, d2)), entries.prunePastDays<SkippedAlarm.Adhan>(d2))
    }

    @Test
    fun `prunePastDays leaves other streams' past entries untouched`() {
        val entries = listOf(
            SkippedAlarm.Adhan(Prayer.Fajr, d1),
            SkippedAlarm.Reminder(reminderId, d1),
        )
        // Adhan scheduler prunes only adhan; the reminder's stale entry is the reminder scheduler's job.
        assertEquals(listOf(SkippedAlarm.Reminder(reminderId, d1)), entries.prunePastDays<SkippedAlarm.Adhan>(d2))
    }

    @Test
    fun `upsert is idempotent for the same occurrence`() {
        val entry = SkippedAlarm.Adhan(Prayer.Dhuhr, d1)
        val result = listOf(entry).upsert(SkippedAlarm.Adhan(Prayer.Dhuhr, d1))
        assertEquals(listOf(entry), result)
    }

    @Test
    fun `upsert appends a different occurrence`() {
        val entries = listOf(SkippedAlarm.Adhan(Prayer.Dhuhr, d1))
        assertEquals(
            listOf(SkippedAlarm.Adhan(Prayer.Dhuhr, d1), SkippedAlarm.Adhan(Prayer.Dhuhr, d2)),
            entries.upsert(SkippedAlarm.Adhan(Prayer.Dhuhr, d2)),
        )
    }

    @Test
    fun `without removes exactly the target occurrence`() {
        val entries = listOf(
            SkippedAlarm.Adhan(Prayer.Dhuhr, d1),
            SkippedAlarm.Adhan(Prayer.Dhuhr, d2),
        )
        assertEquals(
            listOf(SkippedAlarm.Adhan(Prayer.Dhuhr, d2)),
            entries.without(SkippedAlarm.Adhan(Prayer.Dhuhr, d1)),
        )
    }

    @Test
    fun `without a missing occurrence is a no-op`() {
        val entries = listOf(SkippedAlarm.Adhan(Prayer.Dhuhr, d1))
        assertEquals(entries, entries.without(SkippedAlarm.Adhan(Prayer.Asr, d1)))
    }

    @Test
    fun `membership checks ignore the other stream`() {
        // An adhan entry and a reminder entry sharing prayer/date must not cross-match.
        val entries = listOf(SkippedAlarm.Adhan(Prayer.Dhuhr, d1), SkippedAlarm.Reminder(reminderId, d1))
        assertFalse(entries.isReminderSkipped("rem_y", d1))
        assertFalse(emptyList<SkippedAlarm>().isAdhanSkipped(Prayer.Dhuhr, d1))
        assertTrue(entries.isAdhanSkipped(Prayer.Dhuhr, d1))
        assertTrue(entries.isReminderSkipped(reminderId, d1))
    }
}
