package com.github.meypod.al_azan.ramadan

import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationCategory
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationButton
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.domain.util.hijriYear
import com.github.meypod.al_azan.core.domain.util.isRamadanNoticeDue
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Posts the background Ramadan-accuracy notice and handles its action buttons. Shares the in-app
 * dialog's suppression flags ([com.github.meypod.al_azan.core.domain.model.settings.Settings.ramadanRemindedYear]
 * / `ramadanReminderDontShow`), so dismissing via either surface silences both.
 */
@Singleton
class RamadanNoticeHandler @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository,
    private val ramadanNoticeScheduler: RamadanNoticeScheduler,
) {
    /** Daily check fired: post the notice if due, then re-arm for the next day. */
    suspend fun onCheckFired() {
        if (shouldNotify()) notifyNotice()
        ramadanNoticeScheduler.schedule()
    }

    suspend fun onRemindNextYear() {
        settingsRepository.update { it.copy(ramadanRemindedYear = hijriYear(Clock.System.now(), it.selectedArabicCalendar)) }
        notificationRepository.cancelNotification(RamadanNoticeContract.NOTIFICATION_ID)
    }

    suspend fun onDontShowAgain() {
        settingsRepository.update { it.copy(ramadanReminderDontShow = true) }
        notificationRepository.cancelNotification(RamadanNoticeContract.NOTIFICATION_ID)
    }

    private suspend fun shouldNotify(): Boolean {
        val settings = settingsRepository.data.first()
        if (settings.ramadanReminderDontShow) return false
        val now = Clock.System.now()
        val calendar = settings.selectedArabicCalendar
        if (!isRamadanNoticeDue(now, calendar)) return false
        return settings.ramadanRemindedYear != hijriYear(now, calendar)
    }

    private suspend fun notifyNotice() {
        notificationRepository.notify(
            NotificationConfig(
                id = RamadanNoticeContract.NOTIFICATION_ID,
                title = TextResource.StringResId(R.string.ramadan_notice_title),
                body = TextResource.StringResId(R.string.lunar_calendar_warning),
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.RAMADAN_NOTICE_CHANNEL_ID,
                    category = AndroidNotificationCategory.CATEGORY_REMINDER,
                    autoCancel = true,
                    // Re-posted daily during the window; only alert once so it doesn't buzz each re-post.
                    onlyAlertOnce = true,
                    actions = listOf(
                        NotificationButton(
                            title = TextResource.StringResId(R.string.ramadan_remind_next_year),
                            pressAction = NotificationPressAction.Broadcast(
                                action = RamadanNoticeContract.ACTION_RAMADAN_REMIND_NEXT_YEAR,
                                requestCode = RamadanNoticeContract.ACTION_RAMADAN_REMIND_NEXT_YEAR.hashCode(),
                            ),
                        ),
                        NotificationButton(
                            title = TextResource.StringResId(R.string.ramadan_dont_show_again),
                            pressAction = NotificationPressAction.Broadcast(
                                action = RamadanNoticeContract.ACTION_RAMADAN_DONT_SHOW_AGAIN,
                                requestCode = RamadanNoticeContract.ACTION_RAMADAN_DONT_SHOW_AGAIN.hashCode(),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}
