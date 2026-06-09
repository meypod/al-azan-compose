package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.adhan.AdhanScheduler
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.util.formatTime
import com.github.meypod.al_azan.core.presentation.feedback.ScheduleFeedback
import com.github.meypod.al_azan.core.presentation.feedback.ScheduleFeedbackInfo
import io.github.meypod.adhan_kotlin.CalculationParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Reschedules the next adhan whenever the data that determines it changes: the per-prayer alarm
 * schedule, calculation config / selected location, the lunar calendar, or the alarm-type preference.
 * Started once from [com.github.meypod.al_azan.App]; boot/time changes reschedule via the receivers.
 */
@Singleton
class AdhanSyncInitializer @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val adhanScheduler: AdhanScheduler,
    private val scheduleFeedback: ScheduleFeedback,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalAtomicApi::class)
    private val started = AtomicBoolean(false)

    private data class AdhanSyncKey(
        val alarmSettings: AlarmSettings,
        val parameters: CalculationParameters?,
        val calculationAdjustments: CalculationAdjustments,
        val locationId: String?,
        val locationLat: Double?,
        val locationLong: Double?,
        val arabicCalendar: String,
        val useDifferentAlarmType: Boolean,
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
                AdhanSyncKey(
                    alarmSettings = alarmSettings,
                    parameters = calc.parameters,
                    calculationAdjustments = calc.calculationAdjustments,
                    locationId = calc.locationId,
                    locationLat = location?.lat,
                    locationLong = location?.long,
                    arabicCalendar = settings.selectedArabicCalendar,
                    useDifferentAlarmType = settings.useDifferentAlarmType,
                )
            }
                .distinctUntilChanged()
                .collectIndexed { index, _ ->
                    val outcome = adhanScheduler.schedule()
                    // Index 0 is the initial value on app start (and restored settings), not a user
                    // edit — don't surface feedback for it. Boot/time-change/after-fire reschedules
                    // call the scheduler directly (bypassing this flow), so they stay silent too.
                    // Only signal when the next adhan (prayer + fire time) actually changed.
                    if (index > 0 && outcome != null && outcome.changed) {
                        val time = settingsRepository.data.first()
                            .formatTime(outcome.next.prayerTime.toEpochMilliseconds())
                        scheduleFeedback.notify(ScheduleFeedbackInfo.Adhan(outcome.next.prayer, time))
                    }
                }
        }
    }
}
