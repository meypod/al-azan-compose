package com.github.meypod.al_azan.main.settings.widget

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.NotificationWidgetLayout
import com.github.meypod.al_azan.core.domain.model.settings.i18n
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingLinkButton
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermissionSteps
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.navigation.Route

@Composable
fun WidgetSettingsScreen(
    uiState: WidgetSettingsUiState,
    onAction: (WidgetSettingsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    // Snap the toggle back off when a required permission is missing, so the user can retry cleanly.
    val requestWidgetPermissions = rememberSchedulingPermissionRequest(
        // "Don't ask again" is offered/respected only by the home re-check (allowDontAskAgain stays false here).
        isDontAskAgain = { false },
        onDontAskAgain = {},
        onComplete = { results ->
            if (!results.requiredAllGranted()) onAction(WidgetSettingsUiAction.OnShowNotificationWidgetToggle(false))
        },
    )
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

        // Each shared appearance grouping lives on its own screen; the subtitles note which surfaces it affects.
        SectionHeader(title = stringResource(R.string.widget_section_appearance))
        SettingLinkButton(
            title = stringResource(R.string.widget_section_table_appearance),
            subtitle = stringResource(R.string.widget_section_table_appearance_caption),
        ) {
            NavigationController.navigateTo(Route.Main.Settings.WidgetSettings.TableAppearance)
        }
        SettingLinkButton(
            title = stringResource(R.string.widget_section_compact_appearance),
            subtitle = stringResource(R.string.widget_section_compact_appearance_caption),
        ) {
            NavigationController.navigateTo(Route.Main.Settings.WidgetSettings.CompactAppearance)
        }
        SettingLinkButton(
            title = stringResource(R.string.widget_section_custom_appearance),
            subtitle = stringResource(R.string.widget_section_custom_appearance_caption),
        ) {
            NavigationController.navigateTo(Route.Main.Settings.WidgetSettings.CustomBuilder)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() },
    )
}

@Preview
@Composable
private fun WidgetSettingsScreenPreview() {
    AlAzanThemePreview {
        WidgetSettingsScreen(uiState = WidgetSettingsUiState(), onAction = {})
    }
}
