package com.github.meypod.al_azan.core.data.model

import com.github.meypod.al_azan.core.data.model.old.OldExportedSettings
import com.github.meypod.al_azan.core.data.model.old.toRestoreData
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the legacy mapping the first-launch migration relies on (it builds the same
 * [OldExportedSettings] from MMKV and calls [toRestoreData]). Guards the migration path which has no
 * runner-level test, using the real backup fixture.
 */
class OldExportedSettingsToRestoreDataTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(resource: String): OldExportedSettings {
        val text = javaClass.classLoader?.getResource(resource)?.readText()
            ?: throw IllegalStateException("Test resource not found: $resource")
        return json.decodeFromString(OldExportedSettings.serializer(), text)
    }

    @Test
    fun mapsLegacyStoresToRestoreData() {
        val data = parse("al_azan_settings.json").toRestoreData()

        assertEquals("fa", data.settings.selectedLocale)
        assertEquals(ThemeColor.Default, data.settings.themeColor)
        assertEquals(7, data.counters.size)
        assertEquals(1, data.reminders.size)
    }

    @Test
    fun foldsLegacyCurrentLocationInAsFirstFavorite() {
        val data = parse("al_azan_settings.json").toRestoreData()

        // calc-settings "current location" (id "default") is folded in ahead of the saved favorites.
        val first = data.favoriteLocations.first()
        assertTrue(first is StaticFavoriteLocation)
        assertEquals("default", first.id)
        // saved favorites still present afterwards
        assertTrue(data.favoriteLocations.any { it.id == "favorite_city_1694315809709" })
    }

    @Test
    fun keepsCustomAudioForMigration() {
        // Migration is an in-place upgrade: custom sound files still exist, so external muezzin
        // selections must be preserved (only file restore strips them).
        val data = parse("al_azan_settings_2.json").toRestoreData()

        val hasExternalMuezzin = data.settings.selectedAdhanEntries.values.any {
            it is AudioEntry.ExternalAudioEntry
        }
        assertTrue("expected an external muezzin selection to survive migration", hasExternalMuezzin)
    }

    @Test
    fun derivesCalculationAnglesFromMethodWhenNoOverride() {
        // Jafari method: fajr 16.0, isha 14.0, maghrib 4.0. The fixture sets no angle overrides, so the
        // migrated parameters must carry the method's canonical angles, not 0.
        val params = parse("al_azan_settings.json").toRestoreData().calculationSettings.parameters!!

        assertEquals(16.0, params.fajrAngle, 0.0001)
        assertEquals(14.0, params.ishaAngle, 0.0001)
        assertEquals(4.0, params.maghribAngle, 0.0001)
    }

    @Test
    fun keepsAngleOverrideButFillsRestFromMethod() {
        // Fixture overrides only the fajr angle (15.9); isha/maghrib must still come from the method.
        val params = parse("al_azan_settings_2.json").toRestoreData().calculationSettings.parameters!!

        assertEquals(15.9, params.fajrAngle, 0.0001)
        assertEquals(14.0, params.ishaAngle, 0.0001)
        assertEquals(4.0, params.maghribAngle, 0.0001)
    }

    @Test
    fun dropsUnsetPerPrayerPlaceholders() {
        // Fixture has a per-prayer "isha" entry with the legacy "default" id (a placeholder meaning
        // "follow the global muezzin"). It must not be migrated as a concrete per-prayer override.
        val data = parse("al_azan_settings_2.json").toRestoreData()
        val entries = data.settings.selectedAdhanEntries

        assertTrue("global default muezzin must be kept", entries.containsKey(AdhanKey.Default))
        assertTrue("genuine dhuhr override must survive", entries.containsKey(AdhanKey.Dhuhr))
        assertEquals(
            "placeholder isha entry must be dropped",
            false,
            entries.containsKey(AdhanKey.Isha),
        )
    }
}
