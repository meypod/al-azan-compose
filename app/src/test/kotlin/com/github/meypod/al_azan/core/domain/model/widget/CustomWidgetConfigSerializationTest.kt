package com.github.meypod.al_azan.core.domain.model.widget

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import junit.framework.TestCase.assertEquals
import kotlinx.serialization.json.Json
import org.junit.Test

class CustomWidgetConfigSerializationTest {
    // Mirrors the @Named("storage") Json used by MMKVDataStore so the test exercises real persistence.
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private fun roundTrip(config: CustomWidgetConfig): CustomWidgetConfig {
        val encoded = json.encodeToString(CustomWidgetConfig.serializer(), config)
        return json.decodeFromString(CustomWidgetConfig.serializer(), encoded)
    }

    @Test
    fun roundTripsDefaults() {
        assertEquals(CustomWidgetConfig(), roundTrip(CustomWidgetConfig()))
    }

    @Test
    fun roundTripsColorsAndLayout() {
        val config = CustomWidgetConfig(
            bgColor = 0x80112233.toInt(),
            textColor = 0xFFEEDDCC.toInt(),
            highlightColor = null,
            rows = listOf(listOf(Prayer.Isha, Prayer.Fajr), listOf(Prayer.Sunrise)),
            showCountdown = true,
            locationIds = listOf("home", "traveling_mode"),
        )
        assertEquals(config, roundTrip(config))
    }

    @Test
    fun roundTripsHeaderBlocks() {
        val config = CustomWidgetConfig(
            topStart = HeaderBlock.Date(calendar = DateCalendar.Hijri, withDayName = true),
            topEnd = HeaderBlock.LocationName,
        )
        val decoded = roundTrip(config)
        assertEquals(config.topStart, decoded.topStart)
        assertEquals(HeaderBlock.LocationName, decoded.topEnd)
    }

    /** Row arrangement is user-meaningful (drag order) and must survive serialization exactly. */
    @Test
    fun preservesRowArrangement() {
        val rows = listOf(listOf(Prayer.Maghrib, Prayer.Fajr), listOf(Prayer.Dhuhr))
        assertEquals(rows, roundTrip(CustomWidgetConfig(rows = rows)).rows)
    }

    /** Older/newer stored JSON must decode: unknown keys ignored, missing keys fall back to defaults. */
    @Test
    fun toleratesUnknownAndMissingKeys() {
        val raw = """{"bgColor":42,"someFutureField":true}"""
        val decoded = json.decodeFromString(CustomWidgetConfig.serializer(), raw)
        assertEquals(42, decoded.bgColor)
        // Default is a single empty row (no preset prayers) so a fresh widget shows the set-up hint.
        assertEquals(listOf(emptyList<Prayer>()), decoded.rows)
    }
}
