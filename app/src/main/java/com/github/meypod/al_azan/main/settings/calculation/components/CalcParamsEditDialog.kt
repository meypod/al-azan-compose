package com.github.meypod.al_azan.main.settings.calculation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import io.github.meypod.adhan_kotlin.CalculationParameters
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Edit dialog for the calculation method parameters (Fajr/Isha/Maghrib angles and Isha interval).
 * Mirrors the React Native `CalcParamsBox` edit modal: each value gets a +/- stepper around a
 * numeric field, seeded from the currently selected method's parameters.
 */
@Composable
fun CalcParamsEditDialog(
    parameters: CalculationParameters,
    onConfirm: (CalculationParameters) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(parameters) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_calculation_parameters)) },
        text = { CalcParamsEditContent(draft = draft, onDraftChange = { draft = it }) },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }) {
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

@Composable
private fun CalcParamsEditContent(
    draft: CalculationParameters,
    onDraftChange: (CalculationParameters) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_large)),
    ) {
        val degrees = stringResource(R.string.degrees_unit)
        ParamStepper(
            label = stringResource(R.string.fajr_angle),
            value = draft.fajrAngle,
            onValueChange = { onDraftChange(draft.copy(fajrAngle = it)) },
            unit = degrees,
            modifier = Modifier.fillMaxWidth(),
        )
        ParamStepper(
            label = stringResource(R.string.isha_angle),
            value = draft.ishaAngle,
            onValueChange = { onDraftChange(draft.copy(ishaAngle = it)) },
            unit = degrees,
            modifier = Modifier.fillMaxWidth(),
        )
        ParamStepper(
            label = stringResource(R.string.isha_interval),
            value = draft.ishaInterval.toDouble(),
            onValueChange = { onDraftChange(draft.copy(ishaInterval = it.roundToInt())) },
            decimals = false,
            unit = stringResource(R.string.minutes_unit),
            modifier = Modifier.fillMaxWidth(),
        )
        ParamStepper(
            label = stringResource(R.string.maghrib_angle),
            value = draft.maghribAngle,
            onValueChange = { onDraftChange(draft.copy(maghribAngle = it)) },
            unit = degrees,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ParamStepper(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    decimals: Boolean = true,
    unit: String? = null,
) {
    val step = if (decimals) 0.1 else 1.0
    var text by remember { mutableStateOf(formatParam(value, decimals)) }
    val focusManager = LocalFocusManager.current

    fun commit(newValue: Double) {
        text = formatParam(newValue, decimals)
        onValueChange(newValue)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.tiny_padding)),
    ) {
        OutlinedIconButton(onClick = { commit(roundParam(text.toDoubleOrNull() ?: 0.0, step, -1, decimals)) }) {
            Icon(painterResource(R.drawable.minus), contentDescription = stringResource(R.string.decrease))
        }
        CompactOutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val filtered = filterParam(raw, decimals)
                text = filtered
                filtered.toDoubleOrNull()?.let(onValueChange)
            },
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 64.dp)
                .onFocusChanged { focus ->
                    if (!focus.isFocused && text.toDoubleOrNull() == null) commit(0.0)
                },
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimals) KeyboardType.Decimal else KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            trailingIcon = if (unit != null) {
                {
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                null
            },
        )
        OutlinedIconButton(onClick = { commit(roundParam(text.toDoubleOrNull() ?: 0.0, step, +1, decimals)) }) {
            Icon(painterResource(R.drawable.add), contentDescription = stringResource(R.string.increase))
        }
    }
}

private fun filterParam(
    raw: String,
    decimals: Boolean,
): String {
    val sign = if (raw.startsWith('-')) "-" else ""
    val body = raw.drop(sign.length)
    if (!decimals) return sign + body.filter { it.isDigit() }
    var seenDot = false
    val digits = body.filter { ch ->
        when {
            ch.isDigit() -> true

            ch == '.' && !seenDot -> {
                seenDot = true
                true
            }

            else -> false
        }
    }
    return sign + digits
}

private fun roundParam(
    value: Double,
    step: Double,
    direction: Int,
    decimals: Boolean,
): Double {
    val next = value + direction * step
    return if (decimals) (next * 10).roundToLong() / 10.0 else next.roundToLong().toDouble()
}

private fun formatParam(
    value: Double,
    decimals: Boolean,
): String {
    if (!decimals) return value.roundToInt().toString()
    val rounded = (value * 10).roundToLong() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

@Preview
@Composable
private fun CalcParamsEditDialogPreview() {
    AlAzanThemePreview {
        Scaffold { padding ->
            Column(Modifier.padding(padding)) {
                CalcParamsEditDialog(
                    parameters = CalculationParameters(fajrAngle = 18.0, ishaAngle = 17.5, ishaInterval = 0, maghribAngle = 0.0),
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalcParamsEditContentPreview() {
    AlAzanThemePreview {
        var draft by remember {
            mutableStateOf(
                CalculationParameters(fajrAngle = 18.5, ishaAngle = 0.0, ishaInterval = 90, maghribAngle = 4.0),
            )
        }
        CalcParamsEditContent(
            draft = draft,
            onDraftChange = { draft = it },
            modifier = Modifier.padding(dimensionResource(R.dimen.element_padding)),
        )
    }
}
