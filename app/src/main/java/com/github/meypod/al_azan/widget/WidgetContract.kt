package com.github.meypod.al_azan.widget

/** Shared identifiers for the prayer-times widgets. */
object WidgetContract {
    /** Broadcast action for the scheduled widget redraw, dispatched to AlarmReceiver. */
    const val ACTION_WIDGET_UPDATE = "com.github.meypod.al_azan.action.WIDGET_UPDATE"

    /** Stable id of the redraw alarm tracked by the AlarmRepository. */
    const val REDRAW_ALARM_ID = "widget_redraw"

    /** Notification id for the persistent notification widget. */
    const val NOTIFICATION_ID = "prayer_times_widget_notification"
}
