package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BottomSelect(
    options: Iterable<T>,
    selectedKey: String?,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    optionKey: ((T) -> String) = { it.hashCode().toString() },
    optionLabel: ((T) -> String) = { it.toString() },
    onSelect: (T) -> Unit = {},
    searchable: Boolean = false,
    enabled: Boolean = true,
    onTriggerClick: () -> Boolean? = { null },
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    itemContent: @Composable (Map.Entry<String, Pair<T, String>>, () -> Unit) -> Unit = { option, onDismiss ->
        DropdownMenuItem(
            text = { Text(option.value.second) },
            onClick = {
                onSelect(option.value.first)
                onDismiss()
            },
        )
    },
    extraContent: @Composable ColumnScope.(entries: Map<String, Pair<T, String>>) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val onDismiss =
        remember {
            {
                expanded = false
                searchText = ""
            }
        }

    val keyLabelOptionEntries =
        remember(
            options,
            optionKey,
            optionLabel,
        ) { options.associate { optionKey(it) to Pair(it, optionLabel(it)) } }

    val filteredOptions =
        remember(searchable, searchText, keyLabelOptionEntries) {
            if (searchable && searchText.isNotEmpty()) {
                keyLabelOptionEntries.filter {
                    it.value.second.contains(
                        searchText,
                        ignoreCase = true,
                    )
                }
            } else {
                keyLabelOptionEntries
            }
        }

    val selectedLabel =
        remember(optionKey, selectedKey, keyLabelOptionEntries) {
            keyLabelOptionEntries
                .filter { it.key == selectedKey }
                .let { if (it.isNotEmpty()) it.values.first().second else "" }
        }

    val updatedOnTriggerClick by rememberUpdatedState(onTriggerClick)

    Column(modifier = modifier) {
        OutlinedButton(
            modifier = Modifier.widthIn(min = 280.dp),
            onClick = {
                val newExpand = updatedOnTriggerClick() ?: true
                expanded = newExpand
            },
            enabled = enabled,
            shape = MaterialTheme.shapes.small,
            contentPadding = PaddingValues(
                start = dimensionResource(R.dimen.element_padding),
                end = 5.dp,
            ),
            colors = colors,
        ) {
            Row(
                modifier = Modifier.widthIn(min = 280.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectedLabel.isEmpty()) {
                    placeholder?.invoke()
                } else {
                    Text(selectedLabel)
                }

                Spacer(
                    Modifier.width(dimensionResource(R.dimen.element_padding)),
                )

                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_drop_down_24),
                    contentDescription = null,
                )
            }
        }
        extraContent(keyLabelOptionEntries)
    }

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = {
                expanded = false
                searchText = ""
            },
        ) {
            if (searchable) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = {
                        Text(stringResource(R.string.search_placeholder))
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.element_padding)),
                )
            }

            LazyColumn {
                items(filteredOptions.entries.toList(), key = { it.key }) { item ->
                    itemContent(item, onDismiss)
                }
            }

            if (searchable && filteredOptions.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.page_padding))
                        .fillMaxWidth(),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.no_items_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun BottomSelectPreview() {
    AlAzanTheme {
        BottomSelect(
            modifier = Modifier.width(190.dp),
            options = listOf("English", "Persian"),
            optionKey = { it },
            optionLabel = { it },
            selectedKey = "English",
            searchable = true,
        )
    }
}
