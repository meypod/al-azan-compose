package com.github.meypod.al_azan.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * User-authored custom home-screen widget. Mirrors [PrayerTimesWidget]: [onUpdate] cheaply re-pushes
 * the last rendered content from [CustomWidgetRenderCache] (repaints after a launcher reset without a
 * full recompute, honoring each instance's current page), and only runs a full [WidgetUpdater.update]
 * when nothing is cached yet. Also handles the multi-location ‹/› page taps.
 */
@AndroidEntryPoint
class CustomWidget : AppWidgetProvider() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == CustomWidgetRenderer.ACTION_PAGE) {
            handlePageTap(context, intent)
            return
        }
        super.onReceive(context, intent)
    }

    /** Flip the tapped widget by ±1 location page and re-render it from the cached data. */
    private fun handlePageTap(
        context: Context,
        intent: Intent,
    ) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val delta = intent.getIntExtra(CustomWidgetRenderer.EXTRA_DELTA, 0)
        val data = CustomWidgetRenderCache.lastData
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || data == null || data.pages.size <= 1) return

        val newIndex = CustomWidgetPageState.get(appWidgetId) + delta
        CustomWidgetPageState.set(appWidgetId, newIndex)
        AppWidgetManager.getInstance(context)
            .updateAppWidget(appWidgetId, CustomWidgetRenderer.build(context, data, newIndex, appWidgetId))
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val cached = CustomWidgetRenderCache.lastData
        if (cached != null) {
            appWidgetIds.forEach { id ->
                appWidgetManager.updateAppWidget(id, CustomWidgetRenderer.build(context, cached, CustomWidgetPageState.get(id), id))
            }
            return
        }
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
