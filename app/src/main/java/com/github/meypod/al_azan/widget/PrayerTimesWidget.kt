package com.github.meypod.al_azan.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * Home-screen prayer-times app widget.
 *
 * Some launchers reset the widget to its initial layout and fire [onUpdate] repeatedly even with
 * `updatePeriodMillis="0"`. To keep content visible without spinning the full worker each time, an
 * [onUpdate] simply re-pushes the last rendered content from [WidgetRenderCache] (cheap, no
 * WorkManager / notification / alarm work). Only when there's nothing cached yet (fresh placement or
 * after process death) does it enqueue a full refresh. Ongoing content changes come from the worker
 * via the scheduled redraw alarm and the settings/boot/time/locale/foreground triggers.
 */
class PrayerTimesWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = WidgetRenderCache.lastData
        if (cached != null) {
            appWidgetManager.updateAppWidget(appWidgetIds, WidgetRenderer.buildScreenWidget(context, cached))
        } else {
            enqueueWidgetUpdate(context)
        }
    }
}
