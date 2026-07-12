package com.github.meypod.al_azan.widget

import java.util.concurrent.ConcurrentHashMap

/**
 * Process-static current page index per custom-widget instance (appWidgetId → page). Drives the
 * tap ‹/› pagination for multi-location widgets. Not persisted: after process death it resets to the
 * first page, which is harmless.
 */
object CustomWidgetPageState {
    private val indexByWidgetId = ConcurrentHashMap<Int, Int>()

    fun get(appWidgetId: Int): Int = indexByWidgetId[appWidgetId] ?: 0

    fun set(
        appWidgetId: Int,
        index: Int,
    ) {
        indexByWidgetId[appWidgetId] = index
    }
}
