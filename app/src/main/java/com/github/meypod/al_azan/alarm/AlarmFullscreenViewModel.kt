package com.github.meypod.al_azan.alarm

import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.adhan.AdhanContract
import com.github.meypod.al_azan.adhan.AdhanFiringHandler
import com.github.meypod.al_azan.core.data.locale.LocalizedResources
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.repository.AlarmSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.playback.PlaybackService
import com.github.meypod.al_azan.reminder.ReminderFiringHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmFullscreenViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
    private val adhanFiringHandler: AdhanFiringHandler,
    private val reminderFiringHandler: ReminderFiringHandler,
    alarmSettingsRepository: AlarmSettingsRepository,
    settingsRepository: SettingsRepository,
    localizedResources: LocalizedResources,
) : ViewModel() {

    private val prayer: Prayer? =
        savedStateHandle.get<String>(PlaybackService.EXTRA_PRAYER)
            ?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }

    // Reminders reuse this screen but have no snooze and no *auto*-silence (manual "Dismiss & silent"
    // is still offered when DND access is held).
    private val isReminder: Boolean =
        savedStateHandle.get<Boolean>(PlaybackService.EXTRA_IS_REMINDER) == true

    // The time the alarm is for (adhan/prayer time, or reminder time), pre-formatted by the handler.
    private val timeLabel: String =
        savedStateHandle.get<String>(PlaybackService.EXTRA_TIME_LABEL).orEmpty()

    private val header: String
    private val title: String

    init {
        // The injected application context doesn't carry the per-app locale on pre-API 33; fallback
        // strings (the header fires for every adhan — the handler only passes one for reminders)
        // must resolve in the app language like the extras the handler localized.
        header = savedStateHandle.get<String>(PlaybackService.EXTRA_HEADER)
            ?.takeIf { it.isNotBlank() }
            ?: localizedResources.current.getString(R.string.adhan_channel_name)
        title = savedStateHandle.get<String>(PlaybackService.EXTRA_TITLE)
            ?.takeIf { it.isNotBlank() }
            ?: prayer?.let { localizedResources.current.getString(it.stringRes) }.orEmpty()
    }

    // "Dismiss & silent" actually puts the phone into Do Not Disturb, which needs policy access.
    private val dndAccessGranted: Boolean =
        context.getSystemService<NotificationManager>()?.isNotificationPolicyAccessGranted == true

    // When auto-silence is on, the dismiss button itself silences — so the manual pill is hidden and
    // OnDismiss routes through dismiss-and-silent. This is the raw setting; the handler re-checks DND
    // access at dismiss time and posts a notice if it was revoked.
    private var autoSilentOnDismiss = false
    private var autoSilentDurationMinutes = AdhanContract.DISMISS_SILENT_MINUTES

    private val _uiState = MutableStateFlow(
        AlarmFullscreenUiState(
            header = header,
            title = title,
            timeLabel = timeLabel,
            shortRemindMinutes = if (isReminder) 0 else AdhanContract.SHORT_REMIND_MINUTES,
            longRemindMinutes = if (isReminder) 0 else AdhanContract.LONG_REMIND_MINUTES,
            // Manual "Dismiss & silent" is offered for both adhans and reminders whenever the app can
            // change DND. (Auto-silence, below, stays adhan-only.)
            dismissAndSilentMinutes = if (dndAccessGranted) AdhanContract.DISMISS_SILENT_MINUTES else 0,
        ),
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.data.collect { settings ->
                _uiState.update { it.copy(themeColor = settings.themeColor) }
            }
        }
        // Auto-silence (and its dismiss-button wording) is an adhan-only concept.
        if (!isReminder) {
            viewModelScope.launch {
                alarmSettingsRepository.data.collect { alarmSettings ->
                    autoSilentOnDismiss = alarmSettings.autoSilentOnDismiss
                    autoSilentDurationMinutes = alarmSettings.autoSilentDurationMinutes
                    _uiState.update {
                        // Hide the manual pill when auto-silence is active (dismiss already silences) or
                        // when DND access is missing. The plain dismiss button reflects auto-silence only
                        // when it can actually take effect (DND access granted).
                        it.copy(
                            dismissAndSilentMinutes =
                                if (!autoSilentOnDismiss && dndAccessGranted) AdhanContract.DISMISS_SILENT_MINUTES else 0,
                            autoSilentOnDismiss = autoSilentOnDismiss && dndAccessGranted,
                        )
                    }
                }
            }
        }
    }

    private val _finish = Channel<Unit>(Channel.CONFLATED)
    val finish = _finish.receiveAsFlow()

    fun onAction(action: AlarmFullscreenUiAction) {
        when (action) {
            AlarmFullscreenUiAction.OnDismiss -> onDismiss()
            AlarmFullscreenUiAction.OnDismissAndSilent -> onDismissAndSilent()
            AlarmFullscreenUiAction.OnShortRemind -> onShortRemind()
            AlarmFullscreenUiAction.OnLongRemind -> onLongRemind()
        }
        _finish.trySend(Unit)
    }

    private fun onDismiss() =
        if (isReminder) {
            reminderFiringHandler.dismissFromUi()
        } else {
            adhanFiringHandler.dismissFromUi(autoSilentOnDismiss, autoSilentDurationMinutes)
        }

    private fun onDismissAndSilent() =
        if (isReminder) {
            reminderFiringHandler.dismissAndSilentFromUi(AdhanContract.DISMISS_SILENT_MINUTES)
        } else {
            adhanFiringHandler.dismissAndSilentFromUi(AdhanContract.DISMISS_SILENT_MINUTES)
        }

    private fun onShortRemind() {
        prayer?.let { adhanFiringHandler.remindLaterFromUi(it, AdhanContract.SHORT_REMIND_MINUTES) }
    }

    private fun onLongRemind() {
        prayer?.let { adhanFiringHandler.remindLaterFromUi(it, AdhanContract.LONG_REMIND_MINUTES) }
    }
}
