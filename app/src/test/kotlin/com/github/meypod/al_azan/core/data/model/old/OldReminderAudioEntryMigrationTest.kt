package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class OldReminderAudioEntryMigrationTest {
    /** A bundled sound must re-resolve to THIS build's resource int, never the stale stored one. */
    @Test
    fun mapsResourceByStableIdNotStoredInt() {
        val old = OldAudioEntry.OldResourceOldAudioEntry(
            id = "masjid_an_nabawi",
            filepath = 999999, // stale id from the old app — must be ignored
            label = "Masjid an-Nabawi",
            loop = true,
            notif = true,
        )

        val migrated = old.toReminderAudioEntry()

        val resource = migrated as ReminderAudioEntry.ResourceReminderAudioEntry
        assertEquals("masjid_an_nabawi", resource.id)
        assertEquals(R.raw.masjid_an_nabawi, resource.resourceId)
        assertEquals(true, resource.loop)
        assertEquals(true, resource.notif)
    }

    /** A sound id this build no longer bundles must not point at a wrong/missing resource. */
    @Test
    fun unknownResourceIdFallsBackToDefault() {
        val old = OldAudioEntry.OldResourceOldAudioEntry(
            id = "some_removed_sound",
            filepath = 12345,
            label = "Gone",
        )

        assertSame(ReminderAudioEntry.DefaultReminderAudioEntry, old.toReminderAudioEntry())
    }

    @Test
    fun mapsDefaultEntryToDefault() {
        val old = OldAudioEntry.OldDefaultAudioEntry(label = "Default")
        assertSame(ReminderAudioEntry.DefaultReminderAudioEntry, old.toReminderAudioEntry())
    }

    @Test
    fun keepsExternalFilepath() {
        val old = OldAudioEntry.OldExternalAudioEntry(
            id = "ext",
            filepath = "/data/custom.mp3",
            label = "Custom",
            loop = true,
        )

        val migrated = old.toReminderAudioEntry() as ReminderAudioEntry.ExternalReminderAudioEntry
        assertEquals("/data/custom.mp3", migrated.filepath)
        assertEquals(true, migrated.loop)
    }
}
