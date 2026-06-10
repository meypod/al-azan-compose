package com.github.meypod.al_azan.main.silence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@Composable
fun SilenceStatusScreen(
    uiState: SilenceStatusUiState,
    onAction: (SilenceStatusUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.dnd_status_title),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        ACard { cardPadding ->
            Column(
                Modifier
                    .padding(cardPadding)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                if (uiState.active) {
                    SettingLabel(stringResource(R.string.dnd_active_title))
                    Text(
                        stringResource(R.string.dnd_active_body, uiState.untilFormatted.orEmpty()),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    PrimaryButton(onClick = { onAction(SilenceStatusUiAction.OnEndSilence) }) {
                        Text(stringResource(R.string.dnd_status_end))
                    }
                } else {
                    SettingLabel(stringResource(R.string.dnd_status_inactive_title))
                    Text(
                        stringResource(R.string.dnd_status_inactive_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    PrimaryButton(onClick = { onAction(SilenceStatusUiAction.OnClose) }) {
                        Text(stringResource(R.string.dnd_status_open_app))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun SilenceStatusActivePreview() {
    AlAzanTheme {
        SilenceStatusScreen(SilenceStatusUiState(active = true, untilFormatted = "14:30"), onAction = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun SilenceStatusInactivePreview() {
    AlAzanTheme {
        SilenceStatusScreen(SilenceStatusUiState(active = false), onAction = {})
    }
}
