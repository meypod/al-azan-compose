package com.github.meypod.al_azan.widget

import com.github.meypod.al_azan.core.domain.model.widget.NextPrayerWidgetData

/**
 * Process-static cache of the last data rendered to the 1x1 next-prayer widget. Lets
 * [NextPrayerWidget.onUpdate] cheaply re-push the current content when the launcher resets the widget
 * to its initial layout, without spinning up the full updater.
 *
 * Not persisted: after process death there is nothing to re-push, so onUpdate falls back to a full
 * refresh. Mirrors [WidgetRenderCache].
 */
object NextPrayerRenderCache {
    @Volatile
    var lastData: NextPrayerWidgetData? = null
}
