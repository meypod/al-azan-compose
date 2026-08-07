package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import io.github.meypod.adhan_kotlin.CalculationMethod
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time migration for users who selected [CalculationMethod.TURKEY] before adhan 0.0.13.
 *
 * Picking a method stores a snapshot of its parameters, so an upgrade alone does not pick up the new
 * Diyanet defaults — the takdir ([io.github.meypod.adhan_kotlin.HighLatitudeRule.PROPORTIONAL_DEPRESSION])
 * and the declination held at 0h — and those users would keep the pre-0.0.13 times, which were off by
 * as much as 68 minutes away from Turkey.
 *
 * Everything the method itself defines is reset to canonical, including a high latitude rule the user
 * had picked: the old rules were how people worked around the wrong times, and keeping one now would
 * fight the takdir rather than correct anything. The per-prayer minute adjustments are cleared for the
 * same reason — they were compensation for an error that no longer exists, and leaving them in place
 * would turn a fix into a new offset. The hijri date adjustment is left alone; it is a day offset on
 * the calendar and has nothing to do with the calculation.
 *
 * The user is told about all of this: [run] flags the notice, which [DiyanetChangeNoticePoster] then
 * delivers.
 */
@Singleton
class DiyanetParamsMigrationRunner
@Inject
constructor(
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun run() {
        val current = calculationSettingsRepository.fetch()
        val params = current.parameters ?: return
        if (params.method != CalculationMethod.TURKEY) return

        val canonical = params.method.parameters
        val migrated = current.copy(
            parameters = params.copy(
                highLatitudeRule = canonical.highLatitudeRule,
                interpolateDeclination = canonical.interpolateDeclination,
            ),
            // keeps hijriDate, which the user set against the calendar rather than against the times
            calculationAdjustments = CalculationAdjustments(hijriDate = current.calculationAdjustments.hijriDate),
        )
        if (migrated == current) return

        calculationSettingsRepository.update { migrated }
        settingsRepository.update { it.copy(diyanetChangeNoticePending = true) }
    }
}
