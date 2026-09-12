package com.github.meypod.al_azan.core.data.model.old

import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.Madhab
import io.github.meypod.adhan_kotlin.PolarCircleResolution
import io.github.meypod.adhan_kotlin.model.Shafaq
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regression test built from a real affected user's exported legacy data.
 *
 * They upgraded from the React-Native app and were left with Isha computed *before* Maghrib. The cause
 * was the pre-`b688ffd` method lookup, `CalculationMethod.valueOf(key.uppercase())`: the stored key
 * `MoonsightingCommittee` uppercases to `MOONSIGHTINGCOMMITTEE`, which is not the
 * `MOON_SIGHTING_COMMITTEE` constant, so it threw and fell back to [CalculationMethod.OTHER]. Because
 * the code then starts from `method.parameters`, both angles became 0 and the method's built-in offsets
 * were dropped.
 *
 * This is their `CALC_SETTINGS_STORAGE` blob as exported, less the stored coordinates, which have no
 * bearing on the mapping under test.
 */
class LegacyExportedSettingsMigrationTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val exportedBlob = """
        {
          "state": {
            "CALCULATION_METHOD_KEY": "MoonsightingCommittee",
            "ASR_CALCULATION": "shafi",
            "SHAFAQ": "general",
            "POLAR_RESOLUTION": "Unresolved",
            "MIDNIGHT_METHOD": "SunsetToFajr",
            "FAJR_ADJUSTMENT": 0,
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

    private fun decoded() =
        json.decodeFromString(OldCalculationSettings.serializer(), exportedBlob).state

    /**
     * The defect in one assertion: the persisted key does not name any enum constant, so the old
     * `valueOf` lookup could only ever throw for this user.
     */
    @Test
    fun legacyKeyNeverMatchedAnEnumConstantName() {
        val key = decoded().calculationMethodKey
        assertEquals("MoonsightingCommittee", key)
        assertEquals("MOONSIGHTINGCOMMITTEE", key!!.uppercase())
        assertNull(CalculationMethod.entries.firstOrNull { it.name == key.uppercase() })
    }

    /**
     * With the explicit key map the user's method survives, along with the angles and the built-in
     * offsets. Those offsets move Dhuhr and Maghrib independently of the angles, which is why losing the
     * method shifted five times rather than two.
     */
    @Test
    fun exportedSettingsMigrateToMoonsightingCommittee() {
        val params = decoded().getCalculationParameters()

        assertEquals(CalculationMethod.MOON_SIGHTING_COMMITTEE, params.method)
        assertEquals(18.0, params.fajrAngle, 0.0001)
        assertEquals(18.0, params.ishaAngle, 0.0001)
        assertEquals(0, params.ishaInterval)
        assertEquals(0.0, params.maghribAngle, 0.0001)

        assertEquals(5, params.methodAdjustments.dhuhr)
        assertEquals(3, params.methodAdjustments.maghrib)
        assertEquals(3, params.methodAdjustments.sunset)
    }

    /** Every other legacy field the user had set must survive the migration unchanged. */
    @Test
    fun remainingLegacyFieldsSurvive() {
        val params = decoded().getCalculationParameters()

        assertEquals(Madhab.SHAFI, params.madhab)
        assertEquals(Shafaq.GENERAL, params.shafaq)
        assertEquals(PolarCircleResolution.Unresolved, params.polarCircleResolution)
        // No HIGH_LATITUDE_RULE was stored, so it stays unset and renders as "None (Automatic)".
        assertNull(params.highLatitudeRule)
    }

    /** The user had no manual offsets; nothing may invent any. */
    @Test
    fun noAdjustmentsAreIntroduced() {
        val settings = decoded().toCalculationSettings()
        val adjustments = settings.calculationAdjustments

        assertEquals(0, adjustments.fajr)
        assertEquals(0, adjustments.sunrise)
        assertEquals(0, adjustments.dhuhr)
        assertEquals(0, adjustments.asr)
        assertEquals(0, adjustments.maghrib)
        assertEquals(0, adjustments.sunset)
        assertEquals(0, adjustments.isha)
        assertEquals(0, adjustments.midnight)
        assertEquals(0, adjustments.hijriDate)
    }

    /**
     * What the broken release produced, kept as an explicit fixture so the corrupt signature the repair
     * must recognise is written down rather than inferred.
     */
    @Test
    fun brokenFallbackSignatureIsAllZeroWithNoMethodAdjustments() {
        val corrupt = CalculationMethod.OTHER.parameters

        assertEquals(0.0, corrupt.fajrAngle, 0.0001)
        assertEquals(0.0, corrupt.ishaAngle, 0.0001)
        assertEquals(0, corrupt.ishaInterval)
        assertEquals(0.0, corrupt.maghribAngle, 0.0001)
        assertEquals(0, corrupt.methodAdjustments.dhuhr)
        assertEquals(0, corrupt.methodAdjustments.maghrib)
        assertEquals(0, corrupt.methodAdjustments.sunset)
    }
}
