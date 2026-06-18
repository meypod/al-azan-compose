package com.github.meypod.al_azan.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.settings.NotificationWidgetLayout
import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerWidgetData
import com.github.meypod.al_azan.core.domain.model.widget.WidgetData
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.BuildNextPrayerWidgetDataUseCase
import com.github.meypod.al_azan.core.domain.usecase.BuildWidgetDataUseCase
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Recomputes the prayer-times widgets and applies them: redraws the home-screen app widget, posts or
 * cancels the persistent notification widget per the user's toggle, and schedules the next redraw.
 *
 * Runs directly from each trigger (alarm/settings/boot/time/locale/foreground/placement) — no
 * WorkManager — so the redraw happens in the same wake-up that fired it, with no deferral. A [Mutex]
 * serializes concurrent triggers.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val buildWidgetDataUseCase: BuildWidgetDataUseCase,
    private val buildNextPrayerWidgetDataUseCase: BuildNextPrayerWidgetDataUseCase,
    private val alarmRepository: AlarmRepository,
    private val notificationRepository: NotificationRepository,
) {
    private val appWidgetManager by lazy { AppWidgetManager.getInstance(context) }
    private val mutex = Mutex()

    private companion object {
        const val TAG = "WidgetUpdater"
    }

    suspend fun update() =
        mutex.withLock {
            val settings = settingsRepository.data.first()
            val calcSettings = calculationSettingsRepository.data.first()
            val locations = favoriteLocationsRepository.data.first()
            val location = locations.firstOrNull { it.id == calcSettings.locationId }?.locationDetail

            val now = Clock.System.now()
            // Adaptive (Material You) layouts only exist under -v31; on older devices their theme is
            // missing and the widget fails to render. Force the non-adaptive layout there regardless of
            // a stored "on" value (the setting is hidden pre-31, but old data may still carry it).
            val adaptiveSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            val data = buildWidgetDataUseCase(now, settings, calcSettings, location)
                ?.let { if (adaptiveSupported) it else it.copy(adaptiveTheme = false) }
            val nextData = buildNextPrayerWidgetDataUseCase(now, settings, calcSettings, location)
                ?.let { if (adaptiveSupported) it else it.copy(adaptiveTheme = false) }

            val screenIds = appWidgetManager.getAppWidgetIds(ComponentName(context, PrayerTimesWidget::class.java))
            val nextIds = appWidgetManager.getAppWidgetIds(ComponentName(context, NextPrayerWidget::class.java))

            // Prayer-times widget + notification.
            WidgetRenderCache.lastData = data
            if (data == null) {
                // Not configured yet: nothing meaningful to show.
                notificationRepository.cancelNotification(WidgetContract.NOTIFICATION_ID)
            } else {
                if (screenIds.isNotEmpty()) {
                    appWidgetManager.updateAppWidget(screenIds, WidgetRenderer.buildScreenWidget(context, data))
                }
                updateNotification(data, nextData)
            }

            // 1x1 next-prayer widget. When not computable (unconfigured / empty selection) leave it on its
            // placeholder rather than blanking it.
            NextPrayerRenderCache.lastData = nextData
            if (nextData != null && nextIds.isNotEmpty()) {
                appWidgetManager.updateAppWidget(nextIds, WidgetRenderer.buildNextPrayerWidget(context, nextData))
            }

            // Only keep recomputing on a schedule while something is actually on screen. Each surface
            // contributes its own next transition; redraw at the earliest of them.
            val compactNotificationShown = data != null && data.showNotification &&
                data.notificationLayout == NotificationWidgetLayout.Compact && nextData != null
            val nextUpdateCandidates = buildList {
                if (data != null && (screenIds.isNotEmpty() || data.showNotification)) {
                    data.nextUpdateAtMillis?.let { add(it) }
                }
                // The compact notification follows the next-prayer transition even when no 1x1 home
                // widget is placed, so include its redraw time whenever it (or the 1x1 widget) is on.
                if (nextData != null && (nextIds.isNotEmpty() || compactNotificationShown)) {
                    nextData.nextUpdateAtMillis?.let { add(it) }
                }
            }
            scheduleNextRedraw(nextUpdateCandidates.minOrNull())
        }

    private suspend fun updateNotification(
        data: WidgetData,
        nextData: NextPrayerWidgetData?,
    ) {
        if (!data.showNotification) {
            notificationRepository.cancelNotification(WidgetContract.NOTIFICATION_ID)
            return
        }
        // Compact layout reuses the 1x1 next-prayer rendering for both the collapsed and expanded
        // views. Fall back to the table when next-prayer data is unavailable (e.g. no countdown prayer
        // selected) so the notification never blanks.
        val small: RemoteViews
        val big: RemoteViews
        if (data.notificationLayout == NotificationWidgetLayout.Compact && nextData != null) {
            small = WidgetRenderer.buildNextPrayerNotification(context, nextData, expanded = false)
            big = WidgetRenderer.buildNextPrayerNotification(context, nextData, expanded = true)
        } else {
            small = WidgetRenderer.build(context, notifSmallLayout(data.adaptiveTheme), data)
            big = WidgetRenderer.build(context, notifBigLayout(data.adaptiveTheme, data.showCountdown), data)
        }
        notificationRepository.notify(
            NotificationConfig(
                id = WidgetContract.NOTIFICATION_ID,
                android = AndroidNotificationConfig(
                    channelId = EnsureNotificationChannelsUseCase.WIDGET_CHANNEL_ID,
                    ongoing = true,
                    onlyAlertOnce = true,
                    autoCancel = false,
                    showTimestamp = false,
                    sortKey = "-1",
                    customContentView = small,
                    customBigContentView = big,
                ),
            ),
        )
    }

    private suspend fun scheduleNextRedraw(nextMillis: Long?) {
        if (nextMillis == null) {
            alarmRepository.cancel(WidgetContract.REDRAW_ALARM_ID)
            return
        }
        // small buffer so the targeted prayer/day has actually elapsed when the alarm recomputes
        val triggerAt = nextMillis + 1_000
        val now = System.currentTimeMillis()
        if (triggerAt <= now) {
            // A non-future target would fire immediately and loop forever — only happens in a degenerate
            // state (all prayer times in the past for the day); skip and let the next real trigger recompute.
            Log.w(TAG, "Skipping widget redraw: non-future target nextMillis=$nextMillis now=$now")
            alarmRepository.cancel(WidgetContract.REDRAW_ALARM_ID)
            return
        }
        Log.i(TAG, "Next widget redraw in ${(triggerAt - now) / 1000}s")
        // Exact but non-wakeup: fires on time when the device is awake, and the OS coalesces it
        // efficiently while idle (repaints on the next wake) — a cosmetic redraw shouldn't wake the
        // device. Falls back to inexact (still non-wakeup) if exact-alarm permission is missing.
        alarmRepository.schedule(
            ScheduledAlarm(
                id = WidgetContract.REDRAW_ALARM_ID,
                triggerAtMillis = triggerAt,
                action = WidgetContract.ACTION_WIDGET_UPDATE,
                type = AlarmType.Exact,
                wakeup = false,
            ),
        )
    }

    private fun notifSmallLayout(adaptive: Boolean): Int =
        if (adaptive) R.layout.notif_widget_small_adaptive else R.layout.notif_widget_small

    private fun notifBigLayout(
        adaptive: Boolean,
        countdown: Boolean,
    ): Int =
        when {
            countdown && adaptive -> R.layout.notif_widget_big_countdown_adaptive
            countdown -> R.layout.notif_widget_big_countdown
            adaptive -> R.layout.notif_widget_big_adaptive
            else -> R.layout.notif_widget_big
        }
}
