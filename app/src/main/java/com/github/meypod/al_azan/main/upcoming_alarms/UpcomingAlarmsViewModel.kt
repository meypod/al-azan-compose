package com.github.meypod.al_azan.main.upcoming_alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.upsert
import com.github.meypod.al_azan.core.domain.model.alarm.without
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationSettings
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import com.github.meypod.al_azan.playback.PlaybackService
import com.github.meypod.al_azan.reminder.ReminderContract
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Lists every intrusive adhan + reminder occurrence in the upcoming window (now → end of tomorrow),
 * sorted by fire time — the full schedule, not just the single next firing each scheduler arms. The
 * list is **derived** from prayer times + settings via [GetUpcomingIntrusiveAlarmsUseCase], not read
 * from the platform scheduler, so it shows occurrences that aren't concretely armed yet.
 *
 * Skip/Reschedule only mutate [com.github.meypod.al_azan.core.domain.model.settings.Settings.skippedOccurrences]
 * (a logical [SkippedAlarm]); the adhan/reminder sync initializers observe that change, re-arm the next
 * firing, and emit the "next … scheduled" feedback. The one side effect kept here is dismissing a live
 * pre-alarm heads-up, which is tied to the user's tap (the sync flow can't know a skip just happened).
 */
@HiltViewModel
class UpcomingAlarmsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val reminderRepository: ReminderRepository,
    private val notificationRepository: NotificationRepository,
    private val alarmRepository: AlarmRepository,
    private val getUpcomingIntrusiveAlarms: GetUpcomingIntrusiveAlarmsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpcomingAlarmsUiState())
    val uiState = _uiState.asStateFlow()

    private data class Inputs(
        val settings: Settings,
        val alarmSettings: AlarmSettings,
        val calc: CalculationSettings,
        val favorites: List<FavoriteLocation>,
        val reminders: List<Reminder>,
    )

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.data,
                alarmSettingsRepository.data,
                calculationSettingsRepository.data,
                favoriteLocationsRepository.data,
                reminderRepository.data,
            ) { settings, alarmSettings, calc, favorites, reminders ->
                Inputs(settings, alarmSettings, calc, favorites, reminders)
            }.collectLatest { input ->
                val now = Clock.System.now().toEpochMilliseconds()
                val location = input.favorites.firstOrNull { it.id == input.calc.locationId }?.locationDetail
                // Prayer-time math + audio probing off the main thread; collectLatest cancels stale runs.
                val occurrences = withContext(Dispatchers.Default) {
                    getUpcomingIntrusiveAlarms(
                        nowMs = now,
                        settings = input.settings,
                        alarmSettings = input.alarmSettings,
                        calc = input.calc,
                        location = location,
                        reminders = input.reminders,
                    )
                }
                _uiState.update {
                    it.copy(
                        alarms = occurrences.map { occ -> occ.toUi() },
                        loading = false,
                        locale = input.settings.selectedLocale,
                        is24Hour = input.settings.is24HourFormat,
                        numberingSystem = input.settings.numberingSystem,
                        nowMs = now,
                    )
                }
            }
        }
    }

    fun onAction(action: UpcomingAlarmsUiAction) {
        when (action) {
            is UpcomingAlarmsUiAction.OnSkip -> onSkip(action.occurrence)
            is UpcomingAlarmsUiAction.OnReschedule -> onReschedule(action.occurrence)
        }
    }

    private fun onSkip(occurrence: SkippedAlarm) {
        viewModelScope.launch {
            // Dismiss any live pre-alarm heads-up for this occurrence BEFORE recording the skip (which
            // triggers the re-arm) — and only when it's the one armed now, so skipping a later row never
            // clears the genuine next firing's heads-up.
            dismissUpcomingNotificationIfArmed(occurrence)
            settingsRepository.update { it.copy(skippedOccurrences = it.skippedOccurrences.upsert(occurrence)) }
        }
    }

    private fun onReschedule(occurrence: SkippedAlarm) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(skippedOccurrences = it.skippedOccurrences.without(occurrence)) }
        }
    }

    private suspend fun dismissUpcomingNotificationIfArmed(occurrence: SkippedAlarm) {
        val armed = alarmRepository.getScheduled()
        when (occurrence) {
            is SkippedAlarm.Adhan -> {
                val alarm = armed.firstOrNull { it.id == AdhanContract.ADHAN_ALARM_ID } ?: return
                val prayer = alarm.extras[PlaybackService.EXTRA_PRAYER]?.let(::prayerOrNull)
                if (prayer == occurrence.prayer && alarm.dateMatches(occurrence)) {
                    notificationRepository.cancelNotification(AdhanContract.PRE_ADHAN_NOTIFICATION_ID)
                }
            }

            is SkippedAlarm.Reminder -> {
                val alarm = armed.firstOrNull { it.id == ReminderContract.alarmId(occurrence.reminderId) } ?: return
                if (alarm.dateMatches(occurrence)) {
                    notificationRepository.cancelNotification(ReminderContract.preNotificationId(occurrence.reminderId))
                }
            }
        }
    }

    private fun ScheduledAlarm.dateMatches(occurrence: SkippedAlarm): Boolean =
        Instant.fromEpochMilliseconds(triggerAtMillis).toLocalDate() == occurrence.date

    private fun prayerOrNull(name: String): Prayer? = runCatching { Prayer.valueOf(name) }.getOrNull()

    private fun UpcomingOccurrence.toUi(): UpcomingAlarmUi =
        UpcomingAlarmUi(
            occurrence = occurrence,
            isAdhan = isAdhan,
            prayer = prayer,
            reminderLabel = reminder?.label,
            fireTimeMs = fireTimeMs,
            skipped = skipped,
            reminderDuration = reminder?.duration ?: 0,
            reminderDurationModifier = reminder?.durationModifier ?: 0,
        )
}
