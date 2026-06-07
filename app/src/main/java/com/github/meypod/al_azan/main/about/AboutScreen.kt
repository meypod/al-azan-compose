package com.github.meypod.al_azan.main.about

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.BuildConfig
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.components.SettingHeader
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import com.github.meypod.al_azan.core.presentation.util.annotatedStringResource

private const val DEV_UNLOCK_TAPS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onAction: (AboutUiAction) -> Unit = {}) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val tapCount = remember { intArrayOf(0) }

    ScreenScaffold(
        title = stringResource(R.string.calculation_title),
        onBackClick = { (NavigationController.navigateBack()) },
    ) {
        ACard { cardPadding ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                Column(
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) {
                        tapCount[0]++
                        if (tapCount[0] == DEV_UNLOCK_TAPS) {
                            tapCount[0] = 0
                            onAction(AboutUiAction.OnUnlockDeveloper)
                            Toast.makeText(context, R.string.dev_unlocked_toast, Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    SettingHeader(stringResource(R.string.version), BuildConfig.VERSION_NAME)
                }
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

@Preview
@Preview(
    device = Devices.TABLET,
)
@Composable
private fun AboutPreview() {
    AlAzanThemePreview {
        AboutScreen()
    }
}

@Preview(
    heightDp = 200,
)
@Composable
private fun AboutShortHeightPreview() {
    AlAzanThemePreview {
        AboutScreen()
    }
}
