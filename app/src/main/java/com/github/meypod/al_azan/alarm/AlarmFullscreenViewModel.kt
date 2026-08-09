package com.github.meypod.al_azan.alarm

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.adhan.AdhanFiringHandler
import com.github.meypod.al_azan.core.data.locale.LocalizedResources
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.playback.PlaybackService
import com.github.meypod.al_azan.reminder.ReminderFiringHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlarmFullscreenViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val adhanFiringHandler: AdhanFiringHandler,
    private val reminderFiringHandler: ReminderFiringHandler,
    alarmSettingsRepository: AlarmSettingsRepository,
    settingsRepository: SettingsRepository,
    private val localizedResources: LocalizedResources,
) : ViewModel() {

    // "Dismiss & silent" actually puts the phone into Do Not Disturb, which needs policy access.
    private val dndAccessGranted: Boolean =
        context.getSystemService<NotificationManager>()?.isNotificationPolicyAccessGranted == true

    /**
     * Whether an alarm is still sounding. The screen exists to deal with one, so it closes when there is
     * none — dismissed here, dismissed from the notification, stopped by a volume button, or simply over.
     */
    val isSounding: StateFlow<Boolean> = PlaybackService.activeAlarm
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackService.activeAlarm.value != null)

    /**
     * The alarm being shown: the last one to sound, held past its retirement so the screen keeps its
     * content for the moment between the user acting and the activity finishing, instead of blanking out.
     */
    private val shownAlarm: StateFlow<PlaybackService.ActiveAlarm?> = PlaybackService.activeAlarm
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlaybackService.activeAlarm.value)

    private val alarmSettings: StateFlow<AlarmSettings> = alarmSettingsRepository.data
        .stateIn(viewModelScope, SharingStarted.Eagerly, AlarmSettings())

    /**
     * Derived from every input at once. A second alarm can replace the first while this screen is up, so
     * the dismiss wording has to be recomputed against the alarm being shown and not only when the setting
     * behind it changes — otherwise the button stops saying that it silences while it still does.
     */
    val uiState: StateFlow<AlarmFullscreenUiState> =
        combine(shownAlarm, alarmSettings, settingsRepository.data.map { it.themeColor }, ::buildUiState)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                buildUiState(shownAlarm.value, alarmSettings.value, ThemeColor.Default),
            )

    /**
     * Reminders reuse this screen but have no snooze and no *auto*-silence; manual "Dismiss & silent" is
     * offered for both whenever the app can change DND.
     *
     * The strings arrive pre-resolved from the firing side, the only place that knows the app language on
     * the background contexts an alarm fires from; the fallbacks resolve here.
     */
    private fun buildUiState(
        alarm: PlaybackService.ActiveAlarm?,
        settings: AlarmSettings,
        themeColor: ThemeColor,
    ): AlarmFullscreenUiState {
        val isReminder = alarm?.isReminder == true
        // Only reflect auto-silence where it can actually take effect. The manual pill is then redundant,
        // since dismissing already silences.
        val autoSilent = !isReminder && settings.autoSilentOnDismiss && dndAccessGranted
        return AlarmFullscreenUiState(
            header = alarm?.header?.takeIf { it.isNotBlank() }
                ?: localizedResources.current.getString(R.string.adhan_channel_name),
            title = alarm?.title?.takeIf { it.isNotBlank() }
                ?: alarm?.prayer?.let { localizedResources.current.getString(it.stringRes) }.orEmpty(),
            timeLabel = alarm?.timeLabel.orEmpty(),
            shortRemindMinutes = if (isReminder) 0 else AdhanContract.SHORT_REMIND_MINUTES,
            longRemindMinutes = if (isReminder) 0 else AdhanContract.LONG_REMIND_MINUTES,
            dismissAndSilentMinutes = if (!autoSilent && dndAccessGranted) AdhanContract.DISMISS_SILENT_MINUTES else 0,
            autoSilentOnDismiss = autoSilent,
            themeColor = themeColor,
        )
    }

    fun onAction(action: AlarmFullscreenUiAction) {
        val alarm = shownAlarm.value
        // Every action here ends the alarm. Retire it before the handler stops playback, so the app doesn't
        // re-open this screen in the gap before the service tears down.
        alarm?.let { PlaybackService.markAlarmHandled(it.id) }
        val isReminder = alarm?.isReminder == true
        when (action) {
            AlarmFullscreenUiAction.OnDismiss -> onDismiss(isReminder)
            AlarmFullscreenUiAction.OnDismissAndSilent -> onDismissAndSilent(isReminder)
            AlarmFullscreenUiAction.OnShortRemind -> onRemindLater(alarm, AdhanContract.SHORT_REMIND_MINUTES)
            AlarmFullscreenUiAction.OnLongRemind -> onRemindLater(alarm, AdhanContract.LONG_REMIND_MINUTES)
        }
    }

    // Passes the raw setting, not the gated one [buildUiState] shows: the handler re-checks DND access at
    // dismiss time and posts a notice if it was revoked while this screen was up.
    private fun onDismiss(isReminder: Boolean) =
        if (isReminder) {
            reminderFiringHandler.dismissFromUi()
        } else {
            val settings = alarmSettings.value
            adhanFiringHandler.dismissFromUi(settings.autoSilentOnDismiss, settings.autoSilentDurationMinutes)
        }

    private fun onDismissAndSilent(isReminder: Boolean) =
        if (isReminder) {
            reminderFiringHandler.dismissAndSilentFromUi(AdhanContract.DISMISS_SILENT_MINUTES)
        } else {
            adhanFiringHandler.dismissAndSilentFromUi(AdhanContract.DISMISS_SILENT_MINUTES)
        }

    private fun onRemindLater(
        alarm: PlaybackService.ActiveAlarm?,
        minutes: Int,
    ) {
        alarm?.prayer?.let { adhanFiringHandler.remindLaterFromUi(it, minutes) }
    }
}
