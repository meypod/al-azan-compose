package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.meypod.al_azan.adhan.AdhanScheduler
import com.github.meypod.al_azan.ramadan.RamadanNoticeScheduler
import com.github.meypod.al_azan.reminder.ReminderScheduler
import com.github.meypod.al_azan.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    @Inject
    lateinit var adhanScheduler: AdhanScheduler

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @Inject
    lateinit var ramadanNoticeScheduler: RamadanNoticeScheduler

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        // The OS clears all alarms on reboot; recompute the adhan schedule and redraw the widgets.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                adhanScheduler.schedule()
                reminderScheduler.schedule()
                ramadanNoticeScheduler.schedule()
                widgetUpdater.update()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
