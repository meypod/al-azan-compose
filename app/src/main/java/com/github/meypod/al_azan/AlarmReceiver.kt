package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.meypod.al_azan.widget.WidgetContract
import com.github.meypod.al_azan.widget.enqueueWidgetUpdate

/**
 * Entry point for every fired [com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm].
 * Dispatches to the right work based on the broadcast action.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        when (intent?.action) {
            WidgetContract.ACTION_WIDGET_UPDATE -> enqueueWidgetUpdate(context)
        }
    }
}
