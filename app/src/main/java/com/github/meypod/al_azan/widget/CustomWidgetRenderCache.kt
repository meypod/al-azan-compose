package com.github.meypod.al_azan.widget

import com.github.meypod.al_azan.core.domain.model.widget.CustomWidgetData

/**
 * Process-static cache of the last data rendered to the custom widget, mirroring [WidgetRenderCache].
 * Lets [CustomWidget.onUpdate] cheaply re-push the current content when the launcher resets the widget
 * to its initial layout, without running the full [WidgetUpdater]. Not persisted.
 */
object CustomWidgetRenderCache {
    @Volatile
    var lastData: CustomWidgetData? = null
}
