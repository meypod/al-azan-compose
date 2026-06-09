package com.github.meypod.al_azan.di

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.util.formatTime
import com.github.meypod.al_azan.core.presentation.feedback.ScheduleFeedback
import com.github.meypod.al_azan.core.presentation.feedback.ScheduleFeedbackInfo
import com.github.meypod.al_azan.reminder.ReminderScheduler
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

/** Reschedules reminders whenever the reminder list or calculation config / location changes. */
@Singleton
class ReminderSyncInitializer @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderScheduler: ReminderScheduler,
    private val scheduleFeedback: ScheduleFeedback,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(ExperimentalAtomicApi::class)
    private val started = AtomicBoolean(false)

    private data class ReminderSyncKey(
        val reminders: List<Reminder>,
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
                calculationSettingsRepository.data,
                favoriteLocationsRepository.data,
                reminderRepository.data,
            ) { settings, calc, locations, reminders ->
                val location = locations.firstOrNull { it.id == calc.locationId }?.locationDetail
                ReminderSyncKey(
                    reminders = reminders,
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
                    val outcomes = reminderScheduler.schedule()
                    // Index 0 is the initial value on app start, not a user edit — stay silent. Boot/
                    // time-change/after-fire reschedules call the scheduler directly (bypassing this
                    // flow). Only signal reminders whose next fire time actually changed.
                    if (index == 0) return@collectIndexed
                    val changed = outcomes.filter { it.changed }
                    when (changed.size) {
                        0 -> Unit

                        // One signal for a single edit; collapse a multi-reminder shift into a count so
                        // the snackbar doesn't get clobbered N times.
                        1 -> {
                            val outcome = changed.single()
                            val settings = settingsRepository.data.first()
                            scheduleFeedback.notify(
                                ScheduleFeedbackInfo.Reminder(
                                    label = outcome.label,
                                    prayer = outcome.prayer,
                                    duration = outcome.duration,
                                    durationModifier = outcome.durationModifier,
                                    formattedTime = settings.formatTime(outcome.fireTimeMs),
                                ),
                            )
                        }

                        else -> scheduleFeedback.notify(ScheduleFeedbackInfo.ReminderBatch(changed.size))
                    }
                }
        }
    }
}
