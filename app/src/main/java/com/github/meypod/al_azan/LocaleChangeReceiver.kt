package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.meypod.al_azan.widget.enqueueWidgetUpdate

/**
 * Redraws the widgets when the device locale changes so prayer names and digits re-localize.
 * Locale does not affect prayer times, so no alarm rescheduling is needed.
 */
class LocaleChangeReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action == Intent.ACTION_LOCALE_CHANGED) {
            enqueueWidgetUpdate(context)
        }
    }
}
