package com.github.meypod.al_azan.reminder

import android.util.Log
import com.github.meypod.al_azan.core.data.audio.AudioDurationProbe
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSchedulingDefaults
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.alarm.isReminderSkipped
import com.github.meypod.al_azan.core.domain.model.alarm.prunePastDays
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.toLocalDate
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
    private val notificationRepository: NotificationRepository,
    private val audioDurationProbe: AudioDurationProbe,
    private val swedishDownloader: com.github.meypod.al_azan.core.data.network.SwedishDownloader,
) {
    private val mutex = Mutex()

    /** Per-reminder fire time of the last scheduled occurrence, to detect no-op reschedules. */
    private val lastSignatures = mutableMapOf<String, Long>()

    private companion object {
        const val TAG = "ReminderScheduler"
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

            // Prune our own past-day skip entries (the scheduler runs on every settings change / boot /
            // fire, so this is the reliable cleanup point); past-day entries can never match anyway.
            val nowMs = Clock.System.now().toEpochMilliseconds()
            val today = Instant.fromEpochMilliseconds(nowMs).toLocalDate()
            val livePruned = settings.skippedOccurrences.prunePastDays<SkippedAlarm.Reminder>(today)
            if (livePruned.size != settings.skippedOccurrences.size) {
                settingsRepository.update { it.copy(skippedOccurrences = livePruned) }
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
            // Ensure IFIS data is pre-fetched for offline/background support
            if (calc.swedishCityId != null) {
                val calendar = java.util.Calendar.getInstance()
                val currentYear = calendar.get(java.util.Calendar.YEAR)
                val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
                // Prefetch current month
                swedishDownloader.getPrayerTimes(calc.swedishCityId, currentMonth, currentYear)
                // Prefetch next month if we are near the end of the month
                val nextMonth = if (currentMonth == 12) 1 else currentMonth + 1
                val nextYear = if (currentMonth == 12) currentYear + 1 else currentYear
                swedishDownloader.getPrayerTimes(calc.swedishCityId, nextMonth, nextYear)
            }
            val alarmType = AlarmSchedulingDefaults.alarmType(settings.useDifferentAlarmType)
            val outcomes = mutableListOf<Outcome>()
            val newSignatures = mutableMapOf<String, Long>()

            for (reminder in reminders) {
                if (!reminder.enabled) continue
                val deliveredMs = settings.deliveredAlarmTimestamps[ReminderContract.notificationId(reminder.id)] ?: 0L
                val fromMs = maxOf(nowMs, deliveredMs + AlarmSchedulingDefaults.REFIRE_GUARD_MS)
                // "Skip": pass over any occurrence the user skipped (logical (reminderId, date) match) so
                // the next non-skipped day fires instead — no time-based floor, survives re-calcs.
                val triggerMs = nextTriggerMs(reminder, fromMs, calc, settings, location, livePruned) ?: continue

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

                // Pre-reminder ("upcoming") notification: fire once per reminder occurrence for intrusive
                // reminders, unless the user disabled upcoming reminders. Same design as the adhan pre-alarm
                // (see issue #27): arm it for its natural time (trigger − offset) when that is still ahead,
                // else arm it for now so it still fires once. [onPreReminderFired] marks it delivered for this
                // occurrence via [deliveredAlarmTimestamps], so once fired later reschedules don't re-post it.
                val preDeliveredForMs = settings.deliveredAlarmTimestamps[ReminderContract.preNotificationId(reminder.id)]
                if (!intrusive || alarmSettings.dontNotifyUpcoming) {
                    // No longer intrusive (e.g. the reminder's sound turned off), or upcoming reminders
                    // disabled: drop the pending pre-alarm AND any notice on screen, and clear its delivered
                    // mark so re-enabling shows the heads-up again — a torn-down pre must behave like a
                    // rescheduled one. See issue #27.
                    alarmRepository.cancel(ReminderContract.preAlarmId(reminder.id))
                    notificationRepository.cancelNotification(ReminderContract.preNotificationId(reminder.id))
                    if (preDeliveredForMs != null) settingsRepository.clearDelivered(ReminderContract.preNotificationId(reminder.id))
                } else if (preDeliveredForMs != triggerMs) {
                    val preMs = (triggerMs - alarmSettings.preAlarmMinutesBefore * 60_000L)
                        .coerceAtLeast(nowMs)
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
                }
                // else: already delivered for this occurrence — leave the visible notice up (it clears when
                // the reminder fires) and leave the spent pre-alarm entry to be replaced next occurrence.
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
        skipped: List<SkippedAlarm>,
    ): Long? {
        val parameters = calc.parameters ?: return null
        val offsetMinutes = (reminder.duration * reminder.durationModifier).toDuration(DurationUnit.MINUTES)
        for (dayOffset in 0..AlarmSchedulingDefaults.SEARCH_DAYS) {
            val dayInstant = Instant.fromEpochMilliseconds(fromMs) + dayOffset.toDuration(DurationUnit.DAYS)
            val times = getShariaTimesUseCase(
                instant = dayInstant,
                calculationParameters = parameters,
                calculationAdjustments = calc.calculationAdjustments,
                arabicCalendar = settings.selectedArabicCalendar,
                locationDetail = location,
                swedishCityId = calc.swedishCityId,
            )
            val trigger = times.forPrayer(reminder.prayer) + offsetMinutes
            val matchesDay = reminder.days?.shouldFireFor(trigger) ?: true
            val isSkipped = skipped.isReminderSkipped(reminder.id, trigger.toLocalDate())
            if (matchesDay && !isSkipped && trigger.toEpochMilliseconds() >= fromMs) {
                return trigger.toEpochMilliseconds()
            }
        }
        return null
    }
}
