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
fun DangerDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = text) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(text = confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(text = dismissLabel) }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun DangerDialogPreview() {
    DangerDialog(
        title = stringResource(R.string.attention_title),
        text = stringResource(R.string.skip_dialog_body),
        confirmLabel = "skip",
        dismissLabel = "Cancel",
        onConfirm = {},
        onDismissRequest = {},
    )
}
