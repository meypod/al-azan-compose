package com.github.meypod.al_azan.main.settings.adhan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.AdhanKey
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.model.adhan.toAdhanKey
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.domain.model.settings.isResolvable
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.AudioPickerField
import com.github.meypod.al_azan.core.presentation.components.AudioPickerSection
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermissionSteps
import com.github.meypod.al_azan.core.presentation.dialog.isDontAskAgain
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest
import com.github.meypod.al_azan.core.presentation.mapper.stringRes
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.main.settings.adhan.components.AdhanScheduleRowUiState
import com.github.meypod.al_azan.main.settings.adhan.components.AdhanToggleChip
import com.github.meypod.al_azan.main.settings.adhan.components.ChipAccent
import com.github.meypod.al_azan.main.settings.adhan.components.WeekdayChipRow

private const val DEFAULT_MUEZZIN_KEY = "__default__"
private const val DEFAULT_VIBRATION_KEY = "__default__"

@Composable
fun PrayerScheduleScreen(
    prayer: Prayer,
    uiState: AdhanSettingsUiState,
    onAction: (AdhanSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val globalDefaultMuezzin = uiState.settings.selectedAdhanEntries[AdhanKey.Default]
    // An unresolvable override (orphaned/corrupted) is treated as no override, so the row falls back to
    // the global default instead of showing "unknown" — matching what actually plays (resolveSound).
    val customMuezzin = uiState.settings.selectedAdhanEntries[prayer.toAdhanKey()]?.takeIf { it.isResolvable() }
    val soundDays = uiState.alarmSettings.getSoundSettings(prayer).selectedDays()
    val notifyDays = uiState.alarmSettings.getNotifSettings(prayer).selectedDays()
    val customVibration = uiState.alarmSettings.getVibrationSettings(prayer)

    val resources = LocalResources.current
    val defaultVibrationLabel = stringResource(R.string.use_default_vibration)
    val vibrationOptions = listOf<VibrationMode?>(null) + VibrationMode.entries

    val defaultLabel = stringResource(R.string.use_default_muezzin)
    val labelFn = audioEntryLabel()
    val userIds = uiState.settings.savedUserAudioEntries.map { it.id }.toSet()
    val muezzinSections = listOf<AudioPickerSection<AudioEntry?>>(
        AudioPickerSection(null, listOf<AudioEntry?>(null)),
        AudioPickerSection(stringResource(R.string.muezzin), uiState.settings.savedAdhanAudioEntries),
        AudioPickerSection(stringResource(R.string.your_sounds), uiState.settings.savedUserAudioEntries),
        AudioPickerSection(stringResource(R.string.device_sounds), uiState.deviceSounds),
    )

    val rowState = AdhanScheduleRowUiState.fromPrayerAlarmSettings(
        prayer,
        uiState.alarmSettings.getNotifSettings(prayer),
        uiState.alarmSettings.getSoundSettings(prayer),
    )

    // Enabling any schedule here needs notification + exact-alarm permissions, same flow as the
    // widget. guardEnable runs the flow on enable and stores how to snap the toggle back off.
    val pendingRevert = remember { mutableStateOf<(() -> Unit)?>(null) }
    val requestPermissions = rememberSchedulingPermissionRequest(
        // Required perms keep asking in the enable flow; only the optional phone-state permission honors
        // (and offers) "don't ask again" here — the flow special-cases it.
        isDontAskAgain = { uiState.settings.isDontAskAgain(it) },
        onDontAskAgain = { onAction(AdhanSettingsUiAction.OnPermissionDontAskAgain(it)) },
        onComplete = { results -> if (!results.requiredAllGranted()) pendingRevert.value?.invoke() },
    )

    fun guardEnable(
        enabling: Boolean,
        revert: () -> Unit,
    ) {
        if (enabling) {
            pendingRevert.value = revert
            requestPermissions(SchedulingPermissionSteps.adhan)
        }
    }

    ScreenScaffold(
        title = prayer.i18n(),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding), Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdhanToggleChip(
                label = stringResource(R.string.notification),
                state = rowState.notifyState,
                onClick = {
                    val enabling = rowState.notifyState != ToggleableState.On
                    onAction(AdhanSettingsUiAction.OnNotifyClick(prayer))
                    guardEnable(enabling) { onAction(AdhanSettingsUiAction.OnNotifyClick(prayer)) }
                },
            )
            AdhanToggleChip(
                label = stringResource(R.string.sound),
                state = rowState.soundState,
                onClick = {
                    val enabling = rowState.soundState != ToggleableState.On
                    val notifyWasOn = rowState.notifyState == ToggleableState.On
                    onAction(AdhanSettingsUiAction.OnSoundClick(prayer))
                    guardEnable(enabling) {
                        onAction(AdhanSettingsUiAction.OnSoundClick(prayer))
                        // enabling sound force-enables notify; clear it unless it was already on
                        if (!notifyWasOn) onAction(AdhanSettingsUiAction.OnNotifyClick(prayer))
                    }
                },
            )
        }

        HorizontalDivider()

        Column {
            SettingLabel(stringResource(R.string.custom_muezzin))
            AudioPickerField(
                modifier = Modifier.fillMaxWidth(),
                sections = muezzinSections,
                selectedKey = customMuezzin?.id ?: DEFAULT_MUEZZIN_KEY,
                playingId = uiState.playingId,
                optionKey = { it?.id ?: DEFAULT_MUEZZIN_KEY },
                optionLabel = { it?.let(labelFn) ?: defaultLabel },
                optionSubtitle = { if (it == null) globalDefaultMuezzin?.let(labelFn) else null },
                // The "use default" item previews the resolved global muezzin, so its play/stop state
                // tracks that sound's id rather than the synthetic default key.
                optionPreviewKey = { it?.id ?: globalDefaultMuezzin?.id ?: DEFAULT_MUEZZIN_KEY },
                optionCanDelete = { it != null && it.id in userIds },
                onSelect = { onAction(AdhanSettingsUiAction.OnScheduleMuezzinChange(prayer, it)) },
                onPreview = { (it ?: globalDefaultMuezzin)?.let { e -> onAction(AdhanSettingsUiAction.OnPreviewAudio(e)) } },
                onStopPreview = { onAction(AdhanSettingsUiAction.OnStopPreview) },
                onAddLocalFile = { filepath, name ->
                    onAction(AdhanSettingsUiAction.OnAddPrayerMuezzinFile(prayer, filepath, name))
                },
                onDelete = { it?.let { e -> onAction(AdhanSettingsUiAction.OnDeleteUserAudio(e)) } },
            )
        }

        Column {
            SettingLabel(stringResource(R.string.vibration_mode))
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = vibrationOptions,
                optionKey = { it?.name ?: DEFAULT_VIBRATION_KEY },
                optionLabel = { it?.let { mode -> resources.getString(mode.stringRes()) } ?: defaultVibrationLabel },
                selectedKey = customVibration?.name ?: DEFAULT_VIBRATION_KEY,
                onSelect = { onAction(AdhanSettingsUiAction.OnScheduleVibrationChange(prayer, it)) },
            )
        }

        Column {
            SettingLabel(stringResource(R.string.adhan))
            WeekdayChipRow(
                selected = soundDays,
                onToggle = { day ->
                    val enabling = day !in soundDays
                    val notifyHadDay = day in notifyDays
                    onAction(AdhanSettingsUiAction.OnScheduleSoundDayToggle(prayer, day))
                    guardEnable(enabling) {
                        onAction(AdhanSettingsUiAction.OnScheduleSoundDayToggle(prayer, day))
                        // enabling a sound day force-enables its notify day; clear it unless it was already on
                        if (!notifyHadDay) onAction(AdhanSettingsUiAction.OnScheduleNotifyDayToggle(prayer, day))
                    }
                },
                accent = ChipAccent.Tertiary,
            )
        }

        HorizontalDivider()

        Column {
            SettingLabel(stringResource(R.string.notifications))
            WeekdayChipRow(
                selected = notifyDays,
                onToggle = { day ->
                    val enabling = day !in notifyDays
                    onAction(AdhanSettingsUiAction.OnScheduleNotifyDayToggle(prayer, day))
                    guardEnable(enabling) { onAction(AdhanSettingsUiAction.OnScheduleNotifyDayToggle(prayer, day)) }
                },
            )
        }
    }
}

@Preview
@Composable
private fun PrayerScheduleScreenPreview() {
    AlAzanTheme {
        PrayerScheduleScreen(
            prayer = Prayer.Fajr,
            uiState = AdhanSettingsUiState(),
            onAction = {},
        )
    }
}
