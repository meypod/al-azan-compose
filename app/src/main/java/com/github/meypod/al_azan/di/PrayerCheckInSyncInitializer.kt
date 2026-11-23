package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.prayer_checkin.PrayerCheckInScheduler
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.MidnightMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Reschedules prayer check-in prompt and expiration alarms whenever check-in settings,
 * calculation parameters, location, or calendar preferences change.
 */
@Singleton
class PrayerCheckInSyncInitializer @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val prayerCheckInScheduler: PrayerCheckInScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalAtomicApi::class)
    private val started = AtomicBoolean(false)

    private data class SyncKey(
        val prayerCheckInEnabled: Boolean,
        val prayerCheckInSinceMillis: Long?,
        val parameters: CalculationParameters?,
        val calculationAdjustments: CalculationAdjustments,
        val midnightMethod: MidnightMethod,
        val locationId: String?,
        val locationLat: Double?,
        val locationLong: Double?,
        val arabicCalendar: String,
    )

    @OptIn(ExperimentalAtomicApi::class)
    fun start() {
        if (!started.compareAndSet(expectedValue = false, newValue = true)) return

        scope.launch {
            combine(
                settingsRepository.data,
                alarmSettingsRepository.data,
                calculationSettingsRepository.data,
                favoriteLocationsRepository.data,
            ) { settings, alarmSettings, calc, locations ->
                val location = locations.firstOrNull { it.id == calc.locationId }?.locationDetail
                SyncKey(
                    prayerCheckInEnabled = alarmSettings.prayerCheckInEnabled,
                    prayerCheckInSinceMillis = alarmSettings.prayerCheckInSinceMillis,
                    parameters = calc.parameters,
                    calculationAdjustments = calc.calculationAdjustments,
                    midnightMethod = calc.midnightMethod,
                    locationId = calc.locationId,
                    locationLat = location?.lat,
                    locationLong = location?.long,
                    arabicCalendar = settings.selectedArabicCalendar,
                )
            }
                .distinctUntilChanged()
                .collect {
                    prayerCheckInScheduler.schedule()
                }
        }
    }
}
