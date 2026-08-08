package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.core.domain.model.calculation.withDiyanetFixApplied
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
 * What gets reset — canonical parameters, minute adjustments cleared — is
 * [com.github.meypod.al_azan.core.domain.model.calculation.withDiyanetFixApplied], shared with backup
 * restore so both routes into old settings are treated the same.
 *
 * Guarded by [com.github.meypod.al_azan.core.domain.model.settings.Settings.diyanetFixApplied], which it
 * sets on the way out. That flag is part of the user's data rather than install bookkeeping, so restoring
 * a backup from before the fix brings it back as `false` and this runs again over the restored settings.
 *
 * The user is told about it: [run] raises the notice, which [DiyanetChangeNoticePoster] then delivers.
 */
@Singleton
class DiyanetParamsMigrationRunner
@Inject
constructor(
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun run() {
        if (settingsRepository.fetch().diyanetFixApplied) return

        val current = calculationSettingsRepository.fetch()
        val fixed = current.withDiyanetFixApplied()
        // Corrected settings first, then the flag that says they are corrected: a failure in between
        // leaves the flag unset and this runs again, where the reverse would strand the old times.
        if (fixed != current) calculationSettingsRepository.update { fixed }
        settingsRepository.update {
            it.copy(
                diyanetFixApplied = true,
                // Nothing changed for a fresh install or a non-Diyanet user; there is nothing to explain.
                diyanetChangeNoticePending = it.diyanetChangeNoticePending || fixed != current,
            )
        }
    }
}
