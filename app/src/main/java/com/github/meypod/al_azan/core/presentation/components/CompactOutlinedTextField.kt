package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.core.presentation.AlAzanTheme

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CompactOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    placeholder: String = "",
    enabled: Boolean = true,
    readOnly: Boolean = false,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    trailingIcon: (@Composable () -> Unit)? = null,
    fixedLabel: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val fValue = if (value.isEmpty() && fixedLabel) placeholder else value

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        interactionSource = interactionSource,
        textStyle = textStyle.merge(
            color = colors.unfocusedTextColor,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = fValue,
            innerTextField = {
                if (readOnly || !isFocused) {
                    Text(
                        text = fValue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = textStyle.merge(
                            color = if (value.isEmpty() &&
                                fixedLabel
                            ) {
                                colors.unfocusedPlaceholderColor
                            } else {
                                colors.unfocusedTextColor
                            },
                        ),
                    )
                } else {
                    innerTextField()
                }
            },
            enabled = enabled,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            colors = colors,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            label = label,
            placeholder = {
                Text(
                    text = placeholder,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                )
            },
            supportingText = supportingText,
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
            trailingIcon = trailingIcon,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFf)
@Composable
private fun CompactOutlinedTextFieldPreview() {
    AlAzanTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CompactOutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = "Placeholder",
                label = {
                    Text("Label")
                },
            )
            CompactOutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth(),
                placeholder = "Placeholder",
                label = {
                    Text("Label")
                },
                fixedLabel = true,
            )

            CompactOutlinedTextField(
                value = "foo",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth(),
                label = {
                    Text("Label")
                },
            )

            CompactOutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth(),
                label = {
                    Text("Label")
                },
                supportingText = {
                    Text("supporting text")
                },
            )
        }
    }
}
