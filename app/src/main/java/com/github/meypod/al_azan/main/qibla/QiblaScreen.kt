package com.github.meypod.al_azan.main.qibla

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.InformationCard
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@Composable
fun QiblaScreen(
    uiState: QiblaUiState,
    onAction: (QiblaUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val localeTag = LocalResources.current.configuration.locales[0].toLanguageTag()
    ScreenScaffold(
        title = stringResource(R.string.qibla),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        InformationCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))) {
                Text(stringResource(R.string.qibla_disclaimer_title), fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.qibla_disclaimer), style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (!uiState.disclaimerAcknowledged) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PrimaryButton(onClick = { onAction(QiblaUiAction.OnUnderstoodClick) }) {
                    Text(stringResource(R.string.qibla_i_understand))
                }
            }
        } else {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.element_padding),
                    alignment = Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_large)),
            ) {
                QiblaTile(
                    icon = R.drawable.globe_location,
                    label = stringResource(R.string.qibla_use_map),
                ) { uriHandler.openUri("$QIBLA_MAP_URL?lang=$localeTag") }
                QiblaTile(
                    icon = R.drawable.compass_outline,
                    label = stringResource(R.string.qibla_use_compass),
                ) { onAction(QiblaUiAction.OnUseCompassClick) }
            }
        }
    }
}

private const val QIBLA_MAP_URL = "https://meypod.github.io/qibla-map/"

@Composable
private fun QiblaTile(
    icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    ACard(onClick = onClick) { cardPadding ->
        Column(
            Modifier
                .padding(cardPadding)
                .size(140.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Icon(painterResource(icon), null, modifier = Modifier.size(64.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview
@Composable
private fun QiblaScreenInitialPreview() {
    AlAzanThemePreview {
        QiblaScreen(uiState = QiblaUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun QiblaScreenAcknowledgedPreview() {
    AlAzanThemePreview {
        QiblaScreen(uiState = QiblaUiState(disclaimerAcknowledged = true), onAction = {})
    }
}
