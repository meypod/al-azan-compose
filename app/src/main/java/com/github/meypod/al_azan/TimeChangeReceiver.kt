package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.github.meypod.al_azan.core.domain.model.system.SystemChange
import com.github.meypod.al_azan.core.domain.repository.SystemChangeRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavIntent
import com.github.meypod.al_azan.core.presentation.navigation.Route
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.TimeZone
import javax.inject.Inject

@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {

    @Inject
    lateinit var systemChangeRepository: SystemChangeRepository

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
            }

            Intent.ACTION_TIME_CHANGED -> onTimeChanged()
        }
    }

    private fun onTimeZoneChanged(newZoneId: String) {
        systemChangeRepository.tryEmit(SystemChange.TimeZoneChanged(newZoneId))
    }

    private fun onTimeChanged() {
        systemChangeRepository.tryEmit(SystemChange.TimeChanged)
    }
}
