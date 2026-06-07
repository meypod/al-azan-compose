package com.github.meypod.al_azan.main.settings.developer

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.adhan.AdhanFiringHandler
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.util.device.VibrationController
import com.github.meypod.al_azan.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeveloperViewModel
@Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val adhanFiringHandler: AdhanFiringHandler,
    private val widgetUpdater: WidgetUpdater,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private companion object {
        const val TEST_DELAY_SECONDS = 10
    }

    fun onAction(action: DeveloperUiAction) {
        when (action) {
            DeveloperUiAction.OnBackClick -> NavigationController.navigateBack()

            DeveloperUiAction.OnFireAdhanNow -> viewModelScope.launch {
                adhanFiringHandler.devFireNow()
            }

            DeveloperUiAction.OnScheduleAdhanWithSound -> scheduleAdhan(playSound = true)

            DeveloperUiAction.OnScheduleAdhanNotifyOnly -> scheduleAdhan(playSound = false)

            DeveloperUiAction.OnPostUpcoming -> viewModelScope.launch {
                adhanFiringHandler.devPostUpcoming()
            }

            DeveloperUiAction.OnVibrateShort -> VibrationController.vibrate(context, VibrationMode.Once)

            DeveloperUiAction.OnVibrateLong -> VibrationController.vibrate(context, VibrationMode.Continuous)

            DeveloperUiAction.OnStopVibration -> VibrationController.stop(context)

            DeveloperUiAction.OnUpdateWidgets -> viewModelScope.launch { widgetUpdater.update() }

            DeveloperUiAction.OnDisableDeveloperMode -> viewModelScope.launch {
                settingsRepository.update { it.copy(devMode = false) }
                NavigationController.navigateBack()
            }
        }
    }

    private fun scheduleAdhan(playSound: Boolean) {
        viewModelScope.launch {
            adhanFiringHandler.devScheduleAdhan(playSound, TEST_DELAY_SECONDS)
            toast(context.getString(R.string.dev_scheduled_toast, TEST_DELAY_SECONDS))
        }
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
