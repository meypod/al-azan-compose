package com.github.meypod.al_azan.core.domain.model.settings

import com.github.meypod.al_azan.R
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEntrySerializerTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun roundTrip(entry: AudioEntry): AudioEntry {
        val encoded = json.encodeToString(AudioEntrySerializer, entry)
        return json.decodeFromString(AudioEntrySerializer, encoded)
    }

    /**
     * Regression (crash on Muezzin/schedule screen): persisted `resId`/`labelResId` are raw `R` ints
     * that shift between builds. A bundled entry must re-resolve from its stable [AudioEntry.id] on
     * read — the stale persisted ints are discarded — so the label never points at a since-moved or
     * removed resource (which threw `Resources$NotFoundException`).
     */
    @Test
    fun reResolvesBundledResourceEntryFromStableId() {
        val stale = AudioEntry.ResourceAudioEntry(id = "masjid_an_nabawi", resId = 1234, labelResId = 5678)
        assertEquals(mapAdhanIdToEntryOrNull("masjid_an_nabawi"), roundTrip(stale))
    }

    /** An id no longer bundled degrades to an unresolvable entry instead of crashing on label lookup. */
    @Test
    fun unknownResourceEntryDegradesGracefully() {
        val decoded = roundTrip(AudioEntry.ResourceAudioEntry(id = "since-removed", resId = 1234, labelResId = 5678))
        assertTrue(decoded is AudioEntry.ResourceAudioEntry)
        decoded as AudioEntry.ResourceAudioEntry
        assertNull(decoded.resId)
        assertEquals(R.string.unknown, decoded.labelResId)
        assertFalse(decoded.isResolvable())
    }

    /**
     * Regression: the polymorphic deserializer used to always pick ResourceAudioEntry, stripping an
     * external entry's filepath/label and resolving it to "unknown" with no playable URI.
     */
    @Test
    fun roundTripsExternalEntry() {
        val entry = AudioEntry.ExternalAudioEntry(
            id = "ext-1",
            filepath = "/data/sound.mp3",
            label = "Custom",
            loop = true,
        )
        val decoded = roundTrip(entry)
        assertTrue("external entry must not deserialize as a resource entry", decoded is AudioEntry.ExternalAudioEntry)
        assertEquals(entry, decoded)
    }

    @Test
    fun resolvableReflectsPlayability() {
        assertTrue(AudioEntry.ResourceAudioEntry(id = "r", resId = 1).isResolvable())
        assertFalse(AudioEntry.ResourceAudioEntry(id = "r", resId = null).isResolvable())
        assertTrue(AudioEntry.ExternalAudioEntry(id = "e", filepath = "/x.mp3").isResolvable())
        assertFalse(AudioEntry.ExternalAudioEntry(id = "e", filepath = null).isResolvable())
        assertFalse(AudioEntry.ExternalAudioEntry(id = "e", filepath = "").isResolvable())
    }
}
