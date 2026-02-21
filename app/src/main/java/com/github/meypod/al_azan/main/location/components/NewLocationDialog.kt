package com.github.meypod.al_azan.main.location.components

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.BottomSelect
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import com.github.meypod.al_azan.core.presentation.components.PrimaryButton
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.fadeScrollEdges
import com.github.meypod.al_azan.main.location.LocationUiAction
import com.github.meypod.al_azan.main.location.NewLocationDialogUiState

@Composable
fun NewLocationDialog(
    countries: List<CountryGeoInfo>,
    cities: List<CityGeoInfo>,
    onAction: (LocationUiAction) -> Unit,
) {
    Dialog(onDismissRequest = { onAction(LocationUiAction.OnNewLocationDismiss) }) {
        NewLocationDialogContent(
            countries = countries,
            cities = cities,
            onAction = onAction,
        )
    }
}

@Composable
private fun NewLocationDialogContent(
    countries: List<CountryGeoInfo>,
    cities: List<CityGeoInfo>,
    onAction: (LocationUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val maxDialogHeight = remember(configuration.screenHeightDp) { configuration.screenHeightDp.dp * 0.9f }

    val scrollState = rememberScrollState()

    val (uiState, setUiState) =
        remember(countries, cities) {
            mutableStateOf(
                NewLocationDialogUiState(
                    countries = countries,
                    cities = cities,
                ),
            )
        }

    val latValue = remember(uiState.latitude) { uiState.latitude.trim().toDoubleOrNull() }
    val lngValue = remember(uiState.longitude) { uiState.longitude.trim().toDoubleOrNull() }

    val confirmEnabled =
        remember(latValue, lngValue) { latValue != null && lngValue != null }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxDialogHeight)
            .padding(horizontal = dimensionResource(R.dimen.page_padding)),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fadeScrollEdges(scrollState, Orientation.Vertical)
                .drawVerticalScrollbar(scrollState)
                .verticalScroll(scrollState)
                .padding(dimensionResource(R.dimen.card_padding)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding_compact)),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = stringResource(R.string.information),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.new_location_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SectionTitle(stringResource(R.string.search_by_city_title))

            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FieldLabel(
                        text = stringResource(R.string.country),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(dimensionResource(R.dimen.element_padding)))
                    FieldLabel(
                        text = stringResource(R.string.city),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    BottomSelect(
                        modifier = Modifier.weight(1f),
                        minWidth = 0.dp,
                        options = countries,
                        optionKey = { it.code },
                        optionLabel = { it.selectedName ?: it.name },
                        optionSearchTag = { (it.selectedName ?: it.name) + "," + it.names },
                        selectedKey = uiState.selectedCountryCode,
                        onSelect = {
                            setUiState(
                                uiState.copy(
                                    selectedCountryCode = it.code,
                                    selectedCityName = null,
                                ),
                            )
                        },
                        searchable = true,
                    )

                    Spacer(Modifier.width(dimensionResource(R.dimen.element_padding)))

                    BottomSelect(
                        modifier = Modifier.weight(1f),
                        minWidth = 0.dp,
                        options = cities,
                        optionKey = { it.name },
                        optionLabel = { it.selectedName ?: it.name },
                        optionSearchTag = { (it.selectedName ?: it.name) + "," + it.names },
                        selectedKey = uiState.selectedCityName,
                        onSelect = { setUiState(uiState.copy(selectedCityName = it.name)) },
                        searchable = true,
                        enabled = uiState.selectedCountryCode != null,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SectionTitle(stringResource(R.string.find_location_title))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                PrimaryButton(onClick = { onAction(LocationUiAction.OnNewLocationFindLocationClick) }) {
                    Icon(
                        painter = painterResource(R.drawable.map_marker),
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(dimensionResource(R.dimen.icon_padding)))
                    Text(
                        text = stringResource(R.string.find_location_button),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SectionTitle(stringResource(R.string.using_coordinates_title))

            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FieldLabel(
                        text = stringResource(R.string.latitude),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.width(dimensionResource(R.dimen.element_padding)))
                    FieldLabel(
                        text = stringResource(R.string.longitude),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val coordinateFieldModifier = Modifier
                        .weight(1f)
                        .height(40.dp)

                    CompactOutlinedTextField(
                        modifier = coordinateFieldModifier,
                        value = uiState.latitude,
                        onValueChange = { setUiState(uiState.copy(latitude = it)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        placeholder = "-",
                    )
                    Text(
                        text = "-",
                        modifier = Modifier.padding(horizontal = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CompactOutlinedTextField(
                        modifier = coordinateFieldModifier,
                        value = uiState.longitude,
                        onValueChange = { setUiState(uiState.copy(longitude = it)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        placeholder = "-",
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                PrimaryButton(onClick = { onAction(LocationUiAction.OnNewLocationPasteCoordinatesClick) }) {
                    Icon(
                        painter = painterResource(R.drawable.clipboard),
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(dimensionResource(R.dimen.icon_padding)))
                    Text(
                        text = stringResource(R.string.paste),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { onAction(LocationUiAction.OnNewLocationDismiss) }) {
                    Text(text = stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(dimensionResource(R.dimen.element_padding)))
                TextButton(
                    onClick = { onAction(LocationUiAction.OnNewLocationConfirm(uiState)) },
                    enabled = confirmEnabled,
                ) {
                    Text(text = stringResource(R.string.confirm))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, heightDp = 400)
@Composable
private fun NewLocationDialogPreview() {
    val countries =
        listOf(
            CountryGeoInfo(code = "US", names = "United States,USA", name = "United States"),
            CountryGeoInfo(code = "CA", names = "Canada", name = "Canada"),
        )
    val cities =
        listOf(
            CityGeoInfo(name = "New York", names = "New York,NYC", lat = 0.0, lng = 0.0, country = "US"),
            CityGeoInfo(name = "Toronto", names = "Toronto", lat = 0.0, lng = 0.0, country = "CA"),
        )

    AlAzanTheme {
        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            NewLocationDialog(
                countries = countries,
                cities = cities,
                onAction = {},
            )
        }
    }
}
