package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.meypod.al_azan.core.domain.model.system.SystemChange
import com.github.meypod.al_azan.core.domain.repository.AlarmRepository
import com.github.meypod.al_azan.core.domain.repository.SystemChangeRepository
import com.github.meypod.al_azan.widget.WidgetUpdater
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var systemChangeRepository: SystemChangeRepository

    @Inject
    lateinit var alarmRepository: AlarmRepository

    @Inject
    lateinit var widgetUpdater: WidgetUpdater

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        when (intent?.action) {
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val newTimezoneId = (
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        intent.getStringExtra(Intent.EXTRA_TIMEZONE)
                    } else {
                        null
                    }
                    ) ?: TimeZone.getDefault().id

                onTimeZoneChanged(newTimezoneId)
                refreshWidgets(context)
            }

            Intent.ACTION_TIME_CHANGED -> {
                onTimeChanged()
                refreshWidgets(context)
            }
        }
    }

    /** Wall-clock changes invalidate both the scheduled redraw alarm and the rendered times. */
    private fun refreshWidgets(context: Context?) {
        if (context == null) return
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

    private fun onTimeZoneChanged(newZoneId: String) {
        systemChangeRepository.tryEmit(SystemChange.TimeZoneChanged(newZoneId))
    }

    private fun onTimeChanged() {
        systemChangeRepository.tryEmit(SystemChange.TimeChanged)
    }
}
