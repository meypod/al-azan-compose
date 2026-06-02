package com.github.meypod.al_azan.main.counter.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Scaffold
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.main.counter.AddCounterDraft
import com.github.meypod.al_azan.main.counter.CounterUiAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCounterDialog(
    draft: AddCounterDraft,
    onAction: (CounterUiAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(CounterUiAction.OnAddDialogDismiss) },
        title = { Text(stringResource(R.string.counter_new_title)) },
        text = {
            OutlinedTextField(
                value = draft.label,
                onValueChange = { onAction(CounterUiAction.OnAddLabelChange(it)) },
                label = { Text(stringResource(R.string.counter_label)) },
                placeholder = { Text(stringResource(R.string.counter_label_placeholder)) },
                isError = draft.labelError,
                supportingText = if (draft.labelError) {
                    { Text(stringResource(R.string.counter_label_required)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            PrimaryButton(onClick = { onAction(CounterUiAction.OnAddDialogConfirm) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(CounterUiAction.OnAddDialogDismiss) }) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.error)
            }
        },
    )
}

@Preview
@Composable
private fun AddCounterDialogPreview() {
    AlAzanThemePreview {
        Scaffold { _ ->
            AddCounterDialog(draft = AddCounterDraft(label = "Subhanallah"), onAction = {})
        }
    }
}

@Preview
@Composable
private fun AddCounterDialogErrorPreview() {
    AlAzanThemePreview {
        Scaffold { _ ->
            AddCounterDialog(draft = AddCounterDraft(label = "", labelError = true), onAction = {})
        }
    }
}
