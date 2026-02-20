package com.github.meypod.al_azan.main.location

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.domain.model.settings.ThemeColor
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ACard
import com.github.meypod.al_azan.core.presentation.components.InformationCard
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.util.bottomBorder

@Composable
fun LocationScreen(
    uiState: LocationUiState,
    onAction: (LocationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(dimensionResource(R.dimen.page_padding)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InformationCard(Modifier.fillMaxWidth()) {
            Column {
                Text(stringResource(R.string.location_description_l1))
                Text(
                    stringResource(
                        R.string.location_description_l2,
                        stringResource(R.string.add_new_location_button),
                    ),
                )
                Text(stringResource(R.string.location_description_l3))
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
                    LocationList(uiState.locations)
                }
            }
        }
    }
}

@Composable
private fun LocationListItem(
    item: FavoriteLocation,
    selected: Boolean = false,
) {
    ListItem(
        modifier = Modifier.bottomBorder(MaterialTheme.colorScheme.outlineVariant, 2.dp),
        headlineContent = {
            val location = item.locationDetail
            val city = location.city?.selectedName ?: location.city?.name
            val country = location.country?.selectedName ?: location.country?.name
            val label = location.label

            Text(
                text = when {
                    !label.isNullOrBlank() -> label
                    !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
                    else -> item.id
                },
            )
        },
        supportingContent = {
            Text(item.locationDetail.toDisplayString())
        },
        leadingContent = {
            Icon(painter = painterResource(R.drawable.grip), contentDescription = stringResource(R.string.drag_handle_description))
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding))) {
                if (selected) {
                    Icon(painterResource(R.drawable.baseline_check_24), contentDescription = stringResource(R.string.selected))
                }
                Icon(painterResource(R.drawable.menu_more_h), contentDescription = stringResource(R.string.see_options))
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
) {
    LazyColumn(Modifier.shadow(2.dp)) {
        items(list, key = { it.id }) {
            LocationListItem(it, it.id == selectedId)
        }
    }
}

private val demoLocations = listOf(
    FavoriteLocation(
        "canada",
        CalculationLocationDetail(56.1304, 106.3468, null, null, "Canada"),
    ),
    FavoriteLocation(
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

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun LocationListItemPreview() {
    AlAzanTheme {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LocationListItem(demoLocations[0])
            LocationListItem(demoLocations[1])
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF00585A)
@Composable
private fun LocationListItemDarkPreview() {
    AlAzanTheme(ThemeColor.Dark) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LocationListItem(demoLocations[0])
            LocationListItem(demoLocations[1])
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LocationListPreview() {
    AlAzanTheme {
        Column(Modifier.padding(15.dp)) {
            LocationList(demoLocations, "canada")
        }
    }
}

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
        LocationScreen(
            uiState = LocationUiState(demoLocations),
            onAction = {},
        )
    }
}
