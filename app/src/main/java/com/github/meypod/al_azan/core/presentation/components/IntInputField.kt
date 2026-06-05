package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Integer text field that keeps a local string buffer so partial input (empty or a lone "-")
 * survives editing. On focus loss an unparseable buffer falls back to 0.
 */
@Composable
fun IntInputField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    allowNegative: Boolean = true,
    label: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions? = null,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    val focusManager = LocalFocusManager.current
    CompactOutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val sign = if (allowNegative && raw.startsWith('-')) "-" else ""
            val filtered = sign + raw.filter { it.isDigit() }
            text = filtered
            filtered.toIntOrNull()?.let(onValueChange)
        },
        modifier = modifier.onFocusChanged { focus ->
            if (!focus.isFocused && text.toIntOrNull() == null) {
                text = "0"
                onValueChange(0)
            }
        },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        keyboardActions = keyboardActions ?: KeyboardActions(onDone = { focusManager.clearFocus() }),
        textStyle = textStyle,
    )
}
