package com.github.meypod.al_azan.main.upcoming_alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.adhan.AdhanScheduler
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.upsert
import com.github.meypod.al_azan.core.domain.model.alarm.withoutFrom
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.playback.PlaybackService
import com.github.meypod.al_azan.reminder.ReminderContract
import com.github.meypod.al_azan.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock

/**
 * Lists the intrusive adhan + reminder alarms relevant to the user, sorted by fire time:
 *  - **active** rows come from the alarms currently armed with [AlarmRepository] (the genuine next
 *    firing of each stream) and offer a Skip action;
 *  - **skipped** rows are synthesized from [com.github.meypod.al_azan.core.domain.model.settings.Settings.skippedAlarms]
 *    (occurrences the user skipped) and offer a Reschedule (undo) action.
 *
 * Skipping appends a [SkippedAlarm] and re-runs the owning scheduler, which arms the *following*
 * occurrence — so the skipped row stays and the next firing appears as a new active row. Rescheduling
 * drops the entry and re-runs the scheduler to re-arm the original next firing.
 */
@HiltViewModel
class UpcomingAlarmsViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderRepository: ReminderRepository,
    private val adhanScheduler: AdhanScheduler,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpcomingAlarmsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.data,
                reminderRepository.data,
                // Reacts to alarms (re)scheduled or cleared anywhere in-process: external fires, boot
                // reconciliation, settings-driven reschedules — not just this screen's own actions.
                alarmRepository.data,
            ) { settings, reminders, scheduled ->
                Triple(settings, reminders, scheduled)
            }.collect { (settings, reminders, scheduled) ->
                val now = Clock.System.now().toEpochMilliseconds()

                // Display filter only — persistence-side pruning is owned by the schedulers, so this
                // screen never writes settings just to be viewed.
                val live = settings.skippedAlarms.filter { it.fireTimeMs > now }

                // An armed alarm whose (id, fireTime) is already recorded as skipped is the very
                // occurrence being skipped: suppress it so it never shows as both active and skipped
                // in the window between persisting the skip and the scheduler re-arming the next firing.
                val skippedKeys = live.map { it.alarmId to it.fireTimeMs }.toSet()
                val remindersById = reminders.associateBy { it.id }
                val active = scheduled
                    .filter { it.isIntrusive() }
                    .filterNot { (it.id to it.triggerAtMillis) in skippedKeys }
                    .map { alarm ->
                        when (alarm.action) {
                            AdhanContract.ACTION_ADHAN -> UpcomingAlarmUi(
                                id = alarm.id,
                                isAdhan = true,
                                prayer = alarm.extras[PlaybackService.EXTRA_PRAYER]?.let(::prayerOrNull),
                                reminderLabel = null,
                                fireTimeMs = alarm.triggerAtMillis,
                                skipped = false,
                            )

                            else -> {
                                val reminder = remindersById[alarm.extras[ReminderContract.EXTRA_REMINDER_ID]]
                                UpcomingAlarmUi(
                                    id = alarm.id,
                                    isAdhan = false,
                                    prayer = reminder?.prayer,
                                    reminderLabel = reminder?.label,
                                    fireTimeMs = alarm.triggerAtMillis,
                                    skipped = false,
                                    reminderDuration = reminder?.duration ?: 0,
                                    reminderDurationModifier = reminder?.durationModifier ?: 0,
                                )
                            }
                        }
                    }

                val skipped = live.map { entry ->
                    when (entry) {
                        is SkippedAlarm.Adhan -> UpcomingAlarmUi(
                            id = entry.alarmId,
                            isAdhan = true,
                            prayer = entry.prayer,
                            reminderLabel = null,
                            fireTimeMs = entry.fireTimeMs,
                            skipped = true,
                        )

                        is SkippedAlarm.Reminder -> UpcomingAlarmUi(
                            id = entry.alarmId,
                            isAdhan = false,
                            prayer = entry.prayer,
                            reminderLabel = entry.label,
                            fireTimeMs = entry.fireTimeMs,
                            skipped = true,
                            reminderDuration = entry.duration,
                            reminderDurationModifier = entry.durationModifier,
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        alarms = (active + skipped).sortedBy { a -> a.fireTimeMs },
                        loading = false,
                        locale = settings.selectedLocale,
                        is24Hour = settings.is24HourFormat,
                        numberingSystem = settings.numberingSystem,
                        nowMs = now,
                    )
                }
            }
        }
    }

    fun onAction(action: UpcomingAlarmsUiAction) {
        when (action) {
            is UpcomingAlarmsUiAction.OnSkip -> onSkip(action.id)
            is UpcomingAlarmsUiAction.OnReschedule -> onReschedule(action.id, action.fireTimeMs)
        }
    }

    private fun onSkip(id: String) {
        viewModelScope.launch {
            val armed = alarmRepository.getScheduled().firstOrNull { it.id == id } ?: return@launch
            val entry: SkippedAlarm = if (armed.action == AdhanContract.ACTION_ADHAN) {
                SkippedAlarm.Adhan(
                    alarmId = id,
                    fireTimeMs = armed.triggerAtMillis,
                    prayer = armed.extras[PlaybackService.EXTRA_PRAYER]?.let(::prayerOrNull),
                )
            } else {
                val reminder = reminderRepository.fetch()
                    .firstOrNull { it.id == armed.extras[ReminderContract.EXTRA_REMINDER_ID] }
                SkippedAlarm.Reminder(
                    alarmId = id,
                    fireTimeMs = armed.triggerAtMillis,
                    prayer = reminder?.prayer,
                    label = reminder?.label,
                    duration = reminder?.duration ?: 0,
                    durationModifier = reminder?.durationModifier ?: 0,
                )
            }
            settingsRepository.update { it.copy(skippedAlarms = it.skippedAlarms.upsert(entry)) }
            reschedule(id)
        }
    }

    private fun onReschedule(
        id: String,
        fireTimeMs: Long,
    ) {
        viewModelScope.launch {
            // Undo this skip and any later skip on the same stream: re-arming at the rescheduled
            // occurrence makes every occurrence after it moot, so those rows disappear too.
            settingsRepository.update { it.copy(skippedAlarms = it.skippedAlarms.withoutFrom(id, fireTimeMs)) }
            reschedule(id)
        }
    }

    /** Re-runs the scheduler that owns [id] so it re-arms honoring the current skip entries. */
    private suspend fun reschedule(id: String) {
        if (id == AdhanContract.ADHAN_ALARM_ID) {
            adhanScheduler.schedule()
        } else {
            reminderScheduler.schedule()
        }
    }

    private fun ScheduledAlarm.isIntrusive(): Boolean =
        (action == AdhanContract.ACTION_ADHAN && extras[AdhanContract.EXTRA_INTRUSIVE] == "true") ||
            (action == ReminderContract.ACTION_REMINDER && extras[ReminderContract.EXTRA_INTRUSIVE] == "true")

    private fun prayerOrNull(name: String): Prayer? = runCatching { Prayer.valueOf(name) }.getOrNull()
}
