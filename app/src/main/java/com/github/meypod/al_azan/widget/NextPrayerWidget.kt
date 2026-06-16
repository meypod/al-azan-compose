package com.github.meypod.al_azan.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 1x1 next-prayer app widget: shows the upcoming prayer's name above a live countdown chronometer.
 *
 * Like [PrayerTimesWidget], [onUpdate] cheaply re-pushes the last rendered content from
 * [NextPrayerRenderCache] (repaints after a launcher reset without recomputing); only when nothing is
 * cached yet does it run a full [WidgetUpdater.update]. Ongoing content changes come from the redraw
 * alarm and the settings/boot/time/locale/foreground triggers.
 */
@AndroidEntryPoint
class NextPrayerWidget : AppWidgetProvider() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = NextPrayerRenderCache.lastData
        if (cached != null) {
            appWidgetManager.updateAppWidget(appWidgetIds, WidgetRenderer.buildNextPrayerWidget(context, cached))
            return
        }
        // Nothing cached yet — do a full update to render for the first time.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                widgetUpdater.update()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
