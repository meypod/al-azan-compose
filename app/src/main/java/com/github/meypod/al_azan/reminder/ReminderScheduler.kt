package com.github.meypod.al_azan.reminder

import android.util.Log
import com.github.meypod.al_azan.core.data.audio.AudioDurationProbe
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
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
    private val audioDurationProbe: AudioDurationProbe,
) {
    private val mutex = Mutex()

    /** Per-reminder fire time of the last scheduled occurrence, to detect no-op reschedules. */
    private val lastSignatures = mutableMapOf<String, Long>()

    private companion object {
        const val TAG = "ReminderScheduler"
        const val REFIRE_GUARD_MS = 10_000L

        /** How many days ahead to search for the next matching occurrence. */
        const val SEARCH_DAYS = 8
    }

    /** A reminder that was scheduled this run, plus whether its fire time differs from before. */
    data class Outcome(
        val id: String,
        val label: String,
        val prayer: Prayer,
        val duration: Int,
        val durationModifier: Int,
        val fireTimeMs: Long,
        val changed: Boolean,
    )

    suspend fun schedule(): List<Outcome> =
        mutex.withLock {
            val settings = settingsRepository.data.first()
            val alarmSettings = alarmSettingsRepository.data.first()
            val calc = calculationSettingsRepository.data.first()
            val parameters = calc.parameters
            val location = favoriteLocationsRepository.data.first()
                .firstOrNull { it.id == calc.locationId }?.locationDetail
            val reminders = reminderRepository.data.first()

            // Prune our own past skip entries (the scheduler runs on every settings change / boot / fire,
            // so this is the reliable cleanup point); past entries are inert (now dominates) anyway.
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val livePruned = settings.skippedAlarms.filterNot {
                it is SkippedAlarm.Reminder && it.fireTimeMs <= nowMs
            }
            if (livePruned.size != settings.skippedAlarms.size) {
                settingsRepository.update { it.copy(skippedAlarms = livePruned) }
            }

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

            if (parameters == null || location == null) {
                lastSignatures.clear()
                return@withLock emptyList()
            }

            val alarmType = if (settings.useDifferentAlarmType) AlarmType.ExactAllowWhileIdle else AlarmType.AlarmClock
            val outcomes = mutableListOf<Outcome>()
            val newSignatures = mutableMapOf<String, Long>()

            for (reminder in reminders) {
                if (!reminder.enabled) continue
                val deliveredMs = settings.deliveredAlarmTimestamps[ReminderContract.notificationId(reminder.id)] ?: 0L
                // "Skip next": arm strictly after the latest skipped occurrence so a later one fires instead.
                val skippedMs = livePruned
                    .filter { it.alarmId == ReminderContract.alarmId(reminder.id) }
                    .maxOfOrNull { it.fireTimeMs } ?: 0L
                val fromMs = maxOf(nowMs, deliveredMs + REFIRE_GUARD_MS, skippedMs + REFIRE_GUARD_MS)
                val triggerMs = nextTriggerMs(reminder, fromMs, calc, settings, location) ?: continue

                val changed = lastSignatures[reminder.id] != triggerMs
                newSignatures[reminder.id] = triggerMs
                outcomes += Outcome(
                    id = reminder.id,
                    label = reminder.label,
                    prayer = reminder.prayer,
                    duration = reminder.duration,
                    durationModifier = reminder.durationModifier,
                    fireTimeMs = triggerMs,
                    changed = changed,
                )

                // Intrusive = looping/long sound OR continuous vibration; a short chime with at most a
                // single buzz is not. Drives the pre-reminder below and the Scheduled-alarms list
                // (stamped into the main alarm's extras).
                val soundEntry = reminder.sound ?: ReminderAudioEntry.DefaultReminderAudioEntry
                val vibration = reminder.vibration ?: alarmSettings.vibrationMode
                val intrusive = vibration == VibrationMode.Continuous || audioDurationProbe.isIntrusive(soundEntry)

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
                            ReminderContract.EXTRA_INTRUSIVE to intrusive.toString(),
                        ),
                    ),
                )

                // Pre-reminder ("upcoming") notification, only for intrusive reminders, unless the user
                // disabled upcoming reminders.
                if (intrusive && !alarmSettings.dontNotifyUpcoming) {
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

            // Replace the signature set so disabled/removed reminders no longer count as "unchanged"
            // (so re-enabling one surfaces feedback again).
            lastSignatures.clear()
            lastSignatures.putAll(newSignatures)

            outcomes
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
