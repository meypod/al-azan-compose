package com.github.meypod.al_azan.main.settings.widget

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.LocalSnackbarController
import com.github.meypod.al_azan.core.presentation.components.PrayerCheckboxTable
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun CompactAppearanceScreen(
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
        title = stringResource(R.string.widget_section_compact_appearance),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
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

@Preview
@Composable
private fun CompactAppearanceScreenPreview() {
    AlAzanThemePreview {
        CompactAppearanceScreen(uiState = WidgetSettingsUiState(), onAction = {})
    }
}
