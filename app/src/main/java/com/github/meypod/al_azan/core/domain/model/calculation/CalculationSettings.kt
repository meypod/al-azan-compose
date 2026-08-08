package com.github.meypod.al_azan.core.domain.model.calculation

import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.MidnightMethod
import kotlinx.serialization.Serializable

@Serializable
data class CalculationSettings(
    val locationId: String? = null,
    val parameters: CalculationParameters? = null,
    val calculationAdjustments: CalculationAdjustments = CalculationAdjustments(),
    val midnightMethod: MidnightMethod = MidnightMethod.SunsetToFajr,
)

/**
 * The adhan 0.0.13 Diyanet fix, applied to settings of unknown vintage: the method's parameters restored
 * wholesale, and the per-prayer minute adjustments cleared.
 *
 * Selecting a method stores a *snapshot* of its parameters, so parameters upgraded from an older release,
 * or restored from a backup taken before one, can carry the pre-0.0.13 values and silently reinstate times
 * that were off by as much as 68 minutes away from Turkey.
 *
 * Everything a Diyanet user had diverged from canonical is treated as a workaround for those wrong times,
 * because that is overwhelmingly what it was: the angles pulled Fajr and Isha back towards the published
 * calendars, the high latitude rule stood in for the takdir the method now has, and the polar resolution
 * was the only way to get times at all inside the polar circle, where `TURKEY` used to throw. The minute
 * adjustments go for the same reason — kept on top of times that are now correct, they push them off again
 * by as much as they used to pull them on. Clobbering all of it is the point of the fix, not a side effect.
 *
 * Two things survive. The madhab, because Asr was never wrong: it sat 0.7 minutes off before the fix, so
 * nobody moved it an hour to compensate, and resetting it would silently shift a Hanafi user's Asr. And the
 * hijri date adjustment, which is a day offset on the calendar rather than on the times.
 *
 * A no-op for every other method.
 */
fun CalculationSettings.withDiyanetFixApplied(): CalculationSettings {
    val current = parameters ?: return this
    if (current.method != CalculationMethod.TURKEY && current.method != CalculationMethod.TURKEY_EUROPE) return this
    return copy(
        parameters = current.method.parameters.copy(madhab = current.madhab),
        calculationAdjustments = CalculationAdjustments(hijriDate = calculationAdjustments.hijriDate),
    )
}
