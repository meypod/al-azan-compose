package com.github.meypod.al_azan.di

import android.util.Log
import com.github.meypod.al_azan.core.data.model.old.LegacyCalculationSettingsReader
import com.github.meypod.al_azan.core.data.model.old.withLegacyMethodHealed
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * One-time repair for installs whose first-launch migration resolved the calculation method with
 * `CalculationMethod.valueOf(key.uppercase())`, before the explicit key map replaced it.
 *
 * Those users kept a Fajr and Isha angle of 0 and lost the method's built-in adjustments, which puts
 * Fajr after sunrise and Isha before maghrib. Fixing the migration only helped installs that had not
 * migrated yet: it runs once, and its guard flag is written even when it throws, so nothing re-reads the
 * legacy stores afterwards.
 *
 * The repair is exact rather than a guess. The migration never deletes the legacy MMKV stores, so the
 * user's original method key is still there to be read back. What gets replaced is
 * [com.github.meypod.al_azan.core.data.model.old.withLegacyMethodHealed].
 *
 * Guarded by [com.github.meypod.al_azan.core.domain.model.settings.Settings.legacyMethodHealApplied],
 * which it sets on the way out. The flag lives in the user's data rather than in install bookkeeping, so
 * restoring a backup taken while the settings were corrupt brings it back as `false` and the repair runs
 * again over the restored values.
 */
@Singleton
class LegacyMethodHealRunner
@Inject
constructor(
    private val legacyCalculationSettingsReader: LegacyCalculationSettingsReader,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun run() {
        if (settingsRepository.fetch().legacyMethodHealApplied) return

        val current = calculationSettingsRepository.fetch()
        val healed = current.withLegacyMethodHealed(legacyCalculationSettingsReader.read())
        // Corrected settings first, then the flag that says they are corrected: a failure in between
        // leaves the flag unset and this runs again, where the reverse would strand the wrong times.
        if (healed != current) calculationSettingsRepository.update { healed }
        settingsRepository.update { it.copy(legacyMethodHealApplied = true) }
    }
}

/**
 * Runs [LegacyMethodHealRunner] off the main thread at startup.
 *
 * A failure is logged and left for the next launch: the flag is only set on a successful pass, so a
 * transient problem cannot silently cost a user the repair.
 */
@Singleton
class LegacyMethodHealInitializer
@Inject
constructor(
    private val legacyMethodHealRunner: LegacyMethodHealRunner,
) {
    private companion object {
        const val TAG = "LegacyMethodHealInitializer"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalAtomicApi::class)
    private val started = AtomicBoolean(false)

    @OptIn(ExperimentalAtomicApi::class)
    fun start() {
        if (!started.compareAndSet(expectedValue = false, newValue = true)) return
        scope.launch {
            runCatching { legacyMethodHealRunner.run() }
                .onFailure { Log.e(TAG, "Legacy method heal failed. Will retry on next launch.", it) }
        }
    }
}
