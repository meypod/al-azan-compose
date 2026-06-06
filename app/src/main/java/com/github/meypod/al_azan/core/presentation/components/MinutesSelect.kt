package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.github.meypod.al_azan.R

/**
 * A minutes picker: a [BottomSelect] over preset minute values plus a "Custom" entry that opens a
 * numeric dialog for an arbitrary value. The selected value is rendered as a localized
 * "N minutes" label even when it is custom (not in [options]).
 */
@Composable
fun MinutesSelect(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
) {
    val resources = LocalResources.current
    val isCustom = selected !in options
    val customLabel = if (isCustom) {
        resources.getQuantityString(R.plurals.time_unit_minutes, selected, selected)
    } else {
        null
    }
    BottomSelect(
        modifier = modifier,
        options = options,
        optionKey = { it.toString() },
        optionLabel = { resources.getQuantityString(R.plurals.time_unit_minutes, it, it) },
        selectedKey = selected.toString(),
        selectedLabelOverride = customLabel,
        onSelect = onSelect,
        label = label,
        supportingText = supportingText,
        headerContent = { onPick ->
            CustomMinutesHeader(
                currentValue = if (isCustom) selected else null,
                onPick = onPick,
            )
        },
    )
}

@Composable
private fun CustomMinutesHeader(
    currentValue: Int?,
    onPick: (Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val isCustom = currentValue != null
    val resources = LocalResources.current
    val title = if (isCustom) {
        resources.getQuantityString(R.plurals.time_unit_minutes, currentValue, currentValue)
    } else {
        stringResource(R.string.custom_minutes)
    }
    DropdownMenuItem(
        text = {
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                if (isCustom) {
                    Text(
                        stringResource(R.string.custom_minutes_tap_to_edit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        onClick = { showDialog = true },
        leadingIcon = if (!isCustom) {
            { Icon(painterResource(R.drawable.add), contentDescription = null) }
        } else {
            null
        },
        trailingIcon = if (isCustom) {
            {
                Icon(
                    painterResource(R.drawable.baseline_check_24),
                    contentDescription = stringResource(R.string.selected),
                )
            }
        } else {
            null
        },
    )
    HorizontalDivider()
    if (showDialog) {
        CustomMinutesDialog(
            initial = currentValue?.toString().orEmpty(),
            onDismiss = { showDialog = false },
            onConfirm = { value ->
                showDialog = false
                onPick(value)
            },
        )
    }
}

@Composable
private fun CustomMinutesDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val canSubmit = value.toIntOrNull()?.let { it > 0 } == true
    val submit = { if (canSubmit) onConfirm(value.toInt()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_minutes_dialog_title)) },
        text = {
            CompactOutlinedTextField(
                value = value,
                onValueChange = { v -> value = v.filter(Char::isDigit) },
                placeholder = stringResource(R.string.custom_minutes),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = canSubmit) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
