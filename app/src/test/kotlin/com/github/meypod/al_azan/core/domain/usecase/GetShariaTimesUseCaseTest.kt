package com.github.meypod.al_azan.core.domain.usecase

import android.app.Application
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimesResult
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.needsPolarCircleResolution
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.PolarCircleResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Instant

/** Tromso, well inside the polar circle: the sun neither rises nor sets around the solstices. */
private val polarLocation = CalculationLocationDetail(lat = 69.65, long = 18.96)
private val temperateLocation = CalculationLocationDetail(lat = 35.69, long = 51.39)

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GetShariaTimesUseCaseTest {

    private val getShariaTimes = GetShariaTimesUseCase()

    private fun timesFor(
        instant: Instant,
        locationDetail: CalculationLocationDetail,
        parameters: CalculationParameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters,
    ) = getShariaTimes(
        instant = instant,
        calculationParameters = parameters,
        calculationAdjustments = CalculationAdjustments(),
        arabicCalendar = "islamic",
        locationDetail = locationDetail,
    )

    @Test
    fun `polar day with no resolution has no times instead of crashing`() {
        assertEquals(ShariaTimesResult.Unresolvable, timesFor(Instant.parse("2026-06-21T12:00:00Z"), polarLocation))
    }

    @Test
    fun `polar night with no resolution has no times instead of crashing`() {
        assertEquals(ShariaTimesResult.Unresolvable, timesFor(Instant.parse("2026-12-21T12:00:00Z"), polarLocation))
    }

    @Test
    fun `ordinary location still has times`() {
        assertNotNull(timesFor(Instant.parse("2026-06-21T12:00:00Z"), temperateLocation).timesOrNull)
    }

    /** The settings warning is a prediction, worth nothing if it disagrees with what the calculation does. */
    @Test
    fun `the polar circle warning agrees with what can be computed`() {
        val solstice = Instant.parse("2026-06-21T12:00:00Z")
        val default = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
        // Diyanet floors the day at five hours, which defines the times a polar day leaves undefined
        val takdir = CalculationMethod.TURKEY.parameters
        val polarOption = default.copy(polarCircleResolution = PolarCircleResolution.AqrabYaum)

        assertTrue(polarLocation.needsPolarCircleResolution(default))
        assertEquals(ShariaTimesResult.Unresolvable, timesFor(solstice, polarLocation, default))

        assertFalse(polarLocation.needsPolarCircleResolution(takdir))
        assertNotNull(timesFor(solstice, polarLocation, takdir).timesOrNull)

        assertFalse(polarLocation.needsPolarCircleResolution(polarOption))
        assertNotNull(timesFor(solstice, polarLocation, polarOption).timesOrNull)

        assertFalse(temperateLocation.needsPolarCircleResolution(default))
        assertNotNull(timesFor(solstice, temperateLocation, default).timesOrNull)
    }
}
