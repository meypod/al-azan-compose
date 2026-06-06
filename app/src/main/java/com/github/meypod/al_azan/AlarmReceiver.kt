package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.meypod.al_azan.widget.WidgetContract
import com.github.meypod.al_azan.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Entry point for every fired [com.github.meypod.al_azan.core.domain.model.alarm.ScheduledAlarm].
 *
 * AlarmManager starts the process to deliver this broadcast even after a memory-kill, so the work runs
 * in the same wake-up — no deferrable worker in between.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    private companion object {
        const val TAG = "AlarmReceiver"
    }

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        when (intent?.action) {
            WidgetContract.ACTION_WIDGET_UPDATE -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        widgetUpdater.update()
                    } catch (e: Exception) {
                        Log.e(TAG, "Widget update failed", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
