package com.github.meypod.al_azan.main.settings.troubleshoot

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.InformationCard
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.SettingLabel
import com.github.meypod.al_azan.core.presentation.components.SettingLinkButton
import com.github.meypod.al_azan.core.presentation.util.annotatedStringResource

@SuppressLint("BatteryLife")
@Composable
fun TroubleshootScreen(
    uiState: TroubleshootUiState,
    onAction: (TroubleshootUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val activity = LocalActivity.current
    val ignoreBatteryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { _ ->
            onAction(TroubleshootUiAction.OnLifecycleChanged(context))
        },
    )

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            onAction(TroubleshootUiAction.OnLifecycleChanged(context))
        }
    }

    Column(
        modifier.padding(dimensionResource(R.dimen.page_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
    ) {
        ACard { cardPadding ->
            Column(
                Modifier
                    .padding(cardPadding)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                SettingLabel(stringResource(R.string.battery_problem_title))
                Text(stringResource(R.string.battery_problem_body), style = MaterialTheme.typography.bodyMedium)

                BatterSaverCTA(isAllowed = uiState.appIsAllowedToKeepRunning) {
                    if (uiState.appIsAllowedToKeepRunning) {
                        onAction(TroubleshootUiAction.OnAppIsAllowedToKeepRunningClick(activity))
                    } else {
                        ignoreBatteryLauncher.launch(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                ("package:" + context.packageName).toUri(),
                            ),
                        )
                    }
                }
            }
        }

        SettingLinkButton(stringResource(R.string.advanced)) {
            onAction(TroubleshootUiAction.OnAdvancedSettingsClick)
        }

        InformationCard(Modifier.fillMaxWidth()) {
            Text(annotatedStringResource(R.string.battery_problem_final_hint, "https://dontkillmyapp.com", "dontkillmyapp.com"))
        }
    }
}

@Composable
fun BatterSaverCTA(
    isAllowed: Boolean,
    onButtonClick: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Column {
            if (isAllowed) {
                OutlinedButton(onClick = onButtonClick) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding)),
                    ) {
                        Text(stringResource(R.string.app_is_allowed_to_keep_running))
                        Icon(painterResource(R.drawable.baseline_check_24), contentDescription = null)
                    }
                }
            } else {
                PrimaryButton(onClick = onButtonClick) {
                    Text(stringResource(R.string.allow_app_to_keep_running))
                }
            }
        }
    }
}

@Preview
@Composable
private fun BatterSaverCTAPreview() {
    AlAzanTheme {
        ACard {
            Column(Modifier.padding(it)) {
                BatterSaverCTA(true) {}
                BatterSaverCTA(false) {}
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
private fun TroubleshootPreview() {
    AlAzanTheme {
        TroubleshootScreen(
            uiState = TroubleshootUiState(),
            onAction = {},
        )
    }
}
