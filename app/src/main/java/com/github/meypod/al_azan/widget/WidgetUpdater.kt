package com.github.meypod.al_azan.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmType
import com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.model.widget.WidgetData
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.BuildWidgetDataUseCase
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Recomputes the prayer-times widgets and applies them: redraws the home-screen app widget, posts or
 * cancels the persistent notification widget per the user's toggle, and schedules the next redraw.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val buildWidgetDataUseCase: BuildWidgetDataUseCase,
    private val alarmRepository: AlarmRepository,
    private val notificationRepository: NotificationRepository,
) {
    private val appWidgetManager by lazy { AppWidgetManager.getInstance(context) }

    private companion object {
        const val TAG = "WidgetUpdater"
    }

    suspend fun update() {
        val settings = settingsRepository.data.first()
        val calcSettings = calculationSettingsRepository.data.first()
        val locations = favoriteLocationsRepository.data.first()
        val location = locations.firstOrNull { it.id == calcSettings.locationId }?.locationDetail

        val data = buildWidgetDataUseCase(Clock.System.now(), settings, calcSettings, location)

        if (data == null) {
            // Not configured yet: nothing meaningful to show.
            WidgetRenderCache.lastData = null
            notificationRepository.cancelNotification(WidgetContract.NOTIFICATION_ID)
            alarmRepository.cancel(WidgetContract.REDRAW_ALARM_ID)
            return
        }

        // Cache so PrayerTimesWidget.onUpdate can cheaply re-push if the launcher resets the widget.
        WidgetRenderCache.lastData = data

        val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, PrayerTimesWidget::class.java))
        if (widgetIds.isNotEmpty()) {
            appWidgetManager.updateAppWidget(widgetIds, WidgetRenderer.buildScreenWidget(context, data))
        }
        updateNotification(data)

        // Only keep recomputing on a schedule while something is actually on screen.
        if (widgetIds.isNotEmpty() || data.showNotification) {
            scheduleNextRedraw(data)
        } else {
            alarmRepository.cancel(WidgetContract.REDRAW_ALARM_ID)
        }
    }

    private suspend fun updateNotification(data: WidgetData) {
        if (!data.showNotification) {
            notificationRepository.cancelNotification(WidgetContract.NOTIFICATION_ID)
            return
        }
        val small = WidgetRenderer.build(context, notifSmallLayout(data.adaptiveTheme), data)
        val big = WidgetRenderer.build(context, notifBigLayout(data.adaptiveTheme, data.showCountdown), data)
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

    private suspend fun scheduleNextRedraw(data: WidgetData) {
        val nextMillis = data.nextUpdateAtMillis
        if (nextMillis == null) {
            alarmRepository.cancel(WidgetContract.REDRAW_ALARM_ID)
            return
        }
        // small buffer so the targeted prayer/day has actually elapsed when we recompute
        val triggerAt = nextMillis + 1_000
        val now = System.currentTimeMillis()
        if (triggerAt <= now) {
            // A non-future target would fire the redraw alarm immediately and loop forever. This only
            // happens in a degenerate state (all prayer times in the past for the day); skip and let
            // the next real trigger (day rollover / settings change) recompute.
            Log.w(TAG, "Skipping widget redraw: non-future target nextMillis=$nextMillis now=$now")
            alarmRepository.cancel(WidgetContract.REDRAW_ALARM_ID)
            return
        }
        Log.i(TAG, "Next widget redraw in ${(triggerAt - now) / 1000}s")
        alarmRepository.schedule(
            ScheduledAlarm(
                id = WidgetContract.REDRAW_ALARM_ID,
                triggerAtMillis = triggerAt,
                action = WidgetContract.ACTION_WIDGET_UPDATE,
                type = AlarmType.ExactAllowWhileIdle,
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
