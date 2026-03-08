package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R

@Composable
fun CtaDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(text = confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(text = dismissLabel, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun CtaDialogPreview() {
    CtaDialog(
        title = stringResource(R.string.permission_denied),
        text = stringResource(R.string.open_permissions_guidance),
        confirmLabel = "Open Settings",
        dismissLabel = "Cancel",
        onConfirm = {},
        onDismissRequest = {},
    )
}
