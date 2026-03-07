package com.github.meypod.al_azan.main.aboutus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.BuildConfig
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import com.github.meypod.al_azan.core.presentation.util.annotatedStringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(
    onAction: (AboutUsUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onAction(AboutUsUiAction.OnBackClick)
                        },
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.back_button))
                    }
                },
                title = {
                    Text(stringResource(R.string.about_us))
                },
            )
        },
    ) { paddingValues ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(dimensionResource(R.dimen.element_padding)),
        ) {
            ACard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(it),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                ) {
                    SettingHeader(stringResource(R.string.version), BuildConfig.VERSION_NAME)
                    SettingHeader(
                        stringResource(R.string.home),
                        annotatedStringResource(R.string.home_url, "https://github.com/meypod/al-azan"),
                    )
                    SettingHeader(stringResource(R.string.license), "AGPL-3.0")
                    HorizontalDivider()
                    Text(
                        stringResource(R.string.about_credits),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
    device = Devices.TABLET,
)
@Composable
private fun AboutUsPreview() {
    AlAzanTheme {
        AboutUsScreen(
            onAction = {},
        )
    }
}
