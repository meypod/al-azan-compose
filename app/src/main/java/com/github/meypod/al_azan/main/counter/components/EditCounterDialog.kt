package com.github.meypod.al_azan.main.counter.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.counter.COUNTER_ID_FAJR
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.DangerDialog
import com.github.meypod.al_azan.core.presentation.components.IntInputField
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.main.counter.CounterUiAction
import com.github.meypod.al_azan.main.counter.EditCounterDraft
import com.github.meypod.al_azan.main.counter.counterDisplayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCounterDialog(
    draft: EditCounterDraft,
    onAction: (CounterUiAction) -> Unit,
) {
    val labelText = if (draft.isDefault) {
        counterDisplayLabel(Counter(id = draft.id, count = draft.count))
    } else {
        draft.label
    }
    AlertDialog(
        onDismissRequest = { onAction(CounterUiAction.OnEditDialogDismiss) },
        title = { Text(stringResource(R.string.counter_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { onAction(CounterUiAction.OnEditLabelChange(it)) },
                    label = { Text(stringResource(R.string.counter_label)) },
                    enabled = !draft.isDefault,
                    readOnly = draft.isDefault,
                    isError = draft.labelError,
                    supportingText = if (draft.labelError) {
                        { Text(stringResource(R.string.counter_label_required)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                IntInputField(
                    value = draft.count,
                    onValueChange = { onAction(CounterUiAction.OnEditCountChange(it)) },
                    allowNegative = false,
                    label = { Text(stringResource(R.string.counter_count)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            PrimaryButton(onClick = { onAction(CounterUiAction.OnEditDialogConfirm) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            if (!draft.isDefault) {
                IconButton(onClick = { onAction(CounterUiAction.OnEditDeleteRequest) }) {
                    Icon(
                        painterResource(R.drawable.delete),
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            TextButton(onClick = { onAction(CounterUiAction.OnEditDialogDismiss) }) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.error)
            }
        },
    )
    if (draft.deleteConfirm) {
        DangerDialog(
            title = stringResource(R.string.counter_delete_confirm_title),
            text = stringResource(R.string.counter_delete_confirm_body, labelText),
            confirmLabel = stringResource(R.string.delete),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = { onAction(CounterUiAction.OnEditDialogDelete) },
            onDismissRequest = { onAction(CounterUiAction.OnEditDeleteCancel) },
        )
    }
}

@Preview
@Composable
private fun EditCounterDialogCustomPreview() {
    AlAzanThemePreview {
        Scaffold { _ ->
            EditCounterDialog(
                draft = EditCounterDraft(
                    id = "abc",
                    label = "Subhanallah",
                    count = 33,
                    isDefault = false,
                ),
                onAction = {},
            )
        }
    }
}

@Preview
@Composable
private fun EditCounterDialogDeleteConfirmPreview() {
    AlAzanThemePreview {
        Scaffold { _ ->
            EditCounterDialog(
                draft = EditCounterDraft(
                    id = "abc",
                    label = "Subhanallah",
                    count = 33,
                    isDefault = false,
                    deleteConfirm = true,
                ),
                onAction = {},
            )
        }
    }
}

@Preview
@Composable
private fun EditCounterDialogDefaultPreview() {
    AlAzanThemePreview {
        Scaffold { _ ->
            EditCounterDialog(
                draft = EditCounterDraft(
                    id = COUNTER_ID_FAJR,
                    label = "",
                    count = 5,
                    isDefault = true,
                ),
                onAction = {},
            )
        }
    }
}
