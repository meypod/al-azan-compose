package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
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
            CustomNumberHeader(
                currentValue = if (isCustom) selected else null,
                valueLabel = { resources.getQuantityString(R.plurals.time_unit_minutes, it, it) },
                dialogTitle = stringResource(R.string.custom_minutes_dialog_title),
                onPick = onPick,
            )
        },
    )
}
