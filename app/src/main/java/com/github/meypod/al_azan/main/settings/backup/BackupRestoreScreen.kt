package com.github.meypod.al_azan.main.settings.backup

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold

sealed interface BackupRestoreUiAction {
    object OnBackClick : BackupRestoreUiAction
    object OnCreateBackupClick : BackupRestoreUiAction
    object OnRestoreClick : BackupRestoreUiAction
}

@Composable
fun BackupRestoreScreen(
    onAction: (BackupRestoreUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.backup_and_restore_title),
        onBackClick = { onAction(BackupRestoreUiAction.OnBackClick) },
        modifier = modifier,
    ) {
        ACard {
            Column(
                Modifier.padding(it),
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
                    PrimaryButton(onClick = { onAction(BackupRestoreUiAction.OnCreateBackupClick) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(painterResource(R.drawable.upload), contentDescription = null)
                            Text(stringResource(R.string.create_a_backup))
                        }
                    }
                }
            }
        }

        ACard {
            Column(
                Modifier.padding(it),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                Text(
                    stringResource(R.string.restore_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(stringResource(R.string.restore_help), style = MaterialTheme.typography.bodyMedium)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PrimaryButton(onClick = { onAction(BackupRestoreUiAction.OnRestoreClick) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                            Text(stringResource(R.string.restore_settings))
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun BackupRestoreScreenPreview() {
    AlAzanThemePreview {
        BackupRestoreScreen(onAction = {})
    }
}
