package com.github.meypod.al_azan.main.location.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
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
import com.github.meypod.al_azan.core.presentation.dialog.rememberLocationAccessHelperDialogs
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.fadeScrollEdges
import com.github.meypod.al_azan.core.presentation.util.filterToDigitsAndDot
import com.github.meypod.al_azan.core.presentation.util.parseClipboardToCoords
import com.github.meypod.al_azan.core.presentation.util.returnMatched
import com.github.meypod.al_azan.core.presentation.util.toEnglishDigits
import com.github.meypod.al_azan.main.location.LocationUiAction
import com.github.meypod.al_azan.main.location.components.NewLocationDialogUiState
import kotlinx.coroutines.launch

@Composable
fun NewLocationDialog(
    onAction: (LocationUiAction) -> Unit,
    getCountries: suspend () -> List<CountryGeoInfo>,
    getCities: suspend (countryCode: String) -> List<CityGeoInfo>,
) {
    Dialog(onDismissRequest = { onAction(LocationUiAction.OnNewLocationDismiss) }) {
        NewLocationDialogContent(
            onAction = onAction,
            getCountries = getCountries,
            getCities = getCities,
        )
    }
}

@Composable
private fun NewLocationDialogContent(
    onAction: (LocationUiAction) -> Unit,
    getCountries: suspend () -> List<CountryGeoInfo>,
    getCities: suspend (countryCode: String) -> List<CityGeoInfo>,
    modifier: Modifier = Modifier,
    uiState: NewLocationDialogUiState = NewLocationDialogUiState(),
) {
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val windowInfo = LocalWindowInfo.current
    val maxDialogHeight = remember(windowInfo.containerDpSize.height) { windowInfo.containerDpSize.height * 0.9f }

    val scrollState = rememberScrollState()

    val updatedGetCities by rememberUpdatedState(getCities)
    val updatedGetCountries by rememberUpdatedState(getCountries)
    var countries by remember { mutableStateOf(emptyList<CountryGeoInfo>()) }
    var cities by remember { mutableStateOf(emptyList<CityGeoInfo>()) }

    var uiState by remember { mutableStateOf(uiState) }

    val latValue = remember(uiState.latitude) { uiState.latitude.trim().toEnglishDigits().toDoubleOrNull() }
    val lngValue = remember(uiState.longitude) { uiState.longitude.trim().toEnglishDigits().toDoubleOrNull() }
    val labelValue = remember(uiState.label) { uiState.label.trim() }

    val triggerLocation = rememberLocationAccessHelperDialogs(
        onLocation = {
            uiState = uiState.copy(
                latitude = it.lat.toString().take(8).trimEnd('.'),
                longitude = it.long.toString().take(8).trimEnd('.'),
                fetchingLocation = false,
                selectedCountry = null,
                selectedCity = null,
            )
        },
    )
    val confirmEnabled =
        remember(latValue, lngValue, labelValue) { latValue != null && lngValue != null && labelValue.isNotBlank() }

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
                .padding(dimensionResource(R.dimen.page_padding)),
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

            CompactOutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(R.dimen.tiny_padding))
                    .height(40.dp),
                value = uiState.label,
                onValueChange = { uiState = uiState.copy(label = it) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                label = {
                    FieldLabel(text = stringResource(R.string.location_label) + " *")
                },
            )

            UsingSectionTitle(stringResource(R.string.search_by_city_title))

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = dimensionResource(R.dimen.tiny_padding)),
                ) {
                    val selectModifier = Modifier.height(40.dp)
                    BottomSelect(
                        modifier = selectModifier.weight(1f),
                        options = countries,
                        optionKey = { it.code },
                        optionLabel = { it.selectedName ?: it.name },
                        optionSearchTag = { it.names },
                        selectedKey = uiState.selectedCountry?.code,
                        onSelect = { chosen ->
                            countries = countries.map { c ->
                                if (c.code == chosen.code) chosen else c
                            }
                            uiState = uiState.copy(
                                selectedCountry = chosen,
                                selectedCity = null,
                            )
                        },
                        onTriggerClick = {
                            if (countries.isEmpty()) {
                                countries = updatedGetCountries()
                            }
                            true
                        },
                        searchable = true,
                        itemContent = returnedMatchItemContent(
                            setSelectedName = { item, selectedName -> item.copy(selectedName = selectedName) },
                        ),
                        label = {
                            FieldLabel(
                                text = stringResource(R.string.country),
                                modifier = Modifier.weight(1f),
                            )
                        },
                    )

                    Spacer(Modifier.width(dimensionResource(R.dimen.element_padding)))

                    BottomSelect(
                        modifier = selectModifier.weight(1f),
                        options = cities,
                        optionKey = { it.name + it.names + it.lat },
                        optionLabel = { it.selectedName ?: it.name },
                        optionSearchTag = { it.names },
                        selectedKey = uiState.selectedCity?.let { it.name + it.names + it.lat },
                        onSelect = { chosen ->
                            cities = cities.map { c ->
                                if (c.isSameCity(chosen)) chosen else c
                            }
                            uiState =
                                uiState.copy(selectedCity = chosen, latitude = chosen.lat.toString(), longitude = chosen.long.toString())
                        },
                        onTriggerClick = {
                            val countryCode = uiState.selectedCountry?.code
                            if (!countryCode.isNullOrBlank()) {
                                cities = updatedGetCities(countryCode)
                            }
                            true
                        },
                        searchable = true,
                        enabled = uiState.selectedCountry != null,
                        itemContent = returnedMatchItemContent(
                            setSelectedName = { item, selectedName -> item.copy(selectedName = selectedName) },
                        ),
                        label = {
                            FieldLabel(
                                text = stringResource(R.string.city),
                                modifier = Modifier.weight(1f),
                            )
                        },
                    )
                }
            }

            UsingSectionTitle(stringResource(R.string.find_location_title))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                PrimaryButton(
                    onClick = {
                        if (uiState.fetchingLocation) return@PrimaryButton
                        uiState = uiState.copy(fetchingLocation = true)
                        triggerLocation()
                    },
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.map_marker),
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.find_location_button),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (uiState.fetchingLocation) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            UsingSectionTitle(stringResource(R.string.using_coordinates_title))

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(R.dimen.tiny_padding)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val coordinateFieldModifier = Modifier
                        .weight(1f)
                        .height(40.dp)

                    CompactOutlinedTextField(
                        modifier = coordinateFieldModifier,
                        value = uiState.latitude,
                        onValueChange = {
                            uiState = uiState.copy(
                                latitude = it.filterToDigitsAndDot(allowLeadingMinus = true),
                                selectedCity = null,
                                selectedCountry = null,
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        placeholder = "-",
                        label = {
                            FieldLabel(
                                text = stringResource(R.string.latitude),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                            )
                        },
                    )
                    Text(
                        text = "-",
                        modifier = Modifier.padding(horizontal = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CompactOutlinedTextField(
                        modifier = coordinateFieldModifier,
                        value = uiState.longitude,
                        onValueChange = {
                            uiState =
                                uiState.copy(
                                    longitude = it.filterToDigitsAndDot(allowLeadingMinus = true),
                                    selectedCity = null,
                                    selectedCountry = null,
                                )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        placeholder = "-",
                        label = {
                            FieldLabel(
                                text = stringResource(R.string.longitude),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                            )
                        },
                    )
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.icon_padding), Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val showClear = uiState.latitude.isNotBlank() || uiState.longitude.isNotBlank()

                item {
                    PrimaryButton(
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(durationMillis = 150),
                            placementSpec = tween(durationMillis = 150),
                            fadeOutSpec = tween(durationMillis = 150),
                        ),
                        onClick = {
                            scope.launch {
                                val clipEntry = clipboard.getClipEntry()
                                val clipData = clipEntry?.clipData
                                val text =
                                    clipData
                                        ?.takeIf { it.itemCount > 0 }
                                        ?.getItemAt(0)
                                        ?.coerceToText(context)
                                        ?.toString()

                                val coords = parseClipboardToCoords(text)
                                if (coords != null) {
                                    uiState =
                                        uiState.copy(
                                            latitude = coords.first.toString(),
                                            longitude = coords.second.toString(),
                                            selectedCity = null,
                                        )
                                } else {
                                    Toast
                                        .makeText(context, R.string.clipboard_coordinates_not_found, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        },
                    ) {
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

                item {
                    AnimatedVisibility(
                        modifier = Modifier.animateItem(),
                        visible = showClear,
                        enter =
                            fadeIn(animationSpec = tween(durationMillis = 150)),
                        exit =
                            fadeOut(animationSpec = tween(durationMillis = 150)),
                    ) {
                        IconButton(
                            modifier = Modifier
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    shape = CircleShape,
                                )
                                .size(40.dp),
                            onClick = {
                                uiState = uiState.copy(
                                    latitude = "",
                                    longitude = "",
                                    selectedCity = null,
                                    selectedCountry = null,
                                )
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.baseline_close_24),
                                contentDescription = stringResource(R.string.clear),
                            )
                        }
                    }
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
                    onClick = {
                        onAction(
                            LocationUiAction.OnNewLocationConfirm(
                                uiState.copy(
                                    latitude = uiState.latitude.trim().toEnglishDigits(),
                                    longitude = uiState.longitude.trim().toEnglishDigits(),
                                    label = uiState.label.trim(),
                                ),
                            ),
                        )
                    },
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
private fun UsingSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.element_padding)))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.element_padding)))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
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

private fun CityGeoInfo.isSameCity(other: CityGeoInfo): Boolean =
    name == other.name && lat == other.lat && long == other.long && country == other.country

private fun <T> returnedMatchItemContent(
    setSelectedName: (T, String) -> T,
): @Composable (Map.Entry<String, Triple<T, String, String>>, Boolean, String, (T) -> Unit) -> Unit =
    { option, selected, needle, onSelectAndDismiss ->
        val showReturnedMatch = needle.isNotBlank()
        val match = if (showReturnedMatch) returnMatched(option.value.third, needle) else null
        val label = match?.value ?: option.value.second
        val firstValue = match?.firstValue.orEmpty()
        val suffix =
            if (
                showReturnedMatch &&
                firstValue.isNotBlank() &&
                firstValue != label
            ) {
                " ($firstValue)"
            } else {
                ""
            }

        DropdownMenuItem(
            text = {
                Text(
                    text = label + suffix,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            onClick = {
                val chosen =
                    if (showReturnedMatch) {
                        setSelectedName(option.value.first, match!!.value)
                    } else {
                        option.value.first
                    }
                onSelectAndDismiss(chosen)
            },
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
            CityGeoInfo(name = "New York", names = "New York,NYC", lat = 0.0, long = 0.0, country = "US"),
            CityGeoInfo(name = "Toronto", names = "Toronto", lat = 0.0, long = 0.0, country = "CA"),
        )

    AlAzanTheme {
        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            NewLocationDialog(
                getCountries = { countries },
                getCities = { cities },
                onAction = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NewLocationDialogContentPreview() {
    AlAzanTheme {
        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            NewLocationDialogContent(
                getCountries = { emptyList() },
                getCities = { emptyList() },
                onAction = {},
                uiState = NewLocationDialogUiState(latitude = "1", longitude = "2"),
            )
        }
    }
}
