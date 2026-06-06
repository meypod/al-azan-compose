package com.github.meypod.al_azan.widget

import com.github.meypod.al_azan.core.domain.model.widget.WidgetData

/**
 * Process-static cache of the last data rendered to the home-screen widget. Lets
 * [PrayerTimesWidget.onUpdate] cheaply re-push the current content when the launcher resets the widget
 * to its initial layout (which some launchers do repeatedly), without spinning up the full worker.
 *
 * Not persisted: after process death there is nothing to re-push, so onUpdate falls back to enqueueing
 * a full refresh.
 */
object WidgetRenderCache {
    @Volatile
    var lastData: WidgetData? = null
}
