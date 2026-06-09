package com.github.meypod.al_azan.core.domain.model.settings

import junit.framework.TestCase.assertEquals
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
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

    @Test
    fun roundTripsResourceEntry() {
        val entry = AudioEntry.ResourceAudioEntry(id = "res-1", resId = 1234, labelResId = 5678)
        assertEquals(entry, roundTrip(entry))
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
