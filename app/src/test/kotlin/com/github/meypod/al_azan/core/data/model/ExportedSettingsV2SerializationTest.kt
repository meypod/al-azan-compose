package com.github.meypod.al_azan.core.data.model

import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportedSettingsV2SerializationTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun sample() =
        ExportedSettingsV2(
            settings = Settings(selectedLocale = "fa", themeColor = ThemeColor.ClassicDark, is24HourFormat = false),
            calculationSettings = CalculationSettings(locationId = "default"),
            alarmSettings = AlarmSettings(preAlarmMinutesBefore = 42),
            counters = listOf(Counter(id = "fajr", count = 3, lastCount = 5)),
            reminders = emptyList(),
            favoriteLocations = listOf(
                StaticFavoriteLocation(
                    id = "home",
                    locationDetail = CalculationLocationDetail(lat = 35.7, long = 51.4, label = "Tehran"),
                ),
            ),
        )

    @Test
    fun roundTripsV2Backup() {
        val encoded = json.encodeToString(ExportedSettingsV2.serializer(), sample())
        val decoded = json.decodeFromString(ExportedSettingsV2.serializer(), encoded)
        assertEquals(sample(), decoded)
    }

    @Test
    fun usesV2StorageKeysSoFormatIsDetectable() {
        val encoded = json.encodeToString(ExportedSettingsV2.serializer(), sample())
        // Restore detection keys on these exact names being present (and absent in legacy files).
        assertTrue(encoded.contains("SETTINGS_STORAGE_V2"))
        assertTrue(encoded.contains("CALC_SETTINGS_STORAGE_V2"))
        assertTrue(encoded.contains("FAVORITE_LOCATIONS_STORAGE_V2"))
    }
}
