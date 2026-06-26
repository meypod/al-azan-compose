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

    /**
     * A bundled adhan voice the old app had DOWNLOADED is stored as an external entry with a now-stale
     * file path. It must re-resolve to this build's bundled resource by its stable id, not keep the path.
     */
    @Test
    fun externalBundledIdReResolvesToResource() {
        val old = OldAudioEntry.OldExternalAudioEntry(
            id = "masjid_an_nabawi",
            filepath = "/old/app/files/masjid_an_nabawi.mp3", // stale path from the old app
            label = "Masjid an-Nabawi",
        )

        val resource = old.toReminderAudioEntry() as ReminderAudioEntry.ResourceReminderAudioEntry
        assertEquals("masjid_an_nabawi", resource.id)
        assertEquals(R.raw.masjid_an_nabawi, resource.resourceId)
    }

    /** The old "silent" entry (stored with a non-uri "silent" filepath) maps to the silent resource. */
    @Test
    fun silentMapsToSilenceResource() {
        val old = OldAudioEntry.OldExternalAudioEntry(
            id = ReminderAudioEntry.SILENT_ID,
            filepath = "silent",
            label = "Silent",
        )

        val resource = old.toReminderAudioEntry() as ReminderAudioEntry.ResourceReminderAudioEntry
        assertEquals(ReminderAudioEntry.SILENT_ID, resource.id)
        assertEquals(R.raw.silence, resource.resourceId)
    }
}
