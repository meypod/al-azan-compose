package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.ReminderRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import io.github.meypod.adhan_kotlin.CalculationMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug-only one-shot setup for store-screenshot runs (see fastlane/sctool/create_screenshots.sh):
 * skips the intro and applies the preset the screenshots are based on — Mecca location, Muslim World
 * League method, the five daily prayers with notification + sound, one example reminder, and the
 * notification widget. Trigger:
 *
 * adb shell am broadcast -a com.github.meypod.al_azan.action.SETUP_SCREENSHOTS \
 *     -n com.github.meypod.al_azan.debug/com.github.meypod.al_azan.ScreenshotSetupReceiver
 */
@AndroidEntryPoint
class ScreenshotSetupReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var alarmSettingsRepository: AlarmSettingsRepository

    @Inject
    lateinit var calculationSettingsRepository: CalculationSettingsRepository

    @Inject
    lateinit var favoriteLocationsRepository: FavoriteLocationsRepository

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var schedulerReconciler: SchedulerReconciler

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != ACTION_SETUP_SCREENSHOTS) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                applyPreset()
                schedulerReconciler.reconcileAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun applyPreset() {
        val mecca = StaticFavoriteLocation(
            id = LOCATION_ID,
            locationDetail = CalculationLocationDetail(
                lat = 21.42664,
                long = 39.82563,
                label = "Mecca",
            ),
        )
        favoriteLocationsRepository.update { locations ->
            listOf(mecca) + locations.filter { it.id != LOCATION_ID }
        }
        calculationSettingsRepository.update {
            it.copy(
                locationId = LOCATION_ID,
                parameters = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters,
            )
        }

        val on = PrayerAlarmSettings.Bool(true)
        alarmSettingsRepository.update {
            it.copy(
                fajrNotify = on,
                fajrSound = on,
                dhuhrNotify = on,
                dhuhrSound = on,
                asrNotify = on,
                asrSound = on,
                maghribNotify = on,
                maghribSound = on,
                ishaNotify = on,
                ishaSound = on,
            )
        }

        // example reminder for the reminders screenshot; keep whatever is already there on re-runs
        reminderRepository.update { reminders ->
            if (reminders.isNotEmpty()) {
                reminders
            } else {
                listOf(
                    Reminder(
                        id = REMINDER_ID,
                        enabled = true,
                        prayer = Prayer.Fajr,
                        duration = 5,
                        durationModifier = -1,
                        once = true,
                    ),
                )
            }
        }

        settingsRepository.update {
            it.copy(
                appInitialConfigDone = true,
                appIntroDone = true,
                showWidget = true,
                // suppress every "ask me" dialog so no popup lands in a screenshot
                dontAskPermissionNotifications = true,
                dontAskPermissionAlarm = true,
                dontAskPermissionPhoneState = true,
                dontAskPermissionFullScreenIntent = true,
                dontAskPermissionDndAccess = true,
                dontAskPermissionBatteryOptimization = true,
            )
        }
    }

    private companion object {
        const val ACTION_SETUP_SCREENSHOTS = "com.github.meypod.al_azan.action.SETUP_SCREENSHOTS"
        const val LOCATION_ID = "screenshot-mecca"
        const val REMINDER_ID = "screenshot-reminder"
    }
}
