package com.github.meypod.al_azan.core.data.model.old

import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.MidnightMethod
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the legacy calculation-method key mapping. The RN app persisted PascalCase keys while
 * [CalculationMethod] entries are SCREAMING_SNAKE_CASE; a naive `valueOf(key.uppercase())` silently
 * dropped every multi-word method (MuslimWorldLeague, MoonsightingCommittee, UmmAlQura,
 * NorthAmerica) to OTHER, losing the user's selection on migration and backup restore.
 */
class OldCalculationMethodMigrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    // Independent expectation table (not the production map) covering the complete key set the
    // legacy app could persist, from al-azan/src/adhan/calculation_methods.ts.
    private val expectedMappings = mapOf(
        "Custom" to CalculationMethod.OTHER,
        "MoonsightingCommittee" to CalculationMethod.MOON_SIGHTING_COMMITTEE,
        "MuslimWorldLeague" to CalculationMethod.MUSLIM_WORLD_LEAGUE,
        "Egyptian" to CalculationMethod.EGYPTIAN,
        "Karachi" to CalculationMethod.KARACHI,
        "UmmAlQura" to CalculationMethod.UMM_AL_QURA,
        "NorthAmerica" to CalculationMethod.NORTH_AMERICA,
        "Gulf" to CalculationMethod.GULF,
        "Dubai" to CalculationMethod.DUBAI,
        "Kuwait" to CalculationMethod.KUWAIT,
        "Qatar" to CalculationMethod.QATAR,
        "Singapore" to CalculationMethod.SINGAPORE,
        "France" to CalculationMethod.FRANCE,
        "France15" to CalculationMethod.FRANCE15,
        "France18" to CalculationMethod.FRANCE18,
        "Turkey" to CalculationMethod.TURKEY,
        "Russia" to CalculationMethod.RUSSIA,
        "Jafari" to CalculationMethod.JAFARI,
        "Tehran" to CalculationMethod.TEHRAN,
        "Kemenag" to CalculationMethod.KEMENAG,
        "Algeria" to CalculationMethod.ALGERIA,
        "Brunei" to CalculationMethod.BRUNEI,
        "Tunisia" to CalculationMethod.TUNISIA,
    )

    private fun stateWith(
        methodKey: String?,
        fajrAdjustment: Int = 0,
        ishaAdjustment: Int = 0,
    ) = OldCalculationSettingsState(
        calculationMethodKey = methodKey,
        midnightMethod = MidnightMethod.SunsetToFajr,
        fajrAdjustment = fajrAdjustment,
        sunriseAdjustment = 0,
        dhuhrAdjustment = 0,
        asrAdjustment = 0,
        sunsetAdjustment = 0,
        maghribAdjustment = 0,
        ishaAdjustment = ishaAdjustment,
        midnightAdjustment = 0,
        hijriDateAdjustment = 0,
    )

    @Test
    fun mapsEveryLegacyMethodKeyToItsEnumEntry() {
        for ((legacyKey, expected) in expectedMappings) {
            assertEquals(
                "legacy key '$legacyKey' must migrate to $expected",
                expected,
                stateWith(legacyKey).getCalculationParameters().method,
            )
        }
    }

    @Test
    fun unknownAndNullKeysFallBackToOther() {
        assertEquals(CalculationMethod.OTHER, stateWith("NoSuchMethod").getCalculationParameters().method)
        assertEquals(CalculationMethod.OTHER, stateWith(null).getCalculationParameters().method)
    }

    @Test
    fun migratedMethodCarriesCanonicalAnglesAndMethodAdjustments() {
        // MuslimWorldLeague was one of the keys broken by the uppercase mapping: it fell back to
        // OTHER (0/0 angles), producing wildly wrong times. The fixture-based tests only cover
        // Jafari (single-word key), which is why this went unnoticed.
        val params = stateWith("MuslimWorldLeague").getCalculationParameters()

        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, params.method)
        assertEquals(18.0, params.fajrAngle, 0.0001)
        assertEquals(17.0, params.ishaAngle, 0.0001)
        assertEquals(1, params.methodAdjustments.dhuhr)
    }

    @Test
    fun userAdjustmentsApplyOnTopOfMigratedMethod() {
        val settings = stateWith("MuslimWorldLeague", fajrAdjustment = 5, ishaAdjustment = -3)
            .toCalculationSettings()

        val params = settings.parameters!!
        assertEquals(CalculationMethod.MUSLIM_WORLD_LEAGUE, params.method)
        // Adjustments land both in the parameters and in the calc-time source of truth.
        assertEquals(5, params.prayerAdjustments.fajr)
        assertEquals(-3, params.prayerAdjustments.isha)
        assertEquals(5, settings.calculationAdjustments.fajr)
        assertEquals(-3, settings.calculationAdjustments.isha)
    }

    @Test
    fun decodesLegacyStoreJsonWithMultiWordMethodKey() {
        // Mirrors the zustand-persist blob shape read from MMKV during first-launch migration.
        val blob = """
            {
              "state": {
                "CALCULATION_METHOD_KEY": "UmmAlQura",
                "MIDNIGHT_METHOD": "SunsetToFajr",
                "FAJR_ADJUSTMENT": 2,
                "SUNRISE_ADJUSTMENT": 0,
                "DHUHR_ADJUSTMENT": 0,
                "ASR_ADJUSTMENT": 0,
                "SUNSET_ADJUSTMENT": 0,
                "MAGHRIB_ADJUSTMENT": 0,
                "ISHA_ADJUSTMENT": 0,
                "MIDNIGHT_ADJUSTMENT": 0,
                "HIJRI_DATE_ADJUSTMENT": 0
              },
              "version": 6
            }
        """.trimIndent()

        val settings = json.decodeFromString(OldCalculationSettings.serializer(), blob)
            .state.toCalculationSettings()

        val params = settings.parameters!!
        assertEquals(CalculationMethod.UMM_AL_QURA, params.method)
        assertEquals(18.5, params.fajrAngle, 0.0001)
        assertEquals(90, params.ishaInterval)
        assertEquals(2, settings.calculationAdjustments.fajr)
    }
}
