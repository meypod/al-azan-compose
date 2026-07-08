package com.github.meypod.al_azan.main.settings.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.WidgetCityNamePos
import com.github.meypod.al_azan.core.domain.model.settings.i18n
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.LocalSnackbarController
import com.github.meypod.al_azan.core.presentation.components.PrayerCheckboxTable
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun TableAppearanceScreen(
    uiState: WidgetSettingsUiState,
    onAction: (WidgetSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
    events: Flow<WidgetSettingsUiEvent> = emptyFlow(),
) {
    val resources = LocalResources.current
    val snackbarController = LocalSnackbarController.current
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is WidgetSettingsUiEvent.ShowMessage ->
                    snackbarController.show(resources.getString(event.messageRes))
            }
        }
    }
    ScreenScaffold(
        title = stringResource(R.string.widget_section_table_appearance),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        ACard { cardPadding ->
            Column(
                Modifier.padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                SettingSwitch(
                    title = stringResource(R.string.show_countdown_timer),
                    subtitle = null,
                    checked = uiState.settings.showWidgetCountdown,
                    onCheckedChange = { onAction(WidgetSettingsUiAction.OnShowCountdownToggle(it)) },
                )
                SettingSwitch(
                    title = stringResource(R.string.widget_swap_layout_direction),
                    subtitle = null,
                    checked = uiState.settings.swapWidgetLayoutDirection,
                    onCheckedChange = { onAction(WidgetSettingsUiAction.OnSwapLayoutDirectionToggle(it)) },
                )
                SettingSwitch(
                    title = stringResource(R.string.highlight_current_prayer_time),
                    subtitle = stringResource(R.string.highlight_current_prayer_time_help),
                    checked = uiState.settings.highlightCurrentPrayerWidget,
                    onCheckedChange = { onAction(WidgetSettingsUiAction.OnHighlightCurrentPrayerToggle(it)) },
                )
                SettingSwitch(
                    title = stringResource(R.string.hijri_day_starts_at_maghrib),
                    subtitle = null,
                    checked = uiState.settings.widgetHijriDayStartsAtMaghrib,
                    onCheckedChange = { onAction(WidgetSettingsUiAction.OnHijriDayStartsAtMaghribToggle(it)) },
                )
            }
        }

        ACard { cardPadding ->
            BottomSelect(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                options = WidgetCityNamePos.entries,
                optionKey = { it.name },
                optionLabel = { it.i18n(resources) },
                selectedKey = uiState.settings.widgetCityNamePos.name,
                onSelect = { onAction(WidgetSettingsUiAction.OnCityNamePosChange(it)) },
                label = { Text(stringResource(R.string.widget_city_name_pos_label)) },
                placeholder = stringResource(R.string.widget_city_name_pos_placeholder),
                supportingText = { Text(stringResource(R.string.widget_city_name_pos_help)) },
            )
        }

        ACard { cardPadding ->
            PrayerCheckboxTable(
                title = stringResource(R.string.widget_show_prayer_times),
                helpText = stringResource(R.string.widget_show_prayer_times_help),
                leftColumn = stringResource(R.string.time_column),
                rightColumn = stringResource(R.string.show_column),
                isChecked = { it !in uiState.settings.hiddenWidgetPrayers },
                onToggle = { prayer, visible ->
                    onAction(WidgetSettingsUiAction.OnPrayerVisibilityChange(prayer, visible))
                },
                modifier = Modifier.padding(cardPadding),
            )
        }
    }
}

@Preview
@Composable
private fun TableAppearanceScreenPreview() {
    AlAzanThemePreview {
        TableAppearanceScreen(uiState = WidgetSettingsUiState(), onAction = {})
    }
}
