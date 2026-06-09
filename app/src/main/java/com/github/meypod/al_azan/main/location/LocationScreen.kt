package com.github.meypod.al_azan.main.location

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.ScreenScaffold
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    uiState: LocationUiState,
    onAction: (LocationUiAction) -> Unit,
    getCountries: suspend () -> List<CountryGeoInfo>,
    getCities: suspend (countryCode: String) -> List<CityGeoInfo>,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = stringResource(R.string.location_title),
        onBackClick = { NavigationController.navigateBack() },
        modifier = modifier,
        scrollable = false,
        floatingActionButton = {
            if (uiState.locations.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        onAction(LocationUiAction.OnNewLocationClick)
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(
                        painterResource(R.drawable.add),
                        contentDescription = stringResource(R.string.add_new_location_button),
                    )
                }
            }
        },
    ) {
        LocationScreenContent(uiState, onAction, getCountries, getCities)
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFF00585A,
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
