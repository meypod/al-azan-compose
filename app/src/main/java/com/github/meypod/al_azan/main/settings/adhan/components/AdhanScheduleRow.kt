package com.github.meypod.al_azan.main.settings.adhan.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.model.alarm.PrayerAlarmSettings
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.main.settings.adhan.AdhanSettingsUiAction

@Immutable
data class AdhanScheduleRowUiState(
    val prayer: Prayer,
    val notifyState: ToggleableState,
    val soundState: ToggleableState,
) {
    companion object {
        fun fromPrayerAlarmSettings(
            prayer: Prayer,
            notifSettings: PrayerAlarmSettings,
            soundSettings: PrayerAlarmSettings,
        ) = AdhanScheduleRowUiState(
            prayer,
            if (notifSettings is PrayerAlarmSettings.Bool) ToggleableState(notifSettings.value) else ToggleableState.Indeterminate,
            if (soundSettings is PrayerAlarmSettings.Bool) ToggleableState(soundSettings.value) else ToggleableState.Indeterminate,
        )
    }
}

sealed interface AdhanScheduleRowUiAction {
    object OnNotifyClick : AdhanScheduleRowUiAction
    object OnSoundClick : AdhanScheduleRowUiAction
    object OnCogClick : AdhanScheduleRowUiAction
}

fun AdhanScheduleRowUiAction.toAdhanSettingsUiAction(prayer: Prayer) =
    when (this) {
        AdhanScheduleRowUiAction.OnNotifyClick -> AdhanSettingsUiAction.OnNotifyClick(prayer)
        AdhanScheduleRowUiAction.OnSoundClick -> AdhanSettingsUiAction.OnSoundClick(prayer)
        AdhanScheduleRowUiAction.OnCogClick -> AdhanSettingsUiAction.OnCogClick(prayer)
    }

@Composable
fun AdhanScheduleRow(
    state: AdhanScheduleRowUiState,
    onAction: (AdhanScheduleRowUiAction) -> Unit,
) {
    FlowRow(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        SettingLabel(state.prayer.i18n(), fontWeight = FontWeight.Medium)

        FlowRow(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tiny_padding)),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            AdhanToggleChip(
                label = stringResource(R.string.notification),
                state = state.notifyState,
                onClick = { onAction(AdhanScheduleRowUiAction.OnNotifyClick) },
            )
            AdhanToggleChip(
                label = stringResource(R.string.sound),
                state = state.soundState,
                onClick = { onAction(AdhanScheduleRowUiAction.OnSoundClick) },
            )

            IconButton(
                onClick = {
                    onAction(AdhanScheduleRowUiAction.OnCogClick)
                },
            ) {
                Icon(
                    painterResource(R.drawable.settings_filled),
                    contentDescription =
                        stringResource(R.string.prayer_setting_accessibility, state.prayer.i18n()),
                )
            }
        }
    }
}

@Composable
fun AdhanToggleChip(
    label: String,
    state: ToggleableState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tiny_padding)),
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Checkbox }
            .padding(dimensionResource(R.dimen.tiny_padding)),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        TriStateCheckbox(state, null)
    }
}

@Preview
@Composable
private fun AdhanScheduleRowPreview() {
    AlAzanTheme {
        ACard {
            Column(Modifier.padding(it), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AdhanScheduleRow(AdhanScheduleRowUiState(Prayer.Fajr, ToggleableState(true), ToggleableState(true)), {})
                HorizontalDivider()
                AdhanScheduleRow(AdhanScheduleRowUiState(Prayer.Sunrise, ToggleableState.Indeterminate, ToggleableState(false)), {})
            }
        }
    }
}
