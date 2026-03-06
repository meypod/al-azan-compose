package com.github.meypod.al_azan.main.location

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.presentation.AlAzanTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationScreen(
    uiState: LocationUiState,
    onAction: (LocationUiAction) -> Unit,
    getCountries: suspend () -> List<CountryGeoInfo>,
    getCities: suspend (countryCode: String) -> List<CityGeoInfo>,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onAction(LocationUiAction.OnBackClick)
                        },
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.back_button))
                    }
                },
                title = {
                    Text(stringResource(R.string.location_title))
                },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { paddingValues ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            LocationScreenContent(uiState, onAction, getCountries, getCities)
        }
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
