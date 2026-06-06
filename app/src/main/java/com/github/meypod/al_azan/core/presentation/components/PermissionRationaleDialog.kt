package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R

/**
 * Rationale dialog for a permission ask. Shows confirm / cancel, plus an optional "don't ask again"
 * affordance — pass [onDontAskAgain] = null to hide it (only the home re-check offers it).
 */
@Composable
fun PermissionRationaleDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDontAskAgain: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tiny_padding))) {
                if (onDontAskAgain != null) {
                    TextButton(onClick = onDontAskAgain) {
                        Text(
                            stringResource(R.string.dont_ask_again),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onCancel) {
                    Text(
                        stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun PermissionRationaleDialogPreview() {
    PermissionRationaleDialog(
        title = stringResource(R.string.notification_permission_title),
        text = stringResource(R.string.notification_permission_rationale),
        confirmLabel = stringResource(R.string.okay),
        onConfirm = {},
        onCancel = {},
        onDontAskAgain = {},
    )
}
