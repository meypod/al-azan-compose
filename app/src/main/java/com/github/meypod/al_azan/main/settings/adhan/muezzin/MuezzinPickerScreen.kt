package com.github.meypod.al_azan.main.settings.adhan.muezzin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.AudioEntry
import com.github.meypod.al_azan.core.presentation.AlAzanThemePreview
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold

@Composable
fun MuezzinPickerScreen(
    uiState: MuezzinPickerUiState,
    onAction: (MuezzinPickerUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.muezzin),
        onBackClick = { onAction(MuezzinPickerUiAction.OnBackClick) },
        modifier = modifier,
    ) {
        ACard {
            Column(Modifier.padding(it)) {
                uiState.defaultOptions.forEach { entry ->
                    key(entry.id) {
                        EntryRow(
                            label = entry.getLabel(),
                            selected = entry.id == uiState.selectedId,
                            onClick = { onAction(MuezzinPickerUiAction.OnSelect(entry)) },
                        )
                    }
                }
            }
        }

        ACard {
            Column(
                Modifier.padding(it),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                Text(
                    stringResource(R.string.my_muezzins),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                if (uiState.userEntries.isEmpty()) {
                    Text(
                        stringResource(R.string.my_muezzins_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                } else {
                    uiState.userEntries.forEach { entry ->
                        key(entry.id) {
                            EntryRow(
                                label = entry.label,
                                selected = entry.id == uiState.selectedId,
                                onClick = { onAction(MuezzinPickerUiAction.OnSelect(entry)) },
                            )
                        }
                    }
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PrimaryButton(onClick = { onAction(MuezzinPickerUiAction.OnAddFromLocalFilesClick) }) {
                        Icon(painterResource(R.drawable.add), contentDescription = null)
                        Text(
                            stringResource(R.string.add_from_local_files),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = if (selected) {
            {
                Icon(
                    painterResource(R.drawable.baseline_check_24),
                    contentDescription = stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color.Transparent
            },
        ),
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    )
}

@Preview
@Composable
private fun MuezzinPickerScreenPreview() {
    AlAzanThemePreview {
        MuezzinPickerScreen(
            uiState = MuezzinPickerUiState(
                defaultOptions = listOf(
                    AudioEntry.ResourceAudioEntry(id = "abdul_basit", labelResId = R.string.unknown),
                    AudioEntry.ResourceAudioEntry(id = "mishari", labelResId = R.string.unknown),
                ),
                userEntries = listOf(
                    AudioEntry.ExternalAudioEntry(id = "user_1", label = "My recording"),
                ),
                selectedId = "abdul_basit",
            ),
            onAction = {},
        )
    }
}
