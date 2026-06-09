package com.github.meypod.al_azan.main.settings.troubleshoot.advanced

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.InformationRow
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import com.github.meypod.al_azan.core.presentation.components.SettingSwitch
import com.github.meypod.al_azan.core.presentation.dialog.PermissionStep
import com.github.meypod.al_azan.core.presentation.dialog.SchedulingPermission
import com.github.meypod.al_azan.core.presentation.dialog.rememberSchedulingPermissionRequest
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@Composable
fun AdvancedTroubleshootScreen(
    uiState: AdvancedTroubleshootUiState,
    onAction: (AdvancedTroubleshootUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The "different alarm type" (ExactAllowWhileIdle) gets no background-activity-launch exemption, so the
    // full-screen alarm can only appear via the notification's full-screen-intent — which the OS suppresses
    // when notifications are denied, leaving the adhan unstoppable. Require notifications when enabling it,
    // and snap the toggle back off if they aren't granted.
    val requestNotifications = rememberSchedulingPermissionRequest(
        isDontAskAgain = { false },
        onDontAskAgain = {},
        onComplete = { results ->
            if (!results.requiredAllGranted()) onAction(AdvancedTroubleshootUiAction.OnAdaptiveChargingToggle(false))
        },
    )
    ScreenScaffold(
        title = stringResource(R.string.advanced_title),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        ACard { cardPadding ->
            Column(
                Modifier.padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                SettingHeader(stringResource(R.string.adaptive_charging), stringResource(R.string.adaptive_charging_help))

                InformationRow(
                    Modifier.fillMaxWidth(),
                    iconDescription = null,
                ) {
                    Text(stringResource(R.string.adaptive_charging_caution))
                }
                SettingSwitch(
                    title = stringResource(R.string.different_alarm_enable),
                    subtitle = null,
                    checked = uiState.useDifferentAlarmType,
                    onCheckedChange = { enabled ->
                        onAction(AdvancedTroubleshootUiAction.OnAdaptiveChargingToggle(enabled))
                        if (enabled) {
                            requestNotifications(
                                listOf(
                                    PermissionStep(
                                        SchedulingPermission.Notification,
                                        R.string.different_alarm_notification_rationale,
                                        R.string.adhan_notification_permission_denied_text,
                                    ),
                                ),
                            )
                        }
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun AdvancedTroubleshootScreenPreview() {
    AlAzanThemePreview {
        AdvancedTroubleshootScreen(uiState = AdvancedTroubleshootUiState(), onAction = {})
    }
}
