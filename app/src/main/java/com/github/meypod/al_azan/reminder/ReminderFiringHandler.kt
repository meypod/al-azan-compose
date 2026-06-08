package com.github.meypod.al_azan.reminder

import android.content.Context
import android.widget.Toast
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.audio.toAudioUri
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationCategory
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationButton
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.domain.util.formatTime
import com.github.meypod.al_azan.playback.PlaybackLauncher
import com.github.meypod.al_azan.playback.PlaybackRequest
import com.github.meypod.al_azan.playback.missedNotificationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/** Handles fired reminder alarms: plays the sound (full-screen) or notifies, then reschedules. */
@Singleton
class ReminderFiringHandler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val reminderRepository: ReminderRepository,
    private val notificationRepository: NotificationRepository,
    private val alarmRepository: AlarmRepository,
    private val reminderScheduler: ReminderScheduler,
    private val playbackLauncher: PlaybackLauncher,
) {
    suspend fun onReminderFired(
        reminderId: String,
        timestamp: Long,
    ) {
        // Mark delivered so the reschedule targets the *next* occurrence, not this one again.
        settingsRepository.markDelivered(ReminderContract.notificationId(reminderId), timestamp)
        notificationRepository.cancelNotification(ReminderContract.preNotificationId(reminderId))
        val reminder = reminderRepository.data.first().firstOrNull { it.id == reminderId }
        if (reminder == null || !reminder.enabled) {
            reminderScheduler.schedule()
            return
        }
        val settings = settingsRepository.data.first()
        val alarmSettings = alarmSettingsRepository.data.first()

        val title = reminder.label.ifBlank { context.getString(reminder.prayer.stringRes) }
        val timeLabel = settings.formatTime(timestamp)
        // The adhan "Dismiss & silent" window suppresses reminders too: post a silent missed notice
        // instead of sounding, so the user still sees it passed.
        val silencedUntil = settings.adhanSilencedUntilMillis ?: 0L
        if (Clock.System.now().toEpochMilliseconds() < silencedUntil) {
            postMissedNotification(reminderId, title, timeLabel)
        } else {
            val soundEntry = reminder.sound ?: ReminderAudioEntry.DefaultReminderAudioEntry
            val soundUri = soundEntry.toAudioUri(context)
            if (soundUri != null) {
                playbackLauncher.launch(
                    PlaybackRequest.from(
                        settings = settings,
                        alarmSettings = alarmSettings,
                        title = title,
                        body = timeLabel,
                        timeLabel = timeLabel,
                        soundUri = soundUri,
                        channelId = reminderChannel(settings),
                        loop = soundEntry.loop,
                        vibration = reminder.vibration ?: alarmSettings.vibrationMode,
                        header = context.getString(R.string.reminder),
                        isReminder = true,
                    ),
                )
            } else {
                postNotifyOnlyNotification(reminderId, title, timeLabel, settings)
            }
        }

        // A one-off reminder disables itself after firing; recurring ones reschedule the next day.
        if (reminder.once == true) {
            reminderRepository.update { list ->
                list.map { if (it.id == reminderId) it.copy(enabled = false) else it }
            }
        }
        reminderScheduler.schedule()
    }

    /** Post the "upcoming reminder" notification ahead of the reminder time. */
    suspend fun onPreReminderFired(
        reminderId: String,
        timestamp: Long,
    ) {
        val reminder = reminderRepository.data.first().firstOrNull { it.id == reminderId } ?: return
        if (!reminder.enabled) return
        val settings = settingsRepository.data.first()
        val title = reminder.label.ifBlank { context.getString(reminder.prayer.stringRes) }
        val timeLabel = settings.formatTime(timestamp)
        notificationRepository.notify(
            NotificationConfig(
                id = ReminderContract.preNotificationId(reminderId),
                title = TextResource.StringResId(R.string.upcoming_reminder_title),
                body = TextResource.Literal("$title, $timeLabel"),
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.PRE_REMINDER_CHANNEL_ID,
                    category = AndroidNotificationCategory.CATEGORY_REMINDER,
                    autoCancel = true,
                    actions = listOf(
                        NotificationButton(
                            title = TextResource.StringResId(R.string.cancel_alarm),
                            pressAction = NotificationPressAction.Broadcast(
                                action = ReminderContract.ACTION_CANCEL_REMINDER,
                                requestCode = ReminderContract.ACTION_CANCEL_REMINDER.hashCode() xor reminderId.hashCode(),
                                extras = mapOf(ReminderContract.EXTRA_REMINDER_ID to reminderId),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    /** Cancel an upcoming reminder: skip this occurrence and reschedule the next. */
    suspend fun onCancelReminder(reminderId: String) {
        val scheduledTs = alarmRepository.getScheduled()
            .firstOrNull { it.id == ReminderContract.alarmId(reminderId) }?.triggerAtMillis
        if (scheduledTs != null) {
            settingsRepository.markDelivered(ReminderContract.notificationId(reminderId), scheduledTs)
        }
        alarmRepository.cancel(ReminderContract.alarmId(reminderId))
        alarmRepository.cancel(ReminderContract.preAlarmId(reminderId))
        notificationRepository.cancelNotification(ReminderContract.notificationId(reminderId))
        notificationRepository.cancelNotification(ReminderContract.preNotificationId(reminderId))
        reminderScheduler.schedule()

        val reminder = reminderRepository.data.first().firstOrNull { it.id == reminderId }
        val name = reminder?.label?.ifBlank { context.getString(reminder.prayer.stringRes) }
            ?: context.getString(R.string.reminder)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.reminder_cancelled_toast, name), Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun postMissedNotification(
        reminderId: String,
        title: String,
        timeLabel: String,
    ) {
        notificationRepository.notify(
            missedNotificationConfig(
                id = "missed_${ReminderContract.notificationId(reminderId)}",
                title = TextResource.Literal(title),
                body = TextResource.StringResIdWithArgs(R.string.missed_during_silence_body, timeLabel),
            ),
        )
    }

    private suspend fun postNotifyOnlyNotification(
        reminderId: String,
        title: String,
        timeLabel: String,
        settings: Settings,
    ) {
        notificationRepository.notify(
            NotificationConfig(
                id = ReminderContract.notificationId(reminderId),
                title = TextResource.Literal(title),
                body = TextResource.Literal(timeLabel),
                android = AndroidNotificationConfig(
                    channelId = reminderChannel(settings),
                    category = AndroidNotificationCategory.CATEGORY_ALARM,
                    autoCancel = true,
                ),
            ),
        )
    }

    private fun reminderChannel(settings: Settings): String =
        if (settings.bypassDnd) {
            EnsureNotificationChannelsUseCase.REMINDER_DND_CHANNEL_ID
        } else {
            EnsureNotificationChannelsUseCase.REMINDER_CHANNEL_ID
        }
}
