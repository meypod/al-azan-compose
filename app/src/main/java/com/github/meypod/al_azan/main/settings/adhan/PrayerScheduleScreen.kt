package com.github.meypod.al_azan.main.settings.adhan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.model.adhan.toAdhanKey
import com.github.meypod.al_azan.core.domain.model.alarm.VibrationMode
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.mapper.stringRes
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
    val customMuezzin = uiState.settings.selectedAdhanEntries[prayer.toAdhanKey()]
    val soundDays = uiState.alarmSettings.getSoundSettings(prayer).selectedDays()
    val notifyDays = uiState.alarmSettings.getNotifSettings(prayer).selectedDays()
    val customVibration = uiState.alarmSettings.getVibrationSettings(prayer)

    val resources = LocalResources.current
    val defaultVibrationLabel = stringResource(R.string.use_default_vibration)
    val vibrationOptions = listOf<VibrationMode?>(null) + VibrationMode.entries

    val defaultLabel = stringResource(R.string.use_default_muezzin)
    val muezzinOptions = listOf<AudioEntry?>(null) + uiState.settings.savedAdhanAudioEntries
    val optionLabels = uiState.settings.savedAdhanAudioEntries.associate { entry ->
        entry.id to when (entry) {
            is AudioEntry.ResourceAudioEntry -> stringResource(entry.labelResId)
            is AudioEntry.ExternalAudioEntry -> entry.label
        }
    }

    val rowState = AdhanScheduleRowUiState.fromPrayerAlarmSettings(
        prayer,
        uiState.alarmSettings.getNotifSettings(prayer),
        uiState.alarmSettings.getSoundSettings(prayer),
    )

    ScreenScaffold(
        title = prayer.i18n(),
        onBackClick = { onAction(AdhanSettingsUiAction.OnBackClick) },
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
                onClick = { onAction(AdhanSettingsUiAction.OnNotifyClick(prayer)) },
            )
            AdhanToggleChip(
                label = stringResource(R.string.sound),
                state = rowState.soundState,
                onClick = { onAction(AdhanSettingsUiAction.OnSoundClick(prayer)) },
            )
        }

        HorizontalDivider()

        Column {
            SettingLabel(stringResource(R.string.custom_muezzin))
            BottomSelect(
                modifier = Modifier.fillMaxWidth(),
                options = muezzinOptions,
                optionKey = { it?.id ?: DEFAULT_MUEZZIN_KEY },
                optionLabel = { it?.let { e -> optionLabels[e.id] ?: e.id } ?: defaultLabel },
                selectedKey = customMuezzin?.id ?: DEFAULT_MUEZZIN_KEY,
                onSelect = { onAction(AdhanSettingsUiAction.OnScheduleMuezzinChange(prayer, it)) },
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
                onToggle = { onAction(AdhanSettingsUiAction.OnScheduleSoundDayToggle(prayer, it)) },
                accent = ChipAccent.Tertiary,
            )
        }

        HorizontalDivider()

        Column {
            SettingLabel(stringResource(R.string.notifications))
            WeekdayChipRow(
                selected = notifyDays,
                onToggle = { onAction(AdhanSettingsUiAction.OnScheduleNotifyDayToggle(prayer, it)) },
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
