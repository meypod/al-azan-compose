package com.github.meypod.al_azan.prayer_checkin

import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.data.locale.LocalizedResources
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.timesOrNull
import com.github.meypod.al_azan.core.domain.model.alarm.SkippedAlarm
import com.github.meypod.al_azan.core.domain.model.alarm.upsert
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationCategory
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationButton
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationPressAction
import com.github.meypod.al_azan.core.domain.model.prayer_log.LOGGABLE_DAILY_PRAYERS
import com.github.meypod.al_azan.core.domain.model.prayer_log.PrayerRecordStatus
import com.github.meypod.al_azan.core.domain.model.reminder.nextOccurrence
import com.github.meypod.al_azan.core.domain.model.reminder.reminderCalendars
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.PrayerLogRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.reminder.ReminderContract
import com.github.meypod.al_azan.reminder.ReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Instant

@Singleton
class PrayerCheckInHandler internal constructor(
    private val notificationRepository: NotificationRepository,
    private val prayerLogRepository: PrayerLogRepository,
    private val reminderRepository: ReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val alarmSettingsRepository: AlarmSettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val getShariaTimesUseCase: GetShariaTimesUseCase,
    private val reminderScheduler: ReminderScheduler,
    private val alarmRepository: AlarmRepository,
    private val localizedResources: LocalizedResources,
    private val prayerCheckInFinalizer: PrayerCheckInFinalizer,
    private val prayerCheckInScheduler: PrayerCheckInScheduler,
    private val nowProvider: () -> Instant,
) {
    @Inject
    constructor(
        notificationRepository: NotificationRepository,
        prayerLogRepository: PrayerLogRepository,
        reminderRepository: ReminderRepository,
        settingsRepository: SettingsRepository,
        alarmSettingsRepository: AlarmSettingsRepository,
        calculationSettingsRepository: CalculationSettingsRepository,
        favoriteLocationsRepository: FavoriteLocationsRepository,
        getShariaTimesUseCase: GetShariaTimesUseCase,
        reminderScheduler: ReminderScheduler,
        alarmRepository: AlarmRepository,
        localizedResources: LocalizedResources,
        prayerCheckInFinalizer: PrayerCheckInFinalizer,
        prayerCheckInScheduler: PrayerCheckInScheduler,
    ) : this(
        notificationRepository = notificationRepository,
        prayerLogRepository = prayerLogRepository,
        reminderRepository = reminderRepository,
        settingsRepository = settingsRepository,
        alarmSettingsRepository = alarmSettingsRepository,
        calculationSettingsRepository = calculationSettingsRepository,
        favoriteLocationsRepository = favoriteLocationsRepository,
        getShariaTimesUseCase = getShariaTimesUseCase,
        reminderScheduler = reminderScheduler,
        alarmRepository = alarmRepository,
        localizedResources = localizedResources,
        prayerCheckInFinalizer = prayerCheckInFinalizer,
        prayerCheckInScheduler = prayerCheckInScheduler,
        nowProvider = { Clock.System.now() },
    )
    suspend fun onPromptFired(
        prayer: Prayer,
        date: LocalDate,
        timestamp: Long,
    ) {
        val deliveredMs = settingsRepository.data.first()
            .deliveredAlarmTimestamps[PrayerCheckInContract.DELIVERED_KEY]
        if (deliveredMs == timestamp) return
        settingsRepository.markDelivered(PrayerCheckInContract.DELIVERED_KEY, timestamp)

        val alarmSettings = alarmSettingsRepository.data.first()
        if (!alarmSettings.prayerCheckInEnabled || prayer.isNonPrayer) {
            prayerCheckInScheduler.schedule()
            return
        }

        prayerCheckInFinalizer.finalizePassedPrayers()

        val existingStatus = prayerLogRepository.getRecordForDate(date)?.statusFor(prayer)
        if (existingStatus == null) {
            postCheckInNotification(prayer, date)
        }

        prayerCheckInScheduler.schedule()
    }

    suspend fun onPrayed(
        prayer: Prayer,
        date: LocalDate,
    ) {
        prayerLogRepository.setPrayerStatus(date, prayer, PrayerRecordStatus.Prayed)
        notificationRepository.cancelNotification(PrayerCheckInContract.notificationId(prayer))
        alarmRepository.cancel(PrayerCheckInContract.expiredAlarmId(prayer))
        skipRemindersInPrayerWindow(prayer, date)
        prayerCheckInScheduler.schedule()
    }

    suspend fun onNotYet(
        prayer: Prayer,
        @Suppress("UNUSED_PARAMETER") date: LocalDate,
    ) {
        // Dismiss the check-in question; pending reminders and the end-of-window alarm stay active.
        notificationRepository.cancelNotification(PrayerCheckInContract.notificationId(prayer))
    }

    suspend fun onExpired(
        prayer: Prayer,
        date: LocalDate,
    ) {
        notificationRepository.cancelNotification(PrayerCheckInContract.notificationId(prayer))
        prayerCheckInFinalizer.finalizePrayerIfUnrecorded(prayer, date)
        prayerCheckInScheduler.schedule()
    }

    /** Keeps check-in notifications, expiration alarms, and reminders in sync with manual log edits. */
    suspend fun onPrayerStatusEdited(
        prayer: Prayer,
        date: LocalDate,
        newStatus: PrayerRecordStatus?,
    ) {
        if (newStatus != null) {
            val scheduledExpired = alarmRepository.getScheduled()
                .firstOrNull { it.id == PrayerCheckInContract.expiredAlarmId(prayer) }
            if (scheduledExpired?.extras?.get(PrayerCheckInContract.EXTRA_DATE) == date.toString()) {
                notificationRepository.cancelNotification(PrayerCheckInContract.notificationId(prayer))
                alarmRepository.cancel(PrayerCheckInContract.expiredAlarmId(prayer))
            }
            if (newStatus == PrayerRecordStatus.Prayed) {
                val window = prayerCheckInFinalizer.getPrayerWindow(date, prayer)
                val now = nowProvider()
                if (window != null && now < window.end) {
                    skipRemindersInPrayerWindow(prayer, date)
                }
            }
        }
        prayerCheckInScheduler.schedule()
    }

    /** Developer helper: immediately posts a check-in notification for the current or Fajr prayer. */
    suspend fun devFireNow() {
        val now = nowProvider()
        val today = now.toLocalDate()
        val prayer = resolveCurrentPrayerForDev(now, today) ?: Prayer.Fajr
        postCheckInNotification(prayer, today)
    }

    private suspend fun resolveCurrentPrayerForDev(
        now: kotlin.time.Instant,
        today: LocalDate,
    ): Prayer? {
        val calc = calculationSettingsRepository.data.first()
        val params = calc.parameters ?: return null
        val location = favoriteLocationsRepository.data.first()
            .firstOrNull { it.id == calc.locationId }?.locationDetail ?: return null
        val settings = settingsRepository.data.first()
        val times = getShariaTimesUseCase(
            instant = localNoonInstant(today),
            calculationParameters = params,
            calculationAdjustments = calc.calculationAdjustments,
            arabicCalendar = settings.selectedArabicCalendar,
            locationDetail = location,
            midnightMethod = calc.midnightMethod,
        ).timesOrNull ?: return null
        return LOGGABLE_DAILY_PRAYERS.firstOrNull { prayer ->
            val window = prayerWindow(prayer, times) ?: return@firstOrNull false
            now >= window.start && now < window.end
        }
    }

    private suspend fun postCheckInNotification(
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

    private suspend fun skipRemindersInPrayerWindow(
        prayer: Prayer,
        date: LocalDate,
    ) {
        val enabledReminders = reminderRepository.data.first().filter { it.enabled }
        if (enabledReminders.isEmpty()) return

        val settings = settingsRepository.data.first()
        val calc = calculationSettingsRepository.data.first()
        val params = calc.parameters
        val location = favoriteLocationsRepository.data.first()
            .firstOrNull { it.id == calc.locationId }?.locationDetail
        val window = prayerCheckInFinalizer.getPrayerWindow(date, prayer)

        val toSkip = if (window != null && params != null && location != null) {
            val calendars = settings.reminderCalendars(calc.calculationAdjustments.hijriDate)
            enabledReminders.mapNotNull { reminder ->
                val occurrence = reminder.nextOccurrence(
                    from = window.start,
                    skipped = settings.skippedOccurrences,
                    calendars = calendars,
                ) { dayInstant ->
                    getShariaTimesUseCase(
                        instant = dayInstant,
                        calculationParameters = params,
                        calculationAdjustments = calc.calculationAdjustments,
                        arabicCalendar = settings.selectedArabicCalendar,
                        locationDetail = location,
                        midnightMethod = calc.midnightMethod,
                    ).timesOrNull
                }
                if (occurrence != null && occurrence.fireAt < window.end) {
                    SkippedAlarm.Reminder(reminder.id, occurrence.fireAt.toLocalDate())
                } else {
                    null
                }
            }
        } else {
            // Fallback when prayer times aren't configured (e.g. dev test fire).
            enabledReminders
                .filter { it.prayer == prayer && it.durationModifier >= 0 }
                .map { SkippedAlarm.Reminder(it.id, date) }
        }

        if (toSkip.isEmpty()) return

        for (skip in toSkip) {
            notificationRepository.cancelNotification(ReminderContract.preNotificationId(skip.reminderId))
            notificationRepository.cancelNotification(ReminderContract.notificationId(skip.reminderId))
            alarmRepository.cancel(ReminderContract.alarmId(skip.reminderId))
            alarmRepository.cancel(ReminderContract.preAlarmId(skip.reminderId))
        }
        settingsRepository.update { current ->
            var updated = current.skippedOccurrences
            for (skip in toSkip) {
                updated = updated.upsert(skip)
            }
            current.copy(skippedOccurrences = updated)
        }
        reminderScheduler.schedule()
    }
}
