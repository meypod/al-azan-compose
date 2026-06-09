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
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermissionSteps
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun WidgetSettingsScreen(
    uiState: WidgetSettingsUiState,
    onAction: (WidgetSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
    events: Flow<WidgetSettingsUiEvent> = emptyFlow(),
) {
    val resources = LocalResources.current
    val snackbarController = LocalSnackbarController.current
    // Snap the toggle back off when a required permission is missing, so the user can retry cleanly.
    val requestWidgetPermissions = rememberSchedulingPermissionRequest(
        // "Don't ask again" is offered/respected only by the home re-check (allowDontAskAgain stays false here).
        isDontAskAgain = { false },
        onDontAskAgain = {},
        onComplete = { results ->
            if (!results.requiredAllGranted()) onAction(WidgetSettingsUiAction.OnShowNotificationWidgetToggle(false))
        },
    )
    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is WidgetSettingsUiEvent.ShowMessage ->
                    snackbarController.show(resources.getString(event.messageRes))
            }
        }
    }
    ScreenScaffold(
        title = stringResource(R.string.widget_settings_title),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        ACard { cardPadding ->
            Column(Modifier.padding(cardPadding)) {
                SettingSwitch(
                    title = stringResource(R.string.show_notification_widget),
                    subtitle = null,
                    checked = uiState.settings.showWidget,
                    onCheckedChange = { enabled ->
                        onAction(WidgetSettingsUiAction.OnShowNotificationWidgetToggle(enabled))
                        // Enabling the notification widget needs notification + exact-alarm permissions.
                        if (enabled) requestWidgetPermissions(SchedulingPermissionSteps.widget)
                    },
                )
            }
        }

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
                    title = stringResource(R.string.use_adaptive_theme),
                    subtitle = null,
                    checked = uiState.settings.adaptiveWidgets,
                    onCheckedChange = { onAction(WidgetSettingsUiAction.OnAdaptiveThemeToggle(it)) },
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
private fun WidgetSettingsScreenPreview() {
    AlAzanThemePreview {
        WidgetSettingsScreen(uiState = WidgetSettingsUiState(), onAction = {})
    }
}
