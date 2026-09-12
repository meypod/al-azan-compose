package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.HighLatitudeRule
import io.github.meypod.adhan_kotlin.Madhab
import io.github.meypod.adhan_kotlin.MidnightMethod
import io.github.meypod.adhan_kotlin.PrayerAdjustments
import io.github.meypod.adhan_kotlin.model.Rounding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Guards the repair for installs corrupted by the pre-key-map migration.
 *
 * The corrupt state is indistinguishable from a deliberate "Custom" selection by parameters alone, so
 * every test here is really about the gate: repair the users who lost a method, touch nobody else.
 */
class LegacyMethodHealTest {
    private fun legacyState(methodKey: String?) = OldCalculationSettingsState(
        calculationMethodKey = methodKey,
        midnightMethod = MidnightMethod.SunsetToFajr,
        fajrAdjustment = 0,
        sunriseAdjustment = 0,
        dhuhrAdjustment = 0,
        asrAdjustment = 0,
        sunsetAdjustment = 0,
        maghribAdjustment = 0,
        ishaAdjustment = 0,
        midnightAdjustment = 0,
        hijriDateAdjustment = 0,
    )

    /** Exactly what the failed lookup left behind: OTHER, every angle 0, no method adjustments. */
    private fun corruptSettings(
        parameters: CalculationParameters = CalculationParameters(),
    ) = CalculationSettings(
        locationId = "default",
        parameters = parameters,
        calculationAdjustments = CalculationAdjustments(),
        midnightMethod = MidnightMethod.SunsetToFajr,
    )

    @Test
    fun restoresTheMethodTheUserOriginallyChose() {
        val healed = corruptSettings().withLegacyMethodHealed(legacyState("MoonsightingCommittee"))

        val params = healed.parameters!!
        assertEquals(CalculationMethod.MOON_SIGHTING_COMMITTEE, params.method)
        assertEquals(18.0, params.fajrAngle, 0.0001)
        assertEquals(18.0, params.ishaAngle, 0.0001)
        // The dropped offsets are what moved Dhuhr and Maghrib, so they must come back too.
        assertEquals(5, params.methodAdjustments.dhuhr)
        assertEquals(3, params.methodAdjustments.maghrib)
        assertEquals(3, params.methodAdjustments.sunset)
    }

    @Test
    fun restoresEveryMultiWordMethodTheBrokenLookupCouldLose() {
        val expected = mapOf(
            "MuslimWorldLeague" to CalculationMethod.MUSLIM_WORLD_LEAGUE,
            "MoonsightingCommittee" to CalculationMethod.MOON_SIGHTING_COMMITTEE,
            "UmmAlQura" to CalculationMethod.UMM_AL_QURA,
            "NorthAmerica" to CalculationMethod.NORTH_AMERICA,
        )

        expected.forEach { (key, method) ->
            val healed = corruptSettings().withLegacyMethodHealed(legacyState(key))
            assertEquals(key, method, healed.parameters!!.method)
        }
    }

    /**
     * A deliberate "Custom" selection produces byte-identical parameters to the corruption. The legacy
     * key is the only thing that tells them apart, and `Custom` must be left alone.
     */
    @Test
    fun leavesDeliberateCustomSelectionUntouched() {
        val settings = corruptSettings()
        assertSame(settings, settings.withLegacyMethodHealed(legacyState("Custom")))
    }

    @Test
    fun leavesSettingsUntouchedWhenNoLegacyDataRemains() {
        val settings = corruptSettings()
        assertSame(settings, settings.withLegacyMethodHealed(null))
    }

    @Test
    fun leavesSettingsUntouchedWhenLegacyKeyIsUnknownOrMissing() {
        val settings = corruptSettings()
        assertSame(settings, settings.withLegacyMethodHealed(legacyState(null)))
        assertSame(settings, settings.withLegacyMethodHealed(legacyState("SomethingElse")))
    }

    /** A healthy install must never be rewritten, even though legacy data is still on disk. */
    @Test
    fun leavesHealthySettingsUntouched() {
        val healthy = corruptSettings(CalculationMethod.KARACHI.parameters)
        assertSame(healthy, healthy.withLegacyMethodHealed(legacyState("MoonsightingCommittee")))
    }

    /** Partially corrupt is not the signature: someone editing Custom angles must be left alone. */
    @Test
    fun leavesCustomWithEditedAnglesUntouched() {
        val edited = corruptSettings(CalculationParameters(fajrAngle = 15.0))
        assertSame(edited, edited.withLegacyMethodHealed(legacyState("MoonsightingCommittee")))
    }

    @Test
    fun leavesUnconfiguredSettingsUntouched() {
        val unconfigured = CalculationSettings()
        assertSame(unconfigured, unconfigured.withLegacyMethodHealed(legacyState("MoonsightingCommittee")))
    }

    /**
     * Choices the user could have made after the bad migration survive the repair. Reverting those would
     * trade one silent surprise for another.
     */
    @Test
    fun preservesSettingsChangedSinceTheBadMigration() {
        val corrupt = corruptSettings(
            CalculationParameters(
                madhab = Madhab.HANAFI,
                highLatitudeRule = HighLatitudeRule.TWILIGHT_ANGLE,
                rounding = Rounding.UP,
                prayerAdjustments = PrayerAdjustments(fajr = 2, isha = -4),
            ),
        )

        val params = corrupt.withLegacyMethodHealed(legacyState("MoonsightingCommittee")).parameters!!

        assertEquals(CalculationMethod.MOON_SIGHTING_COMMITTEE, params.method)
        assertEquals(Madhab.HANAFI, params.madhab)
        assertEquals(HighLatitudeRule.TWILIGHT_ANGLE, params.highLatitudeRule)
        assertEquals(Rounding.UP, params.rounding)
        assertEquals(2, params.prayerAdjustments.fajr)
        assertEquals(-4, params.prayerAdjustments.isha)
    }

    /** Running twice must change nothing the second time. */
    @Test
    fun isIdempotent() {
        val once = corruptSettings().withLegacyMethodHealed(legacyState("MoonsightingCommittee"))
        val twice = once.withLegacyMethodHealed(legacyState("MoonsightingCommittee"))
        assertEquals(once, twice)
    }
}
