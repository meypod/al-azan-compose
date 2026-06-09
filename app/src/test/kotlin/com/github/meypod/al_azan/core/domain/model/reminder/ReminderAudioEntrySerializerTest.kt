package com.github.meypod.al_azan.core.domain.model.reminder

import junit.framework.TestCase.assertEquals
import kotlinx.serialization.json.Json
import org.junit.Assert.assertSame
import org.junit.Test

class ReminderAudioEntrySerializerTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun roundTrip(entry: ReminderAudioEntry): ReminderAudioEntry {
        val encoded = json.encodeToString(ReminderAudioEntry.serializer(), entry)
        return json.decodeFromString(ReminderAudioEntry.serializer(), encoded)
    }

    @Test
    fun roundTripsResourceEntry() {
        val entry = ReminderAudioEntry.ResourceReminderAudioEntry(
            id = "res-1",
            resourceId = 1234,
            label = "Beep",
            loop = true,
            notif = true,
        )
        assertEquals(entry, roundTrip(entry))
    }

    @Test
    fun roundTripsExternalEntry() {
        val entry = ReminderAudioEntry.ExternalReminderAudioEntry(
            id = "ext-1",
            filepath = "/data/sound.mp3",
            label = "Custom",
            loop = true,
        )
        assertEquals(entry, roundTrip(entry))
    }

    /** Regression: the default object serializes to `{}` and must decode back instead of crashing. */
    @Test
    fun roundTripsDefaultEntry() {
        val decoded = roundTrip(ReminderAudioEntry.DefaultReminderAudioEntry)
        assertSame(ReminderAudioEntry.DefaultReminderAudioEntry, decoded)
    }

    /** A stored default reminder was persisted exactly as `{}` before the key-based serializer. */
    @Test
    fun decodesEmptyObjectAsDefault() {
        val decoded = json.decodeFromString(ReminderAudioEntry.serializer(), "{}")
        assertSame(ReminderAudioEntry.DefaultReminderAudioEntry, decoded)
    }

    /** A reminder carrying a default sound must survive a full Reminder round-trip. */
    @Test
    fun roundTripsReminderWithDefaultSound() {
        val reminder = Reminder(
            id = "r1",
            prayer = com.github.meypod.al_azan.core.domain.model.adhan.Prayer.Fajr,
            duration = -10,
            durationModifier = -1,
            sound = ReminderAudioEntry.DefaultReminderAudioEntry,
        )
        val encoded = json.encodeToString(Reminder.serializer(), reminder)
        val decoded = json.decodeFromString(Reminder.serializer(), encoded)
        assertEquals(reminder, decoded)
    }
}
