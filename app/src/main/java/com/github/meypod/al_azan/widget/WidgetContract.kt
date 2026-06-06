package com.github.meypod.al_azan.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

/** Shared identifiers for the prayer-times widgets. */
object WidgetContract {
    /** Broadcast action used by scheduled redraw alarms. */
    const val ACTION_WIDGET_UPDATE = "com.github.meypod.al_azan.action.WIDGET_UPDATE"

    /** Stable id of the redraw alarm tracked by the AlarmRepository. */
    const val REDRAW_ALARM_ID = "widget_redraw"

    /** Notification id for the persistent notification widget. */
    const val NOTIFICATION_ID = "prayer_times_widget_notification"

    /** Unique WorkManager name; new requests replace the in-flight one. */
    const val WORK_NAME = "widget_update_work"
}

/** Enqueues an expedited one-off widget refresh, replacing any pending one. */
fun enqueueWidgetUpdate(context: Context) {
    val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork(WidgetContract.WORK_NAME, ExistingWorkPolicy.REPLACE, request)
}
