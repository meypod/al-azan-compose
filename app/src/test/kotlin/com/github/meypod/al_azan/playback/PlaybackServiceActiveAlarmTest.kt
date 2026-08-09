package com.github.meypod.al_azan.playback

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The alarm the app and the alarm screen render from. Each rule here is one the user would feel: a
 * dismissed alarm whose screen re-opens itself, or a live alarm hidden by a dismissal meant for the one
 * it replaced.
 *
 * The state is process-wide, so every test clears it on both sides rather than assuming a starting id.
 */
class PlaybackServiceActiveAlarmTest {

    @Before
    @After
    fun clearActiveAlarm() {
        PlaybackService.activeAlarm.value?.let { PlaybackService.markAlarmHandled(it.id) }
    }

    private fun start(
        title: String = "Fajr",
        prayer: Prayer? = Prayer.Fajr,
        isReminder: Boolean = false,
    ) = PlaybackService.startAlarm(
        prayer = prayer,
        timeLabel = "05:30",
        title = title,
        header = "Adhan",
        isReminder = isReminder,
    )

    @Test
    fun `starting an alarm publishes it`() {
        val alarm = start()

        assertEquals(alarm, PlaybackService.activeAlarm.value)
        assertEquals(Prayer.Fajr, alarm.prayer)
        assertEquals("05:30", alarm.timeLabel)
    }

    /** 0 is the id an absent alarm would carry, so a live one must never take it. */
    @Test
    fun `ids are never zero`() {
        assertNotEquals(0L, start().id)
    }

    @Test
    fun `each alarm gets its own id`() {
        assertNotEquals(start("Fajr").id, start("Dhuhr").id)
    }

    @Test
    fun `handling the sounding alarm retires it`() {
        val alarm = start()

        PlaybackService.markAlarmHandled(alarm.id)

        assertNull(PlaybackService.activeAlarm.value)
    }

    /**
     * The dismiss that matters: the user acted on the screen for the first alarm just as a second one took
     * over. Retiring the first must not hide the second, which is still sounding.
     */
    @Test
    fun `handling a replaced alarm leaves the live one sounding`() {
        val first = start("Fajr")
        val second = start("Dhuhr")

        PlaybackService.markAlarmHandled(first.id)

        assertEquals(second, PlaybackService.activeAlarm.value)
    }

    @Test
    fun `handling an unknown id changes nothing`() {
        val alarm = start()

        PlaybackService.markAlarmHandled(alarm.id + 999)

        assertEquals(alarm, PlaybackService.activeAlarm.value)
    }

    @Test
    fun `a new alarm after one was handled is published`() {
        val first = start("Fajr")
        PlaybackService.markAlarmHandled(first.id)

        val second = start("Dhuhr")

        assertEquals(second, PlaybackService.activeAlarm.value)
    }

    /** Reminders reuse the same screen, so they go through the same state with their flag set. */
    @Test
    fun `a reminder is published as one`() {
        val alarm = start(title = "Reminder", prayer = null, isReminder = true)

        assertTrue(alarm.isReminder)
        assertNull(alarm.prayer)
        assertEquals(alarm, PlaybackService.activeAlarm.value)
    }
}
