package com.github.meypod.al_azan.prayer_checkin

import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.locale.LocalizedResources
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationCategory
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationButton
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.PrayerLogRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.presentation.navigation.Route
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts and cancels check-in notifications, guaranteeing at most one notification per
 * `(prayer, date)` occurrence even if both an exact alarm and a clock-change reconcile fire.
 */
@Singleton
class PrayerCheckInNotifier @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val prayerLogRepository: PrayerLogRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val localizedResources: LocalizedResources,
) {
    private val mutex = Mutex()

    suspend fun promptIfDue(
        prayer: Prayer,
        date: LocalDate,
    ) {
        mutex.withLock {
            if (prayer.isNonPrayer) return@withLock
            if (!alarmSettingsRepository.data.first().prayerCheckInEnabled) return@withLock
            if (prayerLogRepository.getRecordForDate(date)?.statusFor(prayer) != null) return@withLock
            if (settingsRepository.data.first().deliveredPrayerCheckIns[prayer] == date) return@withLock
            if (!notificationRepository.isNotificationsAllowed()) return@withLock

            settingsRepository.update {
                it.copy(deliveredPrayerCheckIns = it.deliveredPrayerCheckIns + (prayer to date))
            }
            postNotification(prayer, date)
        }
    }

    suspend fun forcePromptForDev(
        prayer: Prayer,
        date: LocalDate,
    ) {
        mutex.withLock {
            postNotification(prayer, date)
        }
    }

    suspend fun cancelPrompt(prayer: Prayer) {
        notificationRepository.cancelNotification(PrayerCheckInContract.notificationId(prayer))
    }

    private suspend fun postNotification(
        prayer: Prayer,
        date: LocalDate,
    ) {
        val prayerName = localizedResources.current.getString(prayer.stringRes)
        val question = localizedResources.current.getString(R.string.have_you_prayed_format, prayerName)

        notificationRepository.notify(
            NotificationConfig(
                id = PrayerCheckInContract.notificationId(prayer),
                title = TextResource.StringResId(prayer.stringRes),
                body = TextResource.Literal(question),
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.PRAYER_CHECKIN_CHANNEL_ID,
                    category = AndroidNotificationCategory.CATEGORY_REMINDER,
                    ongoing = true,
                    onlyAlertOnce = true,
                    autoCancel = false,
                    pressAction = NotificationPressAction.Route(Route.Main.PrayerLog),
                    actions = listOf(
                        NotificationButton(
                            title = TextResource.StringResId(R.string.prayer_action_prayed),
                            pressAction = NotificationPressAction.Broadcast(
                                action = PrayerCheckInContract.ACTION_CHECKIN_PRAYED,
                                requestCode = (PrayerCheckInContract.ACTION_CHECKIN_PRAYED + prayer.name).hashCode(),
                                extras = mapOf(
                                    PrayerCheckInContract.EXTRA_PRAYER to prayer.name,
                                    PrayerCheckInContract.EXTRA_DATE to date.toString(),
                                ),
                            ),
                        ),
                        NotificationButton(
                            title = TextResource.StringResId(R.string.prayer_action_not_yet),
                            pressAction = NotificationPressAction.Broadcast(
                                action = PrayerCheckInContract.ACTION_CHECKIN_NOT_YET,
                                requestCode = (PrayerCheckInContract.ACTION_CHECKIN_NOT_YET + prayer.name).hashCode(),
                                extras = mapOf(
                                    PrayerCheckInContract.EXTRA_PRAYER to prayer.name,
                                    PrayerCheckInContract.EXTRA_DATE to date.toString(),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}
