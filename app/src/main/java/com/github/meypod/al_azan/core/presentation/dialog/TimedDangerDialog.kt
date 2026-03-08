package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import kotlinx.coroutines.delay

@Composable
fun TimedDangerDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    seconds: Int = 3,
    confirmDisabledUntilFinished: Boolean = true,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    var remaining by remember { mutableStateOf(seconds) }
    val onConfirmState = rememberUpdatedState(onConfirm)

    LaunchedEffect(seconds) {
        remaining = seconds
        while (remaining > 0) {
            delay(1000L)
            remaining -= 1
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = text) },
        confirmButton = {
            val enabled = if (confirmDisabledUntilFinished) remaining <= 0 else true
            TextButton(
                onClick = { if (enabled) onConfirmState.value() },
                enabled = enabled,
            ) {
                val label = if (remaining > 0) "$confirmLabel ($remaining)" else confirmLabel
                val color = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                Text(text = label, color = color)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(text = dismissLabel) }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun TimedDangerDialogPreview() {
    TimedDangerDialog(
        title = stringResource(R.string.attention_title),
        text = stringResource(R.string.skip_dialog_body),
        confirmLabel = "skip",
        dismissLabel = "Cancel",
        seconds = 3,
        confirmDisabledUntilFinished = true,
        onConfirm = {},
        onDismissRequest = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun TimedDangerDialogEnabledPreview() {
    TimedDangerDialog(
        title = "Warning",
        text = "This action may prevent the app from working properly.",
        confirmLabel = "skip",
        dismissLabel = "Cancel",
        seconds = 0,
        confirmDisabledUntilFinished = true,
        onConfirm = {},
        onDismissRequest = {},
    )
}
