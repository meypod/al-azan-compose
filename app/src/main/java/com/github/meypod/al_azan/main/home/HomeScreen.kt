package com.github.meypod.al_azan.main.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationAdjustments
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.usecase.GetNextShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.usecase.GetShariaTimesUseCase
import com.github.meypod.al_azan.core.domain.util.formatInstant
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.util.dropShadow2
import com.github.meypod.al_azan.main.home.components.ConfigHintCard
import com.github.meypod.al_azan.main.home.components.HomeHeader
import com.github.meypod.al_azan.main.home.components.ShariaTimesBox
import com.github.meypod.al_azan.main.home.components.ShariaTimesBoxUiState
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.data.DateComponents
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

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
        modifier = modifier,
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
                    label = { Text(stringResource(R.string.about)) },
                    selected = false,
                    onClick = {
                        onAction(HomeUiAction.OnAboutLinkClick)
                    },
                )
            }
        },
    ) {
        Scaffold(
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onAction(HomeUiAction.OnMonthlyViewClick) }
                                .padding(6.dp),
                        ) {
                            Icon(painterResource(R.drawable.calendar_month_outline), contentDescription = null)
                            Text(
                                formatInstant(
                                    uiState.viewingInstant,
                                    uiState.locale,
                                    uiState.calendar,
                                    numberingSystem = uiState.numberingSystem,
                                ),
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    modifier = Modifier.graphicsLayer { clip = false },
                    visible = DateComponents.from(uiState.viewingInstant) != DateComponents.from(uiState.currentInstant),
                    enter = slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = tween(durationMillis = 150),
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it * 3 },
                        animationSpec = tween(durationMillis = 200),
                    ),
                ) {
                    val buttonShape = MaterialTheme.shapes.extraLarge
                    ExtendedFloatingActionButton(
                        onClick = { onAction(HomeUiAction.OnShowTodayClick) },
                        shape = buttonShape,
                        modifier = Modifier
                            .widthIn(min = 160.dp)
                            .dropShadow2(buttonShape),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ) {
                        Text(
                            stringResource(R.string.show_today),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center,
        ) { paddingValues ->

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                HomeHeader(
                    uiState,
                    onAction,
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(
                            horizontal = dimensionResource(R.dimen.element_padding),
                        )
                        .then(
                            if (!uiState.themeColor.isClassic()) {
                                Modifier.offset(y = -dimensionResource(R.dimen.home_card_padding))
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    if (uiState.location == null || !uiState.isCalculationConfigured) {
                        ConfigHintCard(
                            missingLocation = uiState.location == null,
                            missingCalculation = !uiState.isCalculationConfigured,
                            onLocationClick = { onAction(HomeUiAction.OnLocationTextClick) },
                            onCalculationClick = { onAction(HomeUiAction.OnCalculationLinkClick) },
                            modifier = Modifier.padding(bottom = dimensionResource(R.dimen.element_padding)),
                        )
                    }
                    ShariaTimesBox(
                        ShariaTimesBoxUiState(
                            shariahTimes = uiState.shariaTimes,
                            locale = uiState.locale,
                            numberingSystem = uiState.numberingSystem,
                            is24Hours = uiState.is24Hour,
                            highlightedShariaTime = uiState.highlightedShariaTime,
                        ),
                    )
                }
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
        val viewing = instant.minus(1.toDuration(DurationUnit.DAYS))
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
                currentInstant = instant,
                viewingInstant = viewing,
                calendar = "gregorian",
                location = location,
                shariaTimes = shariahTimes,
                nextShariaTime = nextShariaTime,
                highlightedShariaTime = nextShariaTime,
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
