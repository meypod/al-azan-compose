package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.util.prepareForSearch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BottomSelect(
    options: Iterable<T>,
    selectedKey: String?,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: String = "",
    supportingText: (@Composable () -> Unit)? = null,
    optionKey: ((T) -> String) = { it.hashCode().toString() },
    optionLabel: ((T) -> String) = { it.toString() },
    optionSearchTag: ((T) -> String) = optionLabel,
    onSelect: (T) -> Unit = {},
    searchable: Boolean = false,
    enabled: Boolean = true,
    onTriggerClick: suspend () -> Boolean? = { null },
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    itemContent: @Composable (
        Map.Entry<String, Triple<T, String, String>>,
        Boolean,
        String,
        (T) -> Unit,
    ) -> Unit = { option, selected, _, onSelectAndDismiss ->
        DefaultBottomSelectItem(option.value.second, selected) {
            onSelectAndDismiss(option.value.first)
        }
    },
) {
    BottomSelectImpl(
        options = options,
        selectedKey = selectedKey,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        optionKey = optionKey,
        optionLabel = optionLabel,
        optionSearchTag = optionSearchTag,
        onSelect = onSelect,
        searchable = searchable,
        enabled = enabled,
        onTriggerClick = onTriggerClick,
        colors = colors,
        itemContent = itemContent,
        initialBusy = false,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> BottomSelectImpl(
    options: Iterable<T>,
    selectedKey: String?,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: String = "",
    supportingText: (@Composable () -> Unit)? = null,
    optionKey: ((T) -> String) = { it.hashCode().toString() },
    optionLabel: ((T) -> String) = { it.toString() },
    optionSearchTag: ((T) -> String) = optionLabel,
    onSelect: (T) -> Unit = {},
    searchable: Boolean = false,
    enabled: Boolean = true,
    onTriggerClick: suspend () -> Boolean? = { null },
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    itemContent: @Composable (
        Map.Entry<String, Triple<T, String, String>>,
        Boolean,
        String,
        (T) -> Unit,
    ) -> Unit = { option, selected, _, onSelectAndDismiss ->
        DefaultBottomSelectItem(option.value.second, selected) {
            onSelectAndDismiss(option.value.first)
        }
    },
    initialBusy: Boolean,
) {
    val focusManager = LocalFocusManager.current

    var expanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var searchText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val onDismiss =
        remember {
            {
                focusManager.clearFocus()
                scope
                    .launch { sheetState.hide() }
                    .invokeOnCompletion {
                        expanded = false
                        searchText = ""
                    }
                Unit
            }
        }

    val keyLabelOptionEntries =
        remember(
            options,
            optionKey,
            optionLabel,
            optionSearchTag,
        ) { options.associate { optionKey(it) to Triple(it, optionLabel(it), optionSearchTag(it)) } }

    val preparedSearchTags =
        remember(keyLabelOptionEntries) {
            keyLabelOptionEntries.mapValues { (_, triple) ->
                prepareForSearch(triple.third)
            }
        }

    val filteredOptions =
        remember(searchable, searchText, keyLabelOptionEntries, preparedSearchTags) {
            if (searchable && searchText.isNotEmpty()) {
                val preparedNeedle = prepareForSearch(searchText)
                keyLabelOptionEntries.filter { entry ->
                    preparedSearchTags[entry.key]?.contains(preparedNeedle) == true
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
    val busyGate = remember { AtomicBoolean(false) }
    var busy by remember { mutableStateOf(initialBusy) }

    val triggerModifier =
        modifier
            .onFocusChanged { state ->
                scope.launch {
                    if (state.hasFocus && busyGate.compareAndSet(false, true)) {
                        busy = true
                        try {
                            val newExpand = updatedOnTriggerClick() ?: true
                            expanded = newExpand
                        } finally {
                            busy = false
                            busyGate.set(false)
                        }
                    }
                }
            }

    val onSelectAndDismiss: (T) -> Unit = remember(onSelect, onDismiss) {
        { selectedItem ->
            onSelect(selectedItem)
            onDismiss()
        }
    }

    val onSelectAndDismissUpdated by rememberUpdatedState(onSelectAndDismiss)

    CompactOutlinedTextField(
        value = selectedLabel,
        onValueChange = {},
        modifier = triggerModifier,
        enabled = enabled,
        readOnly = true,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        trailingIcon = {
            if (busy) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.baseline_arrow_drop_down_24),
                    contentDescription = null,
                )
            }
        },
        colors = colors,
        fixedLabel = true,
    )

    if (expanded) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
        ) {
            if (searchable) {
                TextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                    },
                    placeholder = {
                        Text(stringResource(R.string.search_placeholder))
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(dimensionResource(R.dimen.element_padding)),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                )
            }

            LazyColumn {
                items(filteredOptions.entries.toList(), key = { it.key }) { item ->
                    itemContent(item, item.key == selectedKey, searchText, onSelectAndDismissUpdated)
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

@Composable
private fun DefaultBottomSelectItem(
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit = {},
) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        onClick = onClick,
        trailingIcon = {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.baseline_check_24),
                    contentDescription = stringResource(R.string.selected),
                )
            }
        },
    )
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

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun BottomSelectLabeledPreview() {
    AlAzanTheme {
        BottomSelect(
            modifier = Modifier.width(190.dp),
            options = listOf("English", "Persian"),
            optionKey = { it },
            optionLabel = { it },
            selectedKey = null,
            searchable = true,
            label = {
                Text("Language")
            },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun BottomSelectBusyPreview() {
    AlAzanTheme {
        BottomSelectImpl(
            modifier = Modifier.width(190.dp),
            options = listOf("English", "Persian"),
            optionKey = { it },
            optionLabel = { it },
            selectedKey = "English",
            searchable = true,
            initialBusy = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun DefaultBottomSelectItemPreview() {
    AlAzanTheme {
        Column {
            DefaultBottomSelectItem(
                "English",
                true,
            )
            DefaultBottomSelectItem(
                "Persian",
                false,
            )
        }
    }
}
