package com.github.meypod.al_azan.reminder

import android.content.Context
import android.widget.Toast
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.alarm.DndSilenceController
import com.github.meypod.al_azan.core.data.audio.AudioDurationProbe
import com.github.meypod.al_azan.core.data.audio.SoftSoundPlayer
import com.github.meypod.al_azan.core.data.audio.toAudioUri
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.alarm.upsert
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationCategory
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationButton
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.domain.model.reminder.ReminderAudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.isResolvable
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.domain.util.formatTime
import com.github.meypod.al_azan.core.presentation.mapper.displayName
import com.github.meypod.al_azan.core.util.device.VibrationController
import com.github.meypod.al_azan.playback.PlaybackLauncher
import com.github.meypod.al_azan.playback.PlaybackRequest
import com.github.meypod.al_azan.playback.PlaybackService
import com.github.meypod.al_azan.playback.missedNotificationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val audioDurationProbe: AudioDurationProbe,
    private val softSoundPlayer: SoftSoundPlayer,
    private val dndSilenceController: DndSilenceController,
) {
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Stop the reminder sound (full-screen "Dismiss"). Fire-and-forget; outlives the alarm activity. */
    fun dismissFromUi() {
        uiScope.launch { PlaybackService.stop(context) }
    }

    /**
     * "Dismiss & silent" for a reminder: stop the sound and silence the phone for [minutes]. Unlike
     * adhans, reminders never silence *automatically* — this is only the explicit menu action, and only
     * offered when the app holds DND policy access (gated in AlarmFullscreenViewModel).
     */
    fun dismissAndSilentFromUi(minutes: Int) {
        uiScope.launch {
            PlaybackService.stop(context)
            dndSilenceController.silence(minutes)
        }
    }

    /**
     * Play a reminder right now (sound + full-screen) as if one fired, for testing. Uses the default
     * adhan sound (long audio) so the full-screen alarm exercises the intrusive playback path, and the
     * global vibration mode, so it never depends on a stored reminder existing.
     */
    suspend fun devFireNow() {
        val settings = settingsRepository.data.first()
        val alarmSettings = alarmSettingsRepository.data.first()
        val title = context.getString(R.string.reminder)
        val timeLabel = settings.formatTime(Clock.System.now().toEpochMilliseconds())
        val soundEntry = settings.selectedAdhanEntries[AdhanKey.Default]?.takeIf { it.isResolvable() }
            ?: settings.savedAdhanAudioEntries.firstOrNull()
        val soundUri = soundEntry?.toAudioUri(context) ?: return
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
                vibration = alarmSettings.vibrationMode,
                header = context.getString(R.string.reminder),
                isReminder = true,
            ),
        )
    }

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

        val title = reminder.displayName(context.resources)
        val timeLabel = settings.formatTime(timestamp)
        // The adhan "Dismiss & silent" window suppresses reminders too: post a silent missed notice
        // instead of sounding, so the user still sees it passed.
        val silencedUntil = settings.silencedUntilMillis ?: 0L
        if (Clock.System.now().toEpochMilliseconds() < silencedUntil) {
            postMissedNotification(reminderId, title, timeLabel)
        } else {
            val soundEntry = reminder.sound ?: ReminderAudioEntry.DefaultReminderAudioEntry
            val soundUri = soundEntry.toAudioUri(context)
            val vibration = reminder.vibration ?: alarmSettings.vibrationMode
            // Continuous vibration must loop until dismissed, so it's intrusive regardless of the sound.
            val intrusive = vibration == VibrationMode.Continuous ||
                (soundUri != null && audioDurationProbe.isIntrusive(soundEntry))
            if (soundUri != null && intrusive) {
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
                        vibration = vibration,
                        header = context.getString(R.string.reminder),
                        isReminder = true,
                    ),
                )
            } else {
                // Soft (short, non-looping) sound or no sound: a plain auto-cancel notification, the
                // sound played once via a lightweight player (no foreground service / stop UI), and a
                // single vibration if requested (continuous would have been routed to the service above).
                postNotifyOnlyNotification(reminderId, title, timeLabel, settings)
                if (soundUri != null) {
                    if (vibration != VibrationMode.Off) VibrationController.vibrate(context, VibrationMode.Once)
                    softSoundPlayer.play(soundUri)
                }
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
        val title = reminder.displayName(context.resources)
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

    /**
     * Cancel an upcoming reminder: skip this occurrence and reschedule the next. Identical to the
     * Upcoming-alarms screen's Skip — it records a [SkippedAlarm] so the skipped row appears there with
     * an undo (Reschedule) action, instead of silently dropping the firing.
     */
    suspend fun onCancelReminder(reminderId: String) {
        val scheduledTs = alarmRepository.getScheduled()
            .firstOrNull { it.id == ReminderContract.alarmId(reminderId) }?.triggerAtMillis
        val reminder = reminderRepository.data.first().firstOrNull { it.id == reminderId }
        if (scheduledTs != null) {
            val entry = SkippedAlarm.Reminder(
                alarmId = ReminderContract.alarmId(reminderId),
                fireTimeMs = scheduledTs,
                prayer = reminder?.prayer,
                label = reminder?.label,
                duration = reminder?.duration ?: 0,
                durationModifier = reminder?.durationModifier ?: 0,
            )
            settingsRepository.update { it.copy(skippedAlarms = it.skippedAlarms.upsert(entry)) }
        }
        notificationRepository.cancelNotification(ReminderContract.notificationId(reminderId))
        notificationRepository.cancelNotification(ReminderContract.preNotificationId(reminderId))
        PlaybackService.stop(context)
        reminderScheduler.schedule()

        val name = reminder?.displayName(context.resources) ?: context.getString(R.string.reminder)
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
