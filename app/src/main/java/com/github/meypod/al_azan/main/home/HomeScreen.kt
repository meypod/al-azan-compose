package com.github.meypod.al_azan.main.home

import android.icu.text.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.i18n
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.usecase.GetNextShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.utils.formatCountdownToHHmmss
import com.github.meypod.al_azan.core.domain.utils.formatInstant
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.DarkTertiary
import com.github.meypod.al_azan.core.presentation.components.CompactOutlinedTextField
import com.github.meypod.al_azan.core.presentation.util.patternedBackground
import com.github.meypod.al_azan.core.presentation.util.rememberPatternImageBitmap
import com.github.meypod.al_azan.main.home.components.HomeHeader
import io.github.meypod.adhan_kotlin.CalculationMethod
import kotlinx.coroutines.launch
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAction: (HomeUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerState) {
                Text(stringResource(R.string.app_name), modifier = Modifier.padding(dimensionResource(R.dimen.page_padding)))
                HorizontalDivider()
                NavigationDrawerItem(
                    icon = {
                        Icon(painterResource(R.drawable.alarm), contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.reminder)) },
                    selected = false,
                    onClick = {
                        onAction(HomeUiAction.OnReminderLinkClick)
                    },
                )
                NavigationDrawerItem(
                    icon = {
                        Icon(painterResource(R.drawable.compass_outline), contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.qibla)) },
                    selected = false,
                    onClick = {
                        onAction(HomeUiAction.OnQiblaLinkClick)
                    },
                )
                NavigationDrawerItem(
                    icon = {
                        Icon(painterResource(R.drawable.counter), contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.counter)) },
                    selected = false,
                    onClick = {
                        onAction(HomeUiAction.OnCounterLinkClick)
                    },
                )
                NavigationDrawerItem(
                    icon = {
                        Icon(painterResource(R.drawable.settings), contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.settings)) },
                    selected = false,
                    onClick = {
                        onAction(HomeUiAction.OnSettingsLinkClick)
                    },
                )
                NavigationDrawerItem(
                    icon = {
                        Icon(painterResource(R.drawable.info_variant_outline), contentDescription = null)
                    },
                    label = { Text(stringResource(R.string.about_us)) },
                    selected = false,
                    onClick = {
                        onAction(HomeUiAction.OnAboutUsLinkClick)
                    },
                )
            }
        },
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                CenterAlignedTopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            },
                        ) {
                            Icon(painterResource(R.drawable.menu), contentDescription = stringResource(R.string.menu))
                        }
                    },
                    title = {
                        Text(
                            formatInstant(uiState.currentInstant, uiState.locale, uiState.calendar),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                )
            },
        ) { paddingValues ->
            Column(
                Modifier.padding(paddingValues),
            ) {
                HomeHeader(
                    uiState,
                    onAction,
                )
            }
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
private fun HomeLoadedPreview() {
    AlAzanTheme {
        val location = StaticFavoriteLocation("foo", CalculationLocationDetail(0.0, 0.0, label = "Null Island"))
        val instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val getShariaTimesUseCase = GetShariaTimesUseCase()
        val shariahTimes = getShariaTimesUseCase(
            instant = instant,
            calculationParameters = CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters,
            calculationAdjustments = CalculationAdjustments(),
            arabicCalendar = "islamic",
            locationDetail = CalculationLocationDetail(0.0, 0.0),
        )
        val nextShariaTime = GetNextShariaTimesUseCase(getShariaTimesUseCase)(
            instant = instant,
            calculationParameters = CalculationMethod.MOON_SIGHTING_COMMITTEE.parameters,
            calculationAdjustments = CalculationAdjustments(),
            arabicCalendar = "islamic",
            locationDetail = CalculationLocationDetail(0.0, 0.0),
        )
        HomeScreen(
            uiState = HomeUiState(
                calendar = "gregorian",
                location = location,
                shariaTimes = shariahTimes,
                nextShariaTime = nextShariaTime,
            ),
            onAction = {},
        )
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
private fun HomeInitialPreview() {
    AlAzanTheme {
        HomeScreen(
            uiState = HomeUiState(
                calendar = "gregorian",
            ),
            onAction = {},
        )
    }
}
