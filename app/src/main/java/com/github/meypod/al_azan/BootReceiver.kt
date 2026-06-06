package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        // The OS clears all alarms on reboot; re-register tracked alarms and redraw the widgets.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                alarmRepository.rescheduleAll()
                widgetUpdater.update()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
