package com.github.meypod.al_azan.main.location.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.main.location.EditLocationLabelDraft
import com.github.meypod.al_azan.main.location.LocationUiAction

@Composable
fun EditLocationLabelDialog(
    draft: EditLocationLabelDraft,
    onAction: (LocationUiAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(LocationUiAction.OnEditLabelDismiss) },
        title = { Text(stringResource(R.string.edit_label)) },
        text = {
            OutlinedTextField(
                value = draft.label,
                onValueChange = { onAction(LocationUiAction.OnEditLabelChange(it)) },
                label = { Text(stringResource(R.string.location_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            PrimaryButton(onClick = { onAction(LocationUiAction.OnEditLabelConfirm) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(LocationUiAction.OnEditLabelDismiss) }) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
private fun EditLocationLabelDialogPreview() {
    AlAzanThemePreview {
        Scaffold {
            Column(Modifier.padding(it)) {
                EditLocationLabelDialog(
                    draft = EditLocationLabelDraft(id = "1", label = "Home"),
                    onAction = {},
                )
            }
        }
    }
}
