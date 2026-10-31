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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.github.meypod.al_azan.R

/**
 * "Custom" entry for a [BottomSelect] header: opens a numeric dialog accepting values in [range], and
 * shows [currentValue] (via [valueLabel]) as the selected custom value when there is one.
 */
@Composable
fun CustomNumberHeader(
    currentValue: Int?,
    valueLabel: (Int) -> String,
    dialogTitle: String,
    onPick: (Int) -> Unit,
    range: IntRange = 1..Int.MAX_VALUE,
) {
    var showDialog by remember { mutableStateOf(false) }
    val isCustom = currentValue != null
    val title = if (isCustom) valueLabel(currentValue) else stringResource(R.string.custom_minutes)
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
        CustomNumberDialog(
            title = dialogTitle,
            range = range,
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
private fun CustomNumberDialog(
    title: String,
    range: IntRange,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val canSubmit = value.toIntOrNull()?.let { it in range } == true
    // Say why Confirm is off when the range is bounded; an unbounded one only rules out 0.
    val rangeHint = if (range.last != Int.MAX_VALUE) stringResource(R.string.custom_number_range, range.first, range.last) else null
    val submit = { if (canSubmit) onConfirm(value.toInt()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            CompactOutlinedTextField(
                value = value,
                onValueChange = { v -> value = v.filter(Char::isDigit) },
                placeholder = stringResource(R.string.custom_minutes),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                supportingText = rangeHint?.let { hint ->
                    {
                        Text(
                            hint,
                            color = if (value.isNotEmpty() && !canSubmit) MaterialTheme.colorScheme.error else Color.Unspecified,
                        )
                    }
                },
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
