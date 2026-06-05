package com.github.meypod.al_azan.intro

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.presentation.AlAzanTheme
import com.github.meypod.al_azan.core.presentation.components.SecondaryButton
import com.github.meypod.al_azan.core.presentation.components.TertiaryButton
import com.github.meypod.al_azan.core.presentation.components.TimedDangerDialog
import com.github.meypod.al_azan.core.presentation.navigation.BindBackStackWithController
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.core.presentation.navigation.navigateTo
import com.github.meypod.al_azan.core.presentation.navigation.rememberHorizontalSlideDirections
import com.github.meypod.al_azan.core.presentation.util.drawVerticalScrollbar
import com.github.meypod.al_azan.core.presentation.util.fadeScrollEdges
import com.github.meypod.al_azan.core.presentation.util.patternedBackground
import com.github.meypod.al_azan.core.presentation.util.rememberPatternImageBitmap
import com.github.meypod.al_azan.intro.components.IntroSkipButton
import com.github.meypod.al_azan.intro.components.IntroTitle
import com.github.meypod.al_azan.intro.languageselection.LanguageSelectionScreen
import com.github.meypod.al_azan.intro.languageselection.LanguageSelectionViewModel
import com.github.meypod.al_azan.intro.restorebackup.RestoreBackupScreen
import com.github.meypod.al_azan.intro.restorebackup.RestoreBackupViewModel
import com.github.meypod.al_azan.main.location.LocationScreenContent
import com.github.meypod.al_azan.main.location.LocationUiAction
import com.github.meypod.al_azan.main.location.LocationViewModel
import com.github.meypod.al_azan.main.settings.adhan.AdhanScheduleContent
import com.github.meypod.al_azan.main.settings.adhan.AdhanSettingsViewModel
import com.github.meypod.al_azan.main.settings.adhan.PrayerScheduleScreen
import com.github.meypod.al_azan.main.settings.adhan.muezzin.MuezzinPickerScreen
import com.github.meypod.al_azan.main.settings.adhan.muezzin.MuezzinPickerViewModel
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsScreen
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsViewModel
import com.github.meypod.al_azan.main.settings.calculation.adjustments.AdjustmentsScreen
import com.github.meypod.al_azan.main.settings.calculation.adjustments.AdjustmentsViewModel
import com.github.meypod.al_azan.main.settings.calculation.advanced.AdvancedCalcScreen
import com.github.meypod.al_azan.main.settings.calculation.advanced.AdvancedCalcViewModel
import com.github.meypod.al_azan.main.settings.troubleshoot.TroubleshootScreen
import com.github.meypod.al_azan.main.settings.troubleshoot.TroubleshootViewModel
import com.github.meypod.al_azan.main.settings.troubleshoot.advanced.AdvancedTroubleshootScreen
import com.github.meypod.al_azan.main.settings.troubleshoot.advanced.AdvancedTroubleshootViewModel
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun IntroNavigation(onFinishIntro: () -> Unit) {
    val slideDirections = rememberHorizontalSlideDirections()

    val introBackstack =
        rememberNavBackStack(
            configuration =
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(
                                Route.Intro.LanguageSelection::class,
                                Route.Intro.LanguageSelection.serializer(),
                            )
                            subclass(
                                Route.Intro.RestoreBackup::class,
                                Route.Intro.RestoreBackup.serializer(),
                            )
                            subclass(
                                Route.Intro.Location::class,
                                Route.Intro.Location.serializer(),
                            )
                            subclass(
                                Route.Intro.Calculation::class,
                                Route.Intro.Calculation.serializer(),
                            )
                            subclass(
                                Route.Intro.Calculation.Adjustments::class,
                                Route.Intro.Calculation.Adjustments.serializer(),
                            )
                            subclass(
                                Route.Intro.Calculation.AdvancedCalculation::class,
                                Route.Intro.Calculation.AdvancedCalculation.serializer(),
                            )
                            subclass(
                                Route.Intro.Adhan::class,
                                Route.Intro.Adhan.serializer(),
                            )
                            subclass(
                                Route.Intro.Adhan.Muezzin::class,
                                Route.Intro.Adhan.Muezzin.serializer(),
                            )
                            subclass(
                                Route.Intro.Adhan.PrayerSchedule::class,
                                Route.Intro.Adhan.PrayerSchedule.serializer(),
                            )
                            subclass(
                                Route.Intro.Troubleshoot::class,
                                Route.Intro.Troubleshoot.serializer(),
                            )
                            subclass(
                                Route.Intro.Troubleshoot.AdvancedTroubleshoot::class,
                                Route.Intro.Troubleshoot.AdvancedTroubleshoot.serializer(),
                            )
                        }
                    }
                },
            Route.Intro.LanguageSelection,
        )

    val introViewModel = hiltViewModel<IntroViewModel>()
    val introUiState by introViewModel.uiState.collectAsStateWithLifecycle()
    val introBackgroundColor = colorResource(R.color.intro_background)

    BindBackStackWithController(
        currentRoute = introBackstack::lastOrNull,
        navigateTo = introBackstack::navigateTo,
        popBack = { introBackstack.removeAt(introBackstack.lastIndex) },
        canPopBack = { introBackstack.size > 1 },
        onRouteVisible = { (it as? Route)?.let { route -> introViewModel.onAction(IntroUiAction.OnRouteVisible(route)) } },
    )

    LaunchedEffect(introUiState.appIntroDone) {
        if (introUiState.appIntroDone) {
            onFinishIntro()
        }
    }

    NavDisplay(
        backStack = introBackstack,
        modifier = Modifier
            .fillMaxSize()
            .background(introBackgroundColor),
        contentAlignment = Alignment.Center,
        transitionSpec = {
            slideInHorizontally(
                animationSpec = tween(280),
                initialOffsetX = { fullWidth -> fullWidth * slideDirections.forwardEnter },
            ) togetherWith
                slideOutHorizontally(
                    animationSpec = tween(280),
                    targetOffsetX = { fullWidth -> fullWidth * slideDirections.forwardExit / 2 },
                )
        },
        popTransitionSpec = {
            slideInHorizontally(
                animationSpec = tween(280),
                initialOffsetX = { fullWidth -> fullWidth * slideDirections.backEnter / 2 },
            ) togetherWith
                slideOutHorizontally(
                    animationSpec = tween(280),
                    targetOffsetX = { fullWidth -> fullWidth * slideDirections.backExit },
                )
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(
                animationSpec = tween(280),
                initialOffsetX = { fullWidth -> fullWidth * slideDirections.backEnter / 2 },
            ) togetherWith
                slideOutHorizontally(
                    animationSpec = tween(280),
                    targetOffsetX = { fullWidth -> fullWidth * slideDirections.backExit },
                )
        },
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Route.Intro.LanguageSelection> {
                    val viewModel = hiltViewModel<LanguageSelectionViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    LanguageSelectionScreen(
                        uiState = uiState,
                        onAction = viewModel::onAction,
                        onIntroAction = introViewModel::onAction,
                    )
                }
                entry<Route.Intro.RestoreBackup> {
                    val viewModel = hiltViewModel<RestoreBackupViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = introViewModel::onAction,
                    ) { modifier ->
                        RestoreBackupScreen(
                            uiState = uiState,
                            onAction = viewModel::onAction,
                            onIntroAction = introViewModel::onAction,
                            modifier = modifier,
                            busy = introUiState.busy,
                        )
                    }
                }
                entry<Route.Intro.Location> {
                    val viewModel = hiltViewModel<LocationViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = introViewModel::onAction,
                        floatingActionButton = {
                            if (uiState.locations.isNotEmpty()) {
                                FloatingActionButton(
                                    onClick = {
                                        viewModel.onAction(LocationUiAction.OnNewLocationClick)
                                    },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ) {
                                    Icon(
                                        painterResource(R.drawable.add),
                                        contentDescription = stringResource(R.string.add_new_location_button),
                                    )
                                }
                            }
                        },
                    ) { modifier ->
                        Column(
                            modifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                        ) {
                            IntroTitle(R.string.location_title)
                            LocationScreenContent(
                                uiState = uiState,
                                onAction = viewModel::onAction,
                                getCountries = viewModel::getCountries,
                                getCities = viewModel::getCities,
                            )
                        }
                    }
                }
                entry<Route.Intro.Calculation> {
                    val viewModel = hiltViewModel<CalculationSettingsViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = introViewModel::onAction,
                    ) { modifier ->
                        Column(
                            modifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                        ) {
                            IntroTitle(R.string.calculation_title)
                            CalculationSettingsScreen(
                                uiState = uiState,
                                onAction = viewModel::onAction,
                                adjustmentsRoute = Route.Intro.Calculation.Adjustments,
                                advancedRoute = Route.Intro.Calculation.AdvancedCalculation,
                            )
                        }
                    }
                }
                entry<Route.Intro.Calculation.Adjustments> {
                    val viewModel = hiltViewModel<AdjustmentsViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    AdjustmentsScreen(
                        uiState = uiState,
                        onAction = viewModel::onAction,
                    )
                }
                entry<Route.Intro.Calculation.AdvancedCalculation> {
                    val viewModel = hiltViewModel<AdvancedCalcViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    AdvancedCalcScreen(
                        uiState = uiState,
                        onAction = viewModel::onAction,
                    )
                }
                entry<Route.Intro.Adhan> {
                    val viewModel = hiltViewModel<AdhanSettingsViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = introViewModel::onAction,
                    ) { modifier ->
                        Column(
                            modifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                        ) {
                            IntroTitle(R.string.adhan_schedule_title)
                            AdhanScheduleContent(
                                uiState = uiState,
                                onAction = viewModel::onAction,
                                muezzinRoute = Route.Intro.Adhan.Muezzin,
                                prayerScheduleRoute = { Route.Intro.Adhan.PrayerSchedule(it) },
                            )
                        }
                    }
                }
                entry<Route.Intro.Adhan.Muezzin> {
                    val viewModel = hiltViewModel<MuezzinPickerViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    MuezzinPickerScreen(
                        uiState = uiState,
                        onAction = viewModel::onAction,
                    )
                }
                entry<Route.Intro.Adhan.PrayerSchedule> { key ->
                    val viewModel = hiltViewModel<AdhanSettingsViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    PrayerScheduleScreen(
                        prayer = key.prayer,
                        uiState = uiState,
                        onAction = viewModel::onAction,
                    )
                }
                entry<Route.Intro.Troubleshoot> {
                    val viewModel = hiltViewModel<TroubleshootViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = introViewModel::onAction,
                    ) { modifier ->
                        Column(
                            modifier,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
                        ) {
                            IntroTitle(R.string.troubleshoot_title)
                            TroubleshootScreen(
                                uiState = uiState,
                                onAction = viewModel::onAction,
                                advancedRoute = Route.Intro.Troubleshoot.AdvancedTroubleshoot,
                            )
                        }
                    }
                }
                entry<Route.Intro.Troubleshoot.AdvancedTroubleshoot> {
                    val viewModel = hiltViewModel<AdvancedTroubleshootViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    AdvancedTroubleshootScreen(
                        uiState = uiState,
                        onAction = viewModel::onAction,
                    )
                }
            },
    )
    if (introUiState.showSkipDialog) {
        TimedDangerDialog(
            title = stringResource(R.string.attention_title),
            text = stringResource(R.string.skip_dialog_body),
            confirmLabel = stringResource(R.string.skip_confirm),
            dismissLabel = stringResource(R.string.cancel),
            seconds = 3,
            onDismissRequest = { introViewModel.onAction(IntroUiAction.OnSkipDismiss) },
            onConfirm = { introViewModel.onAction(IntroUiAction.OnSkipConfirmed) },
        )
    }
}

@Composable
private fun IntroStepScaffold(
    uiState: IntroUiState,
    onAction: (IntroUiAction) -> Unit,
    scrollable: Boolean = true,
    floatingActionButton: @Composable (() -> Unit) = {},
    content: @Composable (Modifier) -> Unit,
) {
    val patternImage = rememberPatternImageBitmap(R.drawable.pattern)
    val scrollState = rememberScrollState()
    val introBackgroundColor = colorResource(R.color.intro_background)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            IntroBottomBar(
                uiState = uiState,
                onAction = onAction,
            )
        },
        containerColor = introBackgroundColor,
        floatingActionButton = floatingActionButton,
    ) { paddingValues ->
        val baseModifier =
            Modifier
                .fillMaxSize()
                .patternedBackground(
                    pattern = patternImage,
                    backgroundColor = introBackgroundColor,
                    patternAlpha = 0.03f,
                )
                .padding(paddingValues)

        val contentModifier =
            if (scrollable) {
                baseModifier
                    .fadeScrollEdges(scrollState, Orientation.Vertical)
                    .drawVerticalScrollbar(scrollState, barColor = @Composable { MaterialTheme.colorScheme.onPrimary })
                    .verticalScroll(scrollState)
            } else {
                baseModifier
            }.padding(dimensionResource(R.dimen.page_padding))

        content(contentModifier)
    }
}

@Composable
private fun IntroBottomBar(
    uiState: IntroUiState,
    onAction: (IntroUiAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = dimensionResource(R.dimen.page_padding),
                vertical = dimensionResource(R.dimen.element_padding),
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.element_padding)),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.Center,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButton(
                onClick = {
                    onAction(IntroUiAction.OnBackClick)
                },
            ) {
                Text(stringResource(R.string.back_button))
            }
            IntroSkipButton {
                onAction(IntroUiAction.OnSkipClick)
            }
            if (uiState.isLastStep) {
                TertiaryButton(
                    onClick = {
                        onAction(IntroUiAction.OnFinishClick)
                    },
                ) {
                    Text(stringResource(R.string.finish_button))
                }
            } else {
                TertiaryButton(
                    onClick = {
                        onAction(IntroUiAction.OnNextClick)
                    },
                ) {
                    Text(stringResource(R.string.next_button))
                }
            }
        }

        LinearProgressIndicator(
            progress = { uiState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            trackColor = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IntroStepScaffoldPreview() {
    AlAzanTheme {
        IntroStepScaffold(
            uiState = IntroUiState(route = Route.Intro.LanguageSelection),
            onAction = {},
        ) {
            Column(it) { }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IntroBottomBarEmptyPreview() {
    AlAzanTheme {
        IntroBottomBar(
            uiState = IntroUiState(route = Route.Intro.LanguageSelection),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun IntroBottomBarFilledPreview() {
    AlAzanTheme {
        IntroBottomBar(
            uiState = IntroUiState(route = Route.Intro.RestoreBackup),
            onAction = {},
        )
    }
}
