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
 * Home-screen prayer-times app widget.
 *
 * Some launchers reset the widget to its initial layout and fire [onUpdate] repeatedly even with
 * `updatePeriodMillis="0"`. So [onUpdate] cheaply **re-pushes the last rendered content** from
 * [WidgetRenderCache] — no full recompute, just RemoteViews — which both repaints after a host reset
 * and stays inert against the spam. Only when nothing is cached yet (fresh placement or after process
 * death) does it run a full [WidgetUpdater.update]. Ongoing content changes come from the redraw alarm
 * and the settings/boot/time/locale/foreground triggers.
 */
@AndroidEntryPoint
class PrayerTimesWidget : AppWidgetProvider() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = WidgetRenderCache.lastData
        if (cached != null) {
            appWidgetManager.updateAppWidget(appWidgetIds, WidgetRenderer.buildScreenWidget(context, cached))
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
