package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview

/**
 * Prompts for a name after a local audio file is picked. Confirming is blocked until the name is
 * non-blank, mirroring the old app's "Add Custom Audio" dialog.
 */
@Composable
fun NewAudioDialog(
    initialName: String,
    onConfirm: (name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    val valid = name.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_custom_audio)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_compact))) {
                CompactOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.audio_name)) },
                    placeholder = stringResource(R.string.audio_name),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!valid) {
                    Text(
                        stringResource(R.string.audio_name_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onConfirm(name.trim()) }) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview
@Composable
private fun NewAudioDialogPreview() {
    AlAzanThemePreview {
        NewAudioDialog(initialName = "my recording", onConfirm = {}, onDismiss = {})
    }
}
