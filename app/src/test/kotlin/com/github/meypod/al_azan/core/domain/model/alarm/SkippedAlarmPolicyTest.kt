package com.github.meypod.al_azan.core.domain.model.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-logic coverage for the skip/reschedule policy: the floor schedulers arm after, lazy pruning,
 * and the undo set-math that makes an early reschedule cancel the skipped occurrence and every later
 * one on the same stream.
 */
class SkippedAlarmPolicyTest {

    private val adhanId = "adhan_alarm"
    private val reminderId = "reminder_alarm_x"

    private fun adhan(
        fire: Long,
        id: String = adhanId,
    ) = SkippedAlarm.Adhan(id, fire)
    private fun reminder(
        fire: Long,
        id: String = reminderId,
    ) = SkippedAlarm.Reminder(id, fire)

    @Test
    fun `latestFireMsFor returns the max fire time for the matching id`() {
        val entries = listOf(adhan(100), adhan(300), adhan(200))
        assertEquals(300L, entries.latestFireMsFor(adhanId))
    }

    @Test
    fun `latestFireMsFor ignores other streams`() {
        val entries = listOf(adhan(100), reminder(999))
        assertEquals(100L, entries.latestFireMsFor(adhanId))
        assertEquals(999L, entries.latestFireMsFor(reminderId))
    }

    @Test
    fun `latestFireMsFor is zero when no entry matches`() {
        assertEquals(0L, listOf(reminder(500)).latestFireMsFor(adhanId))
        assertEquals(0L, emptyList<SkippedAlarm>().latestFireMsFor(adhanId))
    }

    @Test
    fun `prunePast drops own past entries including the now boundary`() {
        val now = 1_000L
        val entries = listOf(adhan(now - 1), adhan(now), adhan(now + 1))
        assertEquals(listOf(adhan(now + 1)), entries.prunePast<SkippedAlarm.Adhan>(now))
    }

    @Test
    fun `prunePast leaves other streams' past entries untouched`() {
        val now = 1_000L
        val entries = listOf(adhan(now - 1), reminder(now - 1))
        // Adhan scheduler prunes only adhan; the reminder's stale entry is the reminder scheduler's job.
        assertEquals(listOf(reminder(now - 1)), entries.prunePast<SkippedAlarm.Adhan>(now))
    }

    @Test
    fun `withoutFrom removes the target and every later occurrence on the same stream`() {
        val entries = listOf(adhan(100), adhan(200), adhan(300))
        // Rescheduling the 200 occurrence re-arms it, making 200 and 300 moot; 100 stays.
        assertEquals(listOf(adhan(100)), entries.withoutFrom(adhanId, 200))
    }

    @Test
    fun `withoutFrom keeps earlier occurrences and other streams`() {
        val entries = listOf(adhan(100), adhan(300), reminder(300))
        assertEquals(listOf(adhan(100), reminder(300)), entries.withoutFrom(adhanId, 300))
    }

    @Test
    fun `upsert replaces an existing skip for the same occurrence`() {
        val old = SkippedAlarm.Adhan(adhanId, 200)
        val new = SkippedAlarm.Adhan(adhanId, 200)
        val result = listOf(old).upsert(new)
        assertEquals(1, result.size)
        assertEquals(new, result.single())
    }

    @Test
    fun `upsert appends a skip for a different occurrence`() {
        val entries = listOf(adhan(100))
        assertEquals(listOf(adhan(100), adhan(200)), entries.upsert(adhan(200)))
    }
}
