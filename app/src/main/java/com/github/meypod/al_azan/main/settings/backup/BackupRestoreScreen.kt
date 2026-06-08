package com.github.meypod.al_azan.main.settings.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.BlockingProgressDialog
import com.github.meypod.al_azan.core.presentation.components.LocalSnackbarController
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val BACKUP_MIME_TYPE = "application/json"
private const val DEFAULT_BACKUP_FILE_NAME = "al_azan_settings.json"

@Composable
fun BackupRestoreScreen(
    uiState: BackupRestoreUiState,
    onAction: (BackupRestoreUiAction) -> Unit,
    modifier: Modifier = Modifier,
    events: Flow<BackupRestoreUiEvent> = emptyFlow(),
) {
    val snackbarController = LocalSnackbarController.current
    val resources = LocalResources.current

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri -> uri?.let { onAction(BackupRestoreUiAction.OnExportFileSelected(it)) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { onAction(BackupRestoreUiAction.OnRestoreFileSelected(it)) } }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is BackupRestoreUiEvent.ShowMessage -> snackbarController.show(resources.getString(event.messageRes))
            }
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.backup_and_restore_title),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
    ) {
        ACard { cardPadding ->
            Column(
                Modifier.padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                Text(
                    stringResource(R.string.create_a_backup),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(stringResource(R.string.backup_help), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "• " + stringResource(R.string.backup_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PrimaryButton(
                        onClick = { exportLauncher.launch(DEFAULT_BACKUP_FILE_NAME) },
                        enabled = !uiState.busy,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(painterResource(R.drawable.upload), contentDescription = null)
                            Text(stringResource(R.string.create_a_backup))
                        }
                    }
                }
            }
        }

        ACard { cardPadding ->
            Column(
                Modifier.padding(cardPadding),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                Text(
                    stringResource(R.string.restore_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(stringResource(R.string.restore_help), style = MaterialTheme.typography.bodyMedium)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PrimaryButton(
                        onClick = { restoreLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "application/octet-stream", "*/*")) },
                        enabled = !uiState.busy,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                            Text(stringResource(R.string.restore_settings))
                        }
                    }
                }
            }
        }
    }

    if (uiState.busy) {
        BlockingProgressDialog(message = stringResource(R.string.please_wait))
    }
}

@Preview
@Composable
private fun BackupRestoreScreenPreview() {
    AlAzanThemePreview {
        BackupRestoreScreen(uiState = BackupRestoreUiState(), onAction = {})
    }
}
