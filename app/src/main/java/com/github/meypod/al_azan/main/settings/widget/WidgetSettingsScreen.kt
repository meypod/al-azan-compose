package com.github.meypod.al_azan.main.settings.widget

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.NotificationWidgetLayout
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
        // General — settings that apply to every widget surface. The adaptive (Material You) theme uses
        // dynamic colors that only exist on Android 12+ (the adaptive layouts are defined under -v31), so
        // the section is hidden where it can't take effect.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SectionHeader(title = stringResource(R.string.widget_section_general))
            ACard { cardPadding ->
                Column(Modifier.padding(cardPadding)) {
                    SettingSwitch(
                        title = stringResource(R.string.use_adaptive_theme),
                        subtitle = null,
                        checked = uiState.settings.adaptiveWidgets,
                        onCheckedChange = { onAction(WidgetSettingsUiAction.OnAdaptiveThemeToggle(it)) },
                    )
                }
            }
        }

        // Notification widget — its own toggle and the layout it renders in.
        SectionHeader(title = stringResource(R.string.widget_section_notification))
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
        // The layout choice only matters once the notification widget is on.
        AnimatedVisibility(visible = uiState.settings.showWidget) {
            ACard { cardPadding ->
                BottomSelect(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(cardPadding),
                    options = NotificationWidgetLayout.entries,
                    optionKey = { it.name },
                    optionLabel = { it.i18n(resources) },
                    selectedKey = uiState.settings.notificationWidgetLayout.name,
                    onSelect = { onAction(WidgetSettingsUiAction.OnNotificationLayoutChange(it)) },
                    label = { Text(stringResource(R.string.notification_widget_layout_label)) },
                    placeholder = stringResource(R.string.notification_widget_layout_label),
                    supportingText = { Text(stringResource(R.string.notification_widget_layout_help)) },
                )
            }
        }

        // Table appearance — shared by the home table widget and the notification in table layout.
        SectionHeader(
            title = stringResource(R.string.widget_section_table_appearance),
            caption = stringResource(R.string.widget_section_table_appearance_caption),
        )
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

        // Compact appearance — shared by the home 1x1 widget and the notification in compact layout.
        SectionHeader(
            title = stringResource(R.string.widget_section_compact_appearance),
            caption = stringResource(R.string.widget_section_compact_appearance_caption),
        )
        ACard { cardPadding ->
            PrayerCheckboxTable(
                title = stringResource(R.string.next_prayer_widget_countdown_prayers),
                helpText = stringResource(R.string.next_prayer_widget_countdown_prayers_help),
                leftColumn = stringResource(R.string.time_column),
                rightColumn = stringResource(R.string.show_column),
                isChecked = { it in uiState.settings.countdownWidgetPrayers },
                onToggle = { prayer, enabled ->
                    onAction(WidgetSettingsUiAction.OnCountdownPrayerToggle(prayer, enabled))
                },
                modifier = Modifier.padding(cardPadding),
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
