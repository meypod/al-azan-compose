package com.github.meypod.al_azan.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R

@Immutable
data class AudioPickerSection<T>(
    /** Header shown above the section's first item; null renders the items with no header. */
    val title: String?,
    val options: List<T>,
)

/**
 * A dropdown that opens a grouped sound picker bottom sheet, shared by the muezzin and reminder
 * sound selectors. Each item can be previewed (play/stop) and the currently selected sound has a
 * preview button beside the field. Optionally exposes "add from local files" (in the sheet header)
 * and per-item delete. Generic over the host's own entry type [T]; the host supplies key/label
 * accessors and receives the chosen [T] back in callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AudioPickerField(
    sections: List<AudioPickerSection<T>>,
    selectedKey: String?,
    playingId: String?,
    optionKey: (T) -> String,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onPreview: (T) -> Unit,
    onStopPreview: () -> Unit,
    modifier: Modifier = Modifier,
    optionCanDelete: (T) -> Boolean = { false },
    optionPreviewable: (T) -> Boolean = { true },
    onAddLocalFile: ((filepath: String, name: String) -> Unit)? = null,
    onDelete: ((T) -> Unit)? = null,
    searchable: Boolean = true,
) {
    val flat = remember(sections) { sections.flatMap { it.options } }
    // Title rendered above each titled section's leading item (keyed by that item's key).
    val headerForKey = remember(sections, optionKey) {
        buildMap {
            sections.forEach { section ->
                if (section.title != null) section.options.firstOrNull()?.let { put(optionKey(it), section.title) }
            }
        }
    }
    val selected = remember(flat, selectedKey, optionKey) { flat.firstOrNull { optionKey(it) == selectedKey } }
    val addFromFilesLabel = stringResource(R.string.add_from_local_files)

    var picked by remember { mutableStateOf<PickedAudio?>(null) }
    val launchPicker = rememberAudioFilePicker { picked = it }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_compact)),
    ) {
        BottomSelect(
            modifier = Modifier.weight(1f),
            options = flat,
            optionKey = optionKey,
            optionLabel = optionLabel,
            selectedKey = selectedKey,
            searchable = searchable,
            onSelect = onSelect,
            headerContent = onAddLocalFile?.let {
                {
                    DropdownMenuItem(
                        text = { Text(addFromFilesLabel) },
                        leadingIcon = { Icon(painterResource(R.drawable.add), contentDescription = null) },
                        onClick = launchPicker,
                    )
                    HorizontalDivider()
                }
            },
            itemContent = { entry, isSelected, _, onSelectAndDismiss ->
                val option = entry.value.first
                Column {
                    headerForKey[entry.key]?.let { AudioSectionHeader(it) }
                    AudioOptionRow(
                        label = entry.value.second,
                        selected = isSelected,
                        playing = playingId == entry.key,
                        previewable = optionPreviewable(option),
                        canDelete = onDelete != null && optionCanDelete(option),
                        onPlayToggle = { if (playingId == entry.key) onStopPreview() else onPreview(option) },
                        onClick = { onSelectAndDismiss(option) },
                        onDelete = { onDelete?.invoke(option) },
                    )
                }
            },
        )
        if (selected != null && optionPreviewable(selected)) {
            PreviewIconButton(
                playing = playingId == selectedKey,
                onToggle = { if (playingId == selectedKey) onStopPreview() else onPreview(selected) },
            )
        }
    }

    picked?.let { p ->
        NewAudioDialog(
            initialName = p.suggestedName,
            onConfirm = { name ->
                onAddLocalFile?.invoke(p.filepath, name)
                picked = null
            },
            onDismiss = { picked = null },
        )
    }
}

@Composable
fun PreviewIconButton(
    playing: Boolean,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle) {
        Icon(
            painterResource(if (playing) R.drawable.outline_stop_24 else R.drawable.outline_play_arrow_24),
            contentDescription = stringResource(if (playing) R.string.stop else R.string.play),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun AudioSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            horizontal = dimensionResource(R.dimen.page_padding),
            vertical = dimensionResource(R.dimen.element_padding_compact),
        ),
    )
}

@Composable
private fun AudioOptionRow(
    label: String,
    selected: Boolean,
    playing: Boolean,
    previewable: Boolean,
    canDelete: Boolean,
    onPlayToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        onClick = onClick,
        leadingIcon = if (previewable) {
            { PreviewIconButton(playing = playing, onToggle = onPlayToggle) }
        } else {
            null
        },
        trailingIcon = if (selected || canDelete) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected) {
                        Icon(
                            painterResource(R.drawable.baseline_check_24),
                            contentDescription = stringResource(R.string.selected),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (canDelete) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                painterResource(R.drawable.delete),
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        } else {
            null
        },
    )
}
