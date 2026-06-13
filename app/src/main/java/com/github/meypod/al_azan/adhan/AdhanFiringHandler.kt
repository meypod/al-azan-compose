package com.github.meypod.al_azan.adhan

import android.app.NotificationManager
import android.content.Context
import android.widget.Toast
import androidx.core.content.getSystemService
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.adhan.AdhanFiringHandler.Companion.DEV_TEST_PRAYER
import com.github.meypod.al_azan.alarm.DndSilenceController
import com.github.meypod.al_azan.core.data.audio.AudioDurationProbe
import com.github.meypod.al_azan.core.data.audio.SoftSoundPlayer
import com.github.meypod.al_azan.core.data.audio.toAudioUri
import com.github.meypod.al_azan.core.data.locale.LocalizedResources
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.toAdhanKey
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.alarm.upsert
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationCategory
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationButton
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.domain.model.settings.isResolvable
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.domain.usecase.GetNextShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.ShariaTimeDetails
import com.github.meypod.al_azan.core.domain.util.formatTime
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
import kotlin.time.Instant

/**
 * Handles fired adhan alarms: marks them delivered, plays the adhan (foreground service) or posts a
 * notify-only notification, posts the pre-adhan reminder, handles cancel/snooze/dismiss, and always
 * reschedules the next adhan via [AdhanScheduler].
 */
@Singleton
class AdhanFiringHandler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val getNextShariaTimesUseCase: GetNextShariaTimesUseCase,
    private val notificationRepository: NotificationRepository,
    private val alarmRepository: AlarmRepository,
    private val adhanScheduler: AdhanScheduler,
    private val playbackLauncher: PlaybackLauncher,
    private val audioDurationProbe: AudioDurationProbe,
    private val softSoundPlayer: SoftSoundPlayer,
    private val dndSilenceController: DndSilenceController,
    private val localizedResources: LocalizedResources,
) {
    private companion object {
        /** Fixed prayer used by dev test helpers so they don't depend on real schedule settings. */
        val DEV_TEST_PRAYER = Prayer.Fajr
    }

    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun onAdhanFired(
        prayer: Prayer,
        playSound: Boolean,
        timestamp: Long,
    ) {
        // Mark delivered so the reschedule below targets the *next* prayer, not this one again.
        settingsRepository.markDelivered(AdhanContract.ADHAN_NOTIFICATION_ID, timestamp)
        // The adhan has arrived: dismiss its "upcoming" pre-notification so it doesn't linger.
        notificationRepository.cancelNotification(AdhanContract.PRE_ADHAN_NOTIFICATION_ID)
        val settings = settingsRepository.data.first()
        val alarmSettings = alarmSettingsRepository.data.first()

        // Honor an active "Dismiss & silent" window: never sound, but post a silent missed notice so
        // the user still learns the prayer passed, then just reschedule.
        val silencedUntil = settings.silencedUntilMillis ?: 0L
        if (Clock.System.now().toEpochMilliseconds() < silencedUntil) {
            postMissedNotification(prayer, timestamp, settings)
            adhanScheduler.schedule()
            return
        }

        val body = buildBody(timestamp, settings, alarmSettings)
        val entry = if (playSound) resolveSound(settings, prayer) else null
        val soundUri = entry?.toAudioUri(context)
        val vibration = alarmSettings.getVibrationSettings(prayer) ?: alarmSettings.vibrationMode
        // Continuous vibration must loop until dismissed, so it's intrusive regardless of the sound.
        val intrusive = vibration == VibrationMode.Continuous ||
            (soundUri != null && audioDurationProbe.isIntrusive(entry))
        if (soundUri != null && intrusive) {
            playbackLauncher.launch(
                PlaybackRequest.from(
                    settings = settings,
                    alarmSettings = alarmSettings,
                    title = localizedResources.current.getString(prayer.stringRes),
                    body = body,
                    timeLabel = settings.formatTime(timestamp),
                    soundUri = soundUri,
                    channelId = adhanChannel(settings),
                    loop = entry.loop,
                    vibration = vibration,
                    prayerName = prayer.name,
                ),
            )
        } else {
            // Soft (short, non-looping) muezzin or notify-only: plain notification, the sound played
            // once via a lightweight player when there is one (no foreground service / stop UI), and a
            // single vibration if requested (continuous would have been routed to the service above).
            postNotifyOnlyNotification(prayer, body, settings)
            if (soundUri != null) {
                if (vibration != VibrationMode.Off) VibrationController.vibrate(context, VibrationMode.Once)
                softSoundPlayer.play(soundUri)
            }
        }
        adhanScheduler.schedule()
    }

    private suspend fun postMissedNotification(
        prayer: Prayer,
        timestamp: Long,
        settings: Settings,
    ) {
        notificationRepository.notify(
            missedNotificationConfig(
                id = "missed_adhan_${prayer.name}",
                title = TextResource.StringResId(prayer.stringRes),
                body = TextResource.StringResIdWithArgs(
                    R.string.missed_during_silence_body,
                    settings.formatTime(timestamp),
                ),
            ),
        )
    }

    private suspend fun buildBody(
        timestamp: Long,
        settings: Settings,
        alarmSettings: AlarmSettings,
    ): String {
        var body = settings.formatTime(timestamp)
        if (alarmSettings.showNextPrayerTime) {
            nextAfter(timestamp, settings, alarmSettings)?.let { next ->
                body += " - ${localizedResources.current.getString(R.string.next_prayer_label)}: " +
                    "${localizedResources.current.getString(
                        next.prayer.stringRes,
                    )}, ${settings.formatTime(next.prayerTime.toEpochMilliseconds())}"
            }
        }
        return body
    }

    private suspend fun nextAfter(
        timestamp: Long,
        settings: Settings,
        alarmSettings: AlarmSettings,
    ): ShariaTimeDetails? =
        runCatching {
            val calc = calculationSettingsRepository.data.first()
            val params = calc.parameters ?: return null
            val location = favoriteLocationsRepository.data.first()
                .firstOrNull { it.id == calc.locationId }?.locationDetail ?: return null
            getNextShariaTimesUseCase(
                instant = Instant.fromEpochMilliseconds(timestamp + 1_000),
                calculationParameters = params,
                calculationAdjustments = calc.calculationAdjustments,
                arabicCalendar = settings.selectedArabicCalendar,
                locationDetail = location,
                alarmSettings = alarmSettings,
            )
        }.getOrNull()

    /**
     * "Dismiss & silent": stop the adhan, cancel its scheduled alarms, and silence the phone for
     * [minutes] via [DndSilenceController], then reschedule the next adhan (it stays suppressed while
     * the silence window is open).
     */
    suspend fun onDismissAndSilent(minutes: Int) {
        PlaybackService.stop(context)
        notificationRepository.cancelNotification(AdhanContract.ADHAN_NOTIFICATION_ID)
        dndSilenceController.silence(minutes)
        alarmRepository.cancel(AdhanContract.ADHAN_ALARM_ID)
        alarmRepository.cancel(AdhanContract.PRE_ADHAN_ALARM_ID)
        adhanScheduler.schedule()
    }

    fun dismissAndSilentFromUi(minutes: Int) {
        uiScope.launch { onDismissAndSilent(minutes) }
    }

    /** The "Dismiss & silent" window ended: release the DND silence and reschedule adhan. */
    suspend fun onUnsilence() {
        dndSilenceController.unsilence()
        adhanScheduler.schedule()
    }

    suspend fun onPreAdhanFired(
        prayer: Prayer,
        timestamp: Long,
    ) {
        val settings = settingsRepository.data.first()
        val prayerName = localizedResources.current.getString(prayer.stringRes)
        notificationRepository.notify(
            NotificationConfig(
                id = AdhanContract.PRE_ADHAN_NOTIFICATION_ID,
                title = TextResource.StringResId(R.string.upcoming_alarm_title),
                body = TextResource.Literal("$prayerName, ${settings.formatTime(timestamp)}"),
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.PRE_ADHAN_CHANNEL_ID,
                    category = AndroidNotificationCategory.CATEGORY_ALARM,
                    autoCancel = true,
                    actions = listOf(
                        NotificationButton(
                            title = TextResource.StringResId(R.string.cancel_alarm),
                            pressAction = NotificationPressAction.Broadcast(
                                action = AdhanContract.ACTION_CANCEL_ADHAN,
                                requestCode = AdhanContract.ACTION_CANCEL_ADHAN.hashCode(),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }

    /**
     * Cancel the upcoming adhan: skip this occurrence and reschedule the one after. Identical to the
     * Upcoming-alarms screen's Skip — it records a [SkippedAlarm] so the skipped row appears there with
     * an undo (Reschedule) action, instead of silently dropping the firing.
     */
    suspend fun onCancelAdhan() {
        val scheduled = alarmRepository.getScheduled()
            .firstOrNull { it.id == AdhanContract.ADHAN_ALARM_ID }
        val prayer = scheduled?.extras?.get(PlaybackService.EXTRA_PRAYER)
            ?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
        if (scheduled != null) {
            val entry = SkippedAlarm.Adhan(
                alarmId = AdhanContract.ADHAN_ALARM_ID,
                fireTimeMs = scheduled.triggerAtMillis,
                prayer = prayer,
            )
            settingsRepository.update { it.copy(skippedAlarms = it.skippedAlarms.upsert(entry)) }
        }
        notificationRepository.cancelNotification(AdhanContract.ADHAN_NOTIFICATION_ID)
        notificationRepository.cancelNotification(AdhanContract.PRE_ADHAN_NOTIFICATION_ID)
        PlaybackService.stop(context)
        adhanScheduler.schedule()
        val message = if (prayer != null) {
            localizedResources.current.getString(
                R.string.adhan_cancelled_named_toast,
                localizedResources.current.getString(prayer.stringRes),
            )
        } else {
            localizedResources.current.getString(R.string.adhan_cancelled_toast)
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * "Remind me later": stop the current adhan and schedule a silent follow-up notification in
     * [minutes], reminding the user the prayer passed. Does NOT replay the adhan.
     */
    suspend fun onRemindLater(
        prayer: Prayer,
        minutes: Int,
    ) {
        PlaybackService.stop(context)
        notificationRepository.cancelNotification(AdhanContract.ADHAN_NOTIFICATION_ID)
        val fireAt = Clock.System.now().toEpochMilliseconds() + minutes * 60_000L
        alarmRepository.schedule(
            ScheduledAlarm(
                id = AdhanContract.REMIND_ALARM_ID,
                triggerAtMillis = fireAt,
                action = AdhanContract.ACTION_ADHAN_REMIND,
                type = AlarmType.AlarmClock,
                extras = mapOf(
                    PlaybackService.EXTRA_PRAYER to prayer.name,
                    AdhanContract.EXTRA_REMIND_MINUTES to minutes.toString(),
                ),
            ),
        )
    }

    /** The "remind me later" timer elapsed: post a notification that the prayer was [minutes] ago. */
    suspend fun onAdhanRemindFired(
        prayer: Prayer,
        minutes: Int,
    ) {
        notificationRepository.notify(
            NotificationConfig(
                id = AdhanContract.REMIND_NOTIFICATION_ID,
                title = TextResource.StringResId(R.string.reminder),
                body = TextResource.StringResIdWithArgs(
                    R.string.adhan_remind_body,
                    TextResource.StringResId(prayer.stringRes),
                    minutes,
                ),
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.ADHAN_REMIND_CHANNEL_ID,
                    category = AndroidNotificationCategory.CATEGORY_REMINDER,
                    autoCancel = true,
                ),
            ),
        )
    }

    /** Stop the adhan (full-screen "Dismiss"). Synchronous. */
    fun onDismiss() {
        PlaybackService.stop(context)
        uiScope.launch { notificationRepository.cancelNotification(AdhanContract.ADHAN_NOTIFICATION_ID) }
    }

    /**
     * Dismiss from the full-screen alarm. When [autoSilent] is on it silences the phone for [minutes] —
     * but only if DND access is still granted. If it was revoked since the setting was enabled, it falls
     * back to a plain dismiss and posts a high-importance notice so the user isn't silently surprised.
     */
    fun dismissFromUi(
        autoSilent: Boolean,
        minutes: Int,
    ) {
        uiScope.launch {
            if (!autoSilent) {
                onDismiss()
                return@launch
            }
            val nm = context.getSystemService<NotificationManager>()
            if (nm != null && nm.isNotificationPolicyAccessGranted) {
                onDismissAndSilent(minutes)
            } else {
                onDismiss()
                notifyDndRevoked()
            }
        }
    }

    private suspend fun notifyDndRevoked() {
        notificationRepository.notify(
            NotificationConfig(
                id = AdhanContract.DND_REVOKED_NOTIFICATION_ID,
                title = TextResource.StringResId(R.string.adhan_dnd_revoked_title),
                body = TextResource.StringResId(R.string.adhan_dnd_revoked_body),
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.PERMISSION_REVOKED_CHANNEL_ID,
                    autoCancel = true,
                ),
            ),
        )
    }

    /** Fire-and-forget "remind me later" from the full-screen alarm; runs off the caller's lifecycle. */
    fun remindLaterFromUi(
        prayer: Prayer,
        minutes: Int,
    ) {
        uiScope.launch { onRemindLater(prayer, minutes) }
    }

    // --- Developer test helpers (only reached from the hidden developer screen) ---

    /**
     * Schedule a real adhan alarm [delaySeconds] from now, for testing. Uses [DEV_TEST_PRAYER] so dev
     * testing never depends on real calculation/location settings being configured.
     */
    suspend fun devScheduleAdhan(
        playSound: Boolean,
        delaySeconds: Int,
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        val fireAt = now + delaySeconds * 1000L
        val prayer = DEV_TEST_PRAYER
        alarmRepository.schedule(
            ScheduledAlarm(
                id = AdhanContract.DEV_TEST_ALARM_ID,
                triggerAtMillis = fireAt,
                action = AdhanContract.ACTION_ADHAN,
                type = AlarmType.AlarmClock,
                extras = mapOf(
                    PlaybackService.EXTRA_PRAYER to prayer.name,
                    AdhanContract.EXTRA_PLAY_SOUND to playSound.toString(),
                    AdhanContract.EXTRA_TIMESTAMP to fireAt.toString(),
                ),
            ),
        )
    }

    /** Fire the adhan right now (sound + full-screen) as if the alarm went off, for testing. */
    suspend fun devFireNow() {
        onAdhanFired(DEV_TEST_PRAYER, playSound = true, timestamp = Clock.System.now().toEpochMilliseconds())
    }

    /** Post the "upcoming adhan" notification for [DEV_TEST_PRAYER], for testing. */
    suspend fun devPostUpcoming() {
        onPreAdhanFired(DEV_TEST_PRAYER, Clock.System.now().toEpochMilliseconds())
    }

    private suspend fun postNotifyOnlyNotification(
        prayer: Prayer,
        body: String,
        settings: Settings,
    ) {
        notificationRepository.notify(
            NotificationConfig(
                id = AdhanContract.ADHAN_NOTIFICATION_ID,
                title = TextResource.StringResId(prayer.stringRes),
                body = TextResource.Literal(body),
                android = AndroidNotificationConfig(
                    channelId = adhanChannel(settings),
                    category = AndroidNotificationCategory.CATEGORY_ALARM,
                    autoCancel = true,
                ),
            ),
        )
    }

    private fun adhanChannel(settings: Settings): String =
        if (settings.bypassDnd) {
            EnsureNotificationChannelsUseCase.ADHAN_DND_CHANNEL_ID
        } else {
            EnsureNotificationChannelsUseCase.ADHAN_CHANNEL_ID
        }

    // A per-prayer override only wins when it's actually playable; an unresolvable one (orphaned/
    // corrupted) falls through to the global default, then to the first bundled sound — never "nothing".
    private fun resolveSound(
        settings: Settings,
        prayer: Prayer,
    ): AudioEntry? =
        settings.selectedAdhanEntries[prayer.toAdhanKey()]?.takeIf { it.isResolvable() }
            ?: settings.selectedAdhanEntries[AdhanKey.Default]?.takeIf { it.isResolvable() }
            ?: settings.savedAdhanAudioEntries.firstOrNull()
}
