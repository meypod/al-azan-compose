package com.github.meypod.al_azan.core.domain.model.calculation

import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.HighLatitudeRule
import io.github.meypod.adhan_kotlin.Madhab
import io.github.meypod.adhan_kotlin.PolarCircleResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Guards the adhan 0.0.13 Diyanet fix. Both the things it does are load-bearing and both are silent when
 * missed: stale parameters drift the times by up to an hour outside Turkey, and minute adjustments kept
 * from before the fix push the corrected times back off by as much as they used to pull them on.
 */
class DiyanetFixTest {

    /** What adhan 0.0.12 stored for [CalculationMethod.TURKEY]. */
    private val preUpgradeTurkeyParams = CalculationMethod.TURKEY.parameters.copy(
        highLatitudeRule = null,
        interpolateDeclination = true,
    )

    @Test
    fun `old Diyanet parameters regain the takdir and the held declination`() {
        val fixed = CalculationSettings(parameters = preUpgradeTurkeyParams).withDiyanetFixApplied()

        assertEquals(HighLatitudeRule.PROPORTIONAL_DEPRESSION, fixed.parameters?.highLatitudeRule)
        assertFalse(fixed.parameters!!.interpolateDeclination)
        assertEquals(CalculationMethod.TURKEY.parameters, fixed.parameters)
    }

    @Test
    fun `a high latitude rule from the old workarounds is replaced`() {
        val settings = CalculationSettings(
            parameters = preUpgradeTurkeyParams.copy(highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT),
        )

        val fixed = settings.withDiyanetFixApplied()

        assertEquals(HighLatitudeRule.PROPORTIONAL_DEPRESSION, fixed.parameters?.highLatitudeRule)
    }

    @Test
    fun `minute adjustments are cleared and the hijri day offset is kept`() {
        val settings = CalculationSettings(
            parameters = preUpgradeTurkeyParams,
            calculationAdjustments = CalculationAdjustments(
                fajr = -3,
                sunrise = 2,
                dhuhr = 1,
                asr = -1,
                maghrib = 4,
                sunset = 4,
                isha = -6,
                midnight = 5,
                tahajjud = -5,
                hijriDate = 1,
            ),
        )

        val fixed = settings.withDiyanetFixApplied()

        assertEquals(CalculationAdjustments(hijriDate = 1), fixed.calculationAdjustments)
    }

    /**
     * The first-launch migration from the old app already writes canonical parameters but keeps that app's
     * adjustments, tuned against the same wrong times. Clearing has to happen on the method, not on whether
     * the parameters happen to look stale.
     */
    @Test
    fun `canonical parameters still get their adjustments cleared`() {
        val settings = CalculationSettings(
            parameters = CalculationMethod.TURKEY.parameters,
            calculationAdjustments = CalculationAdjustments(fajr = -3, hijriDate = 1),
        )

        val fixed = settings.withDiyanetFixApplied()

        assertEquals(CalculationAdjustments(hijriDate = 1), fixed.calculationAdjustments)
    }

    /**
     * Angles are how people pulled the old Fajr and Isha back towards the published calendars, so they go
     * with the rest of the workaround. Someone who set Isha to 16° was hand-building
     * [CalculationMethod.TURKEY_EUROPE], which now exists as a method of its own.
     */
    @Test
    fun `angles the user had tuned are reset`() {
        val settings = CalculationSettings(
            parameters = preUpgradeTurkeyParams.copy(fajrAngle = 19.5, ishaAngle = 16.0),
        )

        val params = settings.withDiyanetFixApplied().parameters!!

        assertEquals(CalculationMethod.TURKEY.parameters.fajrAngle, params.fajrAngle, 0.0)
        assertEquals(CalculationMethod.TURKEY.parameters.ishaAngle, params.ishaAngle, 0.0)
    }

    /**
     * Inside the polar circle the old `TURKEY` threw outright, so a northern user had to change this to get
     * any times at all — a workaround for a defect the method now handles itself.
     */
    @Test
    fun `a polar circle resolution picked to work around the old method is reset`() {
        val settings = CalculationSettings(
            parameters = preUpgradeTurkeyParams.copy(polarCircleResolution = PolarCircleResolution.AqrabBalad),
        )

        val params = settings.withDiyanetFixApplied().parameters!!

        assertEquals(CalculationMethod.TURKEY.parameters.polarCircleResolution, params.polarCircleResolution)
    }

    /**
     * Asr was 0.7 minutes off before the fix, so nobody moved it by an hour to compensate. Resetting the
     * madhab would silently shift a Hanafi user's Asr rather than correct anything.
     */
    @Test
    fun `the madhab is the one choice kept`() {
        val settings = CalculationSettings(parameters = preUpgradeTurkeyParams.copy(madhab = Madhab.HANAFI))

        val params = settings.withDiyanetFixApplied().parameters!!

        assertEquals(Madhab.HANAFI, params.madhab)
        // ...and everything else is still canonical
        assertEquals(CalculationMethod.TURKEY.parameters.copy(madhab = Madhab.HANAFI), params)
    }

    @Test
    fun `everything outside the calculation is left as it was`() {
        val settings = CalculationSettings(locationId = "somewhere", parameters = preUpgradeTurkeyParams)

        val fixed = settings.withDiyanetFixApplied()

        assertEquals(settings.locationId, fixed.locationId)
        assertEquals(settings.midnightMethod, fixed.midnightMethod)
    }

    @Test
    fun `the Europe variant is covered too`() {
        val settings = CalculationSettings(
            parameters = CalculationMethod.TURKEY_EUROPE.parameters.copy(
                highLatitudeRule = null,
                interpolateDeclination = true,
            ),
        )

        assertEquals(CalculationMethod.TURKEY_EUROPE.parameters, settings.withDiyanetFixApplied().parameters)
    }

    @Test
    fun `other methods keep their rule and their adjustments`() {
        val settings = CalculationSettings(
            parameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
                .copy(highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT),
            calculationAdjustments = CalculationAdjustments(fajr = -3),
        )

        assertSame(settings, settings.withDiyanetFixApplied())
    }

    @Test
    fun `settings without parameters pass through`() {
        val settings = CalculationSettings()

        assertSame(settings, settings.withDiyanetFixApplied())
    }

    @Test
    fun `applying it twice changes nothing further`() {
        val once = CalculationSettings(
            parameters = preUpgradeTurkeyParams,
            calculationAdjustments = CalculationAdjustments(fajr = -3, hijriDate = 1),
        ).withDiyanetFixApplied()

        assertEquals(once, once.withDiyanetFixApplied())
    }
}
