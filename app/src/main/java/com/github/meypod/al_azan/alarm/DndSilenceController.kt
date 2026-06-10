package com.github.meypod.al_azan.alarm

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationCategory
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationButton
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.domain.util.formatTime
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Owns the "Dismiss & silent" total-silence Do Not Disturb window shared by adhans and reminders:
 * silences the phone (and suppresses both adhan and reminder playback via
 * [com.github.meypod.al_azan.core.domain.model.settings.Settings.silencedUntilMillis]) for a
 * window, then restores when it ends.
 *
 * DND is engaged with [NotificationManager.setInterruptionFilter] (total silence), capturing the
 * previous filter into [com.github.meypod.al_azan.core.domain.model.settings.Settings.dndRestoreFilter]
 * so [unsilence] can put it back. On API 35+ the platform reroutes this to an app-managed implicit zen
 * rule rather than the global filter, but the call still engages DND — so a single code path covers all
 * versions, with no explicit [android.app.AutomaticZenRule] for the user to find and manage.
 */
@Singleton
class DndSilenceController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val alarmRepository: AlarmRepository,
    private val notificationRepository: NotificationRepository,
) {
    /**
     * Suppress alarms for [minutes] and put the phone into total-silence DND for the same window,
     * arming an unsilence alarm to undo it. Returns whether DND was actually engaged (i.e. policy
     * access was granted); the suppression window is set regardless.
     */
    suspend fun silence(minutes: Int): Boolean = applySilence(Clock.System.now().toEpochMilliseconds() + minutes * 60_000L)

    /**
     * Re-establish an in-progress silence window that the OS may have torn down (reboot clears alarms +
     * notifications). If the saved window already elapsed, clean up instead. Idempotent — safe to call on
     * every boot/reconcile. Without this, a reboot mid-window would strip the control notice and the
     * unsilence alarm, trapping the user in silence.
     */
    suspend fun reconcile() {
        val until = settingsRepository.data.first().silencedUntilMillis ?: return
        if (Clock.System.now().toEpochMilliseconds() >= until) {
            unsilence()
        } else {
            applySilence(until)
        }
    }

    /**
     * (Re)apply total-silence DND for a window ending at [until]: engage the platform DND, persist the
     * window, arm the unsilence alarm, and post the control notice. Reused by both a fresh [silence] and
     * [reconcile], so each step keys off a stable id and is idempotent.
     */
    private suspend fun applySilence(until: Long): Boolean {
        val settings = settingsRepository.data.first()
        val nm = context.getSystemService<NotificationManager>()
            ?.takeIf { it.isNotificationPolicyAccessGranted }

        // Preserve any filter captured by an earlier engage so a reconcile doesn't overwrite it with the
        // already-silenced value.
        var restoreFilter: Int? = settings.dndRestoreFilter
        if (nm != null) {
            if (restoreFilter == null) restoreFilter = nm.currentInterruptionFilter
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }
        val dndEngaged = nm != null

        settingsRepository.update {
            it.copy(silencedUntilMillis = until, dndRestoreFilter = restoreFilter)
        }
        if (dndEngaged) {
            alarmRepository.schedule(
                ScheduledAlarm(
                    id = AdhanContract.UNSILENCE_ALARM_ID,
                    triggerAtMillis = until,
                    action = AdhanContract.ACTION_UNSILENCE,
                    type = AlarmType.ExactAllowWhileIdle,
                ),
            )
        }
        // The OS surfaces no "end early" affordance for our window, so post our own: tapping the action
        // or dismissing the notice both fire ACTION_UNSILENCE, ending the window.
        postSilenceNotice(settings.formatTime(until))
        return dndEngaged
    }

    private suspend fun postSilenceNotice(untilFormatted: String) {
        val end = NotificationPressAction.Broadcast(
            action = AdhanContract.ACTION_UNSILENCE,
            requestCode = AdhanContract.ACTION_UNSILENCE.hashCode(),
        )
        notificationRepository.notify(
            NotificationConfig(
                id = AdhanContract.DND_ACTIVE_NOTIFICATION_ID,
                title = TextResource.StringResId(R.string.dnd_active_title),
                body = TextResource.StringResIdWithArgs(R.string.dnd_active_body, untilFormatted),
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.DND_ACTIVE_CHANNEL_ID,
                    category = AndroidNotificationCategory.CATEGORY_STATUS,
                    onlyAlertOnce = true,
                    autoCancel = false,
                    // Tapping the notice opens the status screen (same place the system DND-rule entry lands).
                    pressAction = NotificationPressAction.Route(Route.Main.SilenceStatus),
                    actions = listOf(
                        NotificationButton(
                            title = TextResource.StringResId(R.string.dnd_active_end_action),
                            pressAction = end,
                        ),
                    ),
                    dismissAction = end,
                ),
            ),
        )
    }

    /** The silence window ended: release the DND, clear the window, and cancel the unsilence alarm. */
    suspend fun unsilence() {
        val settings = settingsRepository.data.first()
        val nm = context.getSystemService<NotificationManager>()
        if (nm != null && nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(settings.dndRestoreFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        settingsRepository.update {
            it.copy(silencedUntilMillis = null, dndRestoreFilter = null)
        }
        alarmRepository.cancel(AdhanContract.UNSILENCE_ALARM_ID)
        notificationRepository.cancelNotification(AdhanContract.DND_ACTIVE_NOTIFICATION_ID)
    }
}
