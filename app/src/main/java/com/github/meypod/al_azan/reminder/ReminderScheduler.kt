package com.github.meypod.al_azan.reminder

import android.util.Log
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

/**
 * Schedules one alarm per enabled [Reminder] with [AlarmRepository], recomputed whenever reminders /
 * calculation config change, on boot/time change, and after each fire. Mirrors [com.github.meypod.al_azan.adhan.AdhanScheduler].
 */
@Singleton
class ReminderScheduler @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val reminderRepository: ReminderRepository,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val alarmRepository: AlarmRepository,
) {
    private val mutex = Mutex()

    private companion object {
        const val TAG = "ReminderScheduler"
        const val REFIRE_GUARD_MS = 10_000L

        /** How many days ahead to search for the next matching occurrence. */
        const val SEARCH_DAYS = 8
    }

    suspend fun schedule() =
        mutex.withLock {
            val settings = settingsRepository.data.first()
            val alarmSettings = alarmSettingsRepository.data.first()
            val calc = calculationSettingsRepository.data.first()
            val parameters = calc.parameters
            val location = favoriteLocationsRepository.data.first()
                .firstOrNull { it.id == calc.locationId }?.locationDetail
            val reminders = reminderRepository.data.first()

            // Drop any scheduled reminder alarm (main or pre) that no longer maps to an enabled reminder.
            val enabledIds = reminders.filter { it.enabled }.map { it.id }.toSet()
            alarmRepository.getScheduled()
                .map { it.id }
                .mapNotNull { id ->
                    when {
                        id.startsWith(ReminderContract.PRE_ALARM_ID_PREFIX) ->
                            id to id.removePrefix(ReminderContract.PRE_ALARM_ID_PREFIX)

                        id.startsWith(ReminderContract.ALARM_ID_PREFIX) ->
                            id to id.removePrefix(ReminderContract.ALARM_ID_PREFIX)

                        else -> null
                    }
                }
                .filter { (_, reminderId) -> reminderId !in enabledIds }
                .forEach { (id, _) -> alarmRepository.cancel(id) }

            if (parameters == null || location == null) return@withLock

            val alarmType = if (settings.useDifferentAlarmType) AlarmType.ExactAllowWhileIdle else AlarmType.AlarmClock

            for (reminder in reminders) {
                if (!reminder.enabled) continue
                val deliveredMs = settings.deliveredAlarmTimestamps[ReminderContract.notificationId(reminder.id)] ?: 0L
                val fromMs = maxOf(Clock.System.now().toEpochMilliseconds(), deliveredMs + REFIRE_GUARD_MS)
                val triggerMs = nextTriggerMs(reminder, fromMs, calc, settings, location) ?: continue

                Log.i(TAG, "Reminder ${reminder.id} (${reminder.prayer}) in ${(triggerMs - fromMs) / 1000}s")
                alarmRepository.schedule(
                    ScheduledAlarm(
                        id = ReminderContract.alarmId(reminder.id),
                        triggerAtMillis = triggerMs,
                        action = ReminderContract.ACTION_REMINDER,
                        type = alarmType,
                        extras = mapOf(
                            ReminderContract.EXTRA_REMINDER_ID to reminder.id,
                            ReminderContract.EXTRA_TIMESTAMP to triggerMs.toString(),
                        ),
                    ),
                )

                // Pre-reminder ("upcoming") notification, unless the user disabled upcoming reminders.
                if (!alarmSettings.dontNotifyUpcoming) {
                    val preMs = (triggerMs - alarmSettings.preAlarmMinutesBefore * 60_000L)
                        .coerceAtLeast(fromMs + REFIRE_GUARD_MS)
                    if (preMs < triggerMs) {
                        alarmRepository.schedule(
                            ScheduledAlarm(
                                id = ReminderContract.preAlarmId(reminder.id),
                                triggerAtMillis = preMs,
                                action = ReminderContract.ACTION_PRE_REMINDER,
                                type = alarmType,
                                extras = mapOf(
                                    ReminderContract.EXTRA_REMINDER_ID to reminder.id,
                                    ReminderContract.EXTRA_TIMESTAMP to triggerMs.toString(),
                                ),
                            ),
                        )
                    } else {
                        alarmRepository.cancel(ReminderContract.preAlarmId(reminder.id))
                    }
                } else {
                    alarmRepository.cancel(ReminderContract.preAlarmId(reminder.id))
                }
            }
        }

    private fun nextTriggerMs(
        reminder: Reminder,
        fromMs: Long,
        calc: CalculationSettings,
        settings: Settings,
        location: CalculationLocationDetail,
    ): Long? {
        val parameters = calc.parameters ?: return null
        val offsetMinutes = (reminder.duration * reminder.durationModifier).toDuration(DurationUnit.MINUTES)
        for (dayOffset in 0..SEARCH_DAYS) {
            val dayInstant = Instant.fromEpochMilliseconds(fromMs) + dayOffset.toDuration(DurationUnit.DAYS)
            val times = getShariaTimesUseCase(
                instant = dayInstant,
                calculationParameters = parameters,
                calculationAdjustments = calc.calculationAdjustments,
                arabicCalendar = settings.selectedArabicCalendar,
                locationDetail = location,
            )
            val trigger = times.forPrayer(reminder.prayer) + offsetMinutes
            val matchesDay = reminder.days?.shouldFireFor(trigger) ?: true
            if (matchesDay && trigger.toEpochMilliseconds() >= fromMs) {
                return trigger.toEpochMilliseconds()
            }
        }
        return null
    }
}
