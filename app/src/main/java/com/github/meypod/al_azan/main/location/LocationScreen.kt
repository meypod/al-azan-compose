package com.github.meypod.al_azan.main.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.favorite_location.TravelingFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.InformationCard
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.components.ReorderableLazyColumn
import com.github.meypod.al_azan.core.presentation.components.TimedDangerDialog
import com.github.meypod.al_azan.core.presentation.util.bottomBorder
import com.github.meypod.al_azan.main.location.components.NewLocationDialog

@Composable
fun LocationScreen(
    uiState: LocationUiState,
    onAction: (LocationUiAction) -> Unit,
    getCountries: suspend () -> List<CountryGeoInfo>,
    getCities: suspend (countryCode: String) -> List<CityGeoInfo>,
    modifier: Modifier = Modifier,
) {
    if (uiState.isNewLocationDialogOpen) {
        NewLocationDialog(
            onAction = onAction,
            getCountries = getCountries,
            getCities = getCities,
        )
    }

    val deletingLocation = uiState.deleteLocationDialogLocation
    if (deletingLocation != null) {
        TimedDangerDialog(
            title = stringResource(R.string.delete_location_confirm_title),
            text = stringResource(
                R.string.delete_location_confirm_body,
                deletingLocation.locationDetail.toDisplayString(),
            ),
            confirmLabel = stringResource(R.string.delete),
            cancelLabel = stringResource(R.string.cancel),
            seconds = 0,
            confirmDisabledUntilFinished = false,
            onConfirm = {
                onAction(LocationUiAction.OnDeleteLocationConfirm(deletingLocation.id))
            },
            onDismissRequest = {
                onAction(LocationUiAction.OnDeleteLocationDismiss)
            },
        )
    }

    Column(
        modifier.padding(dimensionResource(R.dimen.page_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InformationCard(Modifier.fillMaxWidth()) {
            Column {
                Text(stringResource(R.string.location_description_l1))
            }
        }

        Spacer(Modifier.height(dimensionResource(R.dimen.element_padding)))

        ACard(Modifier.fillMaxWidth()) { paddingValues ->
            Column(
                Modifier.padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
            ) {
                Text(
                    text = stringResource(R.string.locations_list_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (uiState.locations.isEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            stringResource(R.string.locations_list_empty_state),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        PrimaryButton(
                            onClick = { onAction(LocationUiAction.OnNewLocationClick) },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = null,
                            )
                            Spacer(Modifier.width(dimensionResource(R.dimen.icon_padding)))
                            Text(stringResource(R.string.add_new_location_button))
                        }
                    }
                } else {
                    LocationList(
                        list = uiState.locations,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationListItem(
    item: FavoriteLocation,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onAction: (LocationUiAction) -> Unit,
    menuExpanded: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    dragging: Boolean = false,
    interactionEnabled: Boolean = true,
) {
    var expanded by remember(item.id) { mutableStateOf(menuExpanded) }

    ListItem(
        modifier = modifier
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                alpha = if (dragging) 0f else 1f
            }
            .bottomBorder(MaterialTheme.colorScheme.outlineVariant, 2.dp),
        headlineContent = {
            Text(
                text = item.locationDetail.toNamed() ?: item.id,
            )
        },
        supportingContent = {
            Text(item.locationDetail.toCoordsString())
        },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.grip),
                contentDescription = stringResource(R.string.drag_handle_description),
                modifier = dragHandleModifier,
            )
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))) {
                if (selected) {
                    Icon(painterResource(R.drawable.baseline_check_24), contentDescription = stringResource(R.string.selected))
                }

                Column(horizontalAlignment = Alignment.End) {
                    IconButton(
                        onClick = {
                            if (interactionEnabled) expanded = true
                        },
                        enabled = interactionEnabled,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.menu_more_h),
                            contentDescription = stringResource(R.string.see_options),
                        )
                    }

                    DropdownMenu(
                        expanded = expanded && interactionEnabled,
                        onDismissRequest = { expanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.set_as_default)) },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_check_24),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                expanded = false
                                onAction(LocationUiAction.OnSetAsDefaultClick(item.id))
                            },
                            enabled = interactionEnabled,
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_location)) },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                expanded = false
                                onAction(LocationUiAction.OnDeleteLocationClick(item.id))
                            },
                            enabled = interactionEnabled,
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors().copy(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    )
}

@Composable
private fun LocationList(
    list: List<FavoriteLocation>,
    selectedId: String? = null,
    onAction: (LocationUiAction) -> Unit,
) {
    val listState = rememberLazyListState()

    ReorderableLazyColumn(
        items = list,
        key = { it.id },
        onMove = { fromIndex, toIndex ->
            onAction(LocationUiAction.OnMoveLocation(fromIndex = fromIndex, toIndex = toIndex))
        },
        listState = listState,
        listModifier = Modifier.shadow(2.dp),
        itemContent = { item, isPlaceholder, itemModifier, dragHandleModifier ->
            LocationListItem(
                item = item,
                selected = item.id == selectedId,
                onAction = onAction,
                dragging = isPlaceholder,
                modifier = itemModifier,
                dragHandleModifier = dragHandleModifier,
            )
        },
        overlayContent = { item, overlayModifier ->
            LocationListItem(
                item = item,
                selected = item.id == selectedId,
                onAction = {},
                dragging = false,
                interactionEnabled = false,
                modifier = overlayModifier,
                dragHandleModifier = Modifier,
            )
        },
    )
}

private val demoLocations = listOf(
    TravelingFavoriteLocation(
        "traveling",
        CalculationLocationDetail(56.1304, 106.3468, null, null),
    ),
    StaticFavoriteLocation(
        "canada",
        CalculationLocationDetail(56.1304, 106.3468, null, null, "Canada"),
    ),
    StaticFavoriteLocation(
        "baqdad",
        CalculationLocationDetail(
            33.312805,
            44.361488,
            CityGeoInfo("Baqdad", "-", 1.0, 1.0, "IQ", "Baqdad"),
            CountryGeoInfo("IQ", "", "Iraq", "Iraq"),
            null,
        ),
    ),
)

@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
)
@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
    device = Devices.TABLET,
)
@Composable
private fun LocationScreenPreview() {
    AlAzanTheme {
        LocationScreen(
            uiState = LocationUiState(),
            onAction = {},
            getCities = { emptyList() },
            getCountries = { emptyList() },
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
)
@Composable
private fun LocationScreenWithLocationsPreview() {
    AlAzanTheme {
        val locations = remember {
            mutableStateListOf<FavoriteLocation>().apply { addAll(demoLocations) }
        }
        LocationScreen(
            uiState = LocationUiState(locations.toList()),
            onAction = { action ->
                when (action) {
                    is LocationUiAction.OnMoveLocation -> {
                        val from = action.fromIndex
                        val to = action.toIndex
                        if (from in locations.indices && to in locations.indices && from != to) {
                            val item = locations.removeAt(from)
                            locations.add(to, item)
                        }
                    }

                    else -> Unit
                }
            },
            getCities = { emptyList() },
            getCountries = { emptyList() },
        )
    }
}
