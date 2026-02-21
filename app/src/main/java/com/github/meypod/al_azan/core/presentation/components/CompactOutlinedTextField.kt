package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.core.presentation.AlAzanTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CompactOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = OutlinedTextFieldDefaults.colors()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        interactionSource = interactionSource,
        textStyle = textStyle.merge(
            TextStyle(color = MaterialTheme.colorScheme.onSurface),
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            colors = colors,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            placeholder = {
                BasicText(
                    placeholder,
                    modifier = Modifier.fillMaxWidth(),
                    style = textStyle.merge(TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)),
                )
            },
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = enabled,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = colors,
                    shape = MaterialTheme.shapes.small,
                    focusedBorderThickness = OutlinedTextFieldDefaults.FocusedBorderThickness,
                    unfocusedBorderThickness = OutlinedTextFieldDefaults.UnfocusedBorderThickness,
                )
            },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFf)
@Composable
private fun CompactOutlinedTextFieldPreview() {
    AlAzanTheme {
        val (value, setValue) = remember { mutableStateOf("") }
        CompactOutlinedTextField(
            value = value,
            onValueChange = setValue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = "Placeholder",
        )
    }
}
