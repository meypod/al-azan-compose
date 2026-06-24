package com.github.meypod.al_azan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import io.github.meypod.adhan_kotlin.CalculationMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug-only setup for store-screenshot runs (see fastlane/sctool/create_screenshots.sh):
 *
 * - SETUP_SCREENSHOTS skips the intro and applies the preset the screenshots are based on — Mecca
 *   location, Muslim World League method, the five daily prayers with notification + sound, and the
 *   notification widget.
 * - RESET_INTRO clears the onboarding flags so the app cold-starts back into the intro, used to
 *   capture the language-selection (first intro) screenshot.
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
    lateinit var schedulerReconciler: SchedulerReconciler

    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent?.action) {
                    ACTION_SETUP_SCREENSHOTS -> {
                        applyPreset()
                        schedulerReconciler.reconcileAll()
                    }

                    ACTION_RESET_INTRO -> resetIntro()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun resetIntro() {
        settingsRepository.update {
            it.copy(appInitialConfigDone = false, appIntroDone = false)
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
        const val ACTION_RESET_INTRO = "com.github.meypod.al_azan.action.RESET_INTRO"
        const val LOCATION_ID = "screenshot-mecca"
    }
}
