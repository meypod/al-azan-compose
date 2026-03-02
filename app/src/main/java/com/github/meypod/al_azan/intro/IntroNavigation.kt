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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.github.meypod.al_azan.core.presentation.navigation.BindBackStackWithViewModel
import com.github.meypod.al_azan.core.presentation.navigation.Route
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
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsScreen
import com.github.meypod.al_azan.main.settings.calculation.CalculationSettingsViewModel
import com.github.meypod.al_azan.main.location.LocationScreen
import com.github.meypod.al_azan.main.location.LocationUiAction
import com.github.meypod.al_azan.main.location.LocationViewModel
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
                        }
                    }
                },
            Route.Intro.LanguageSelection,
        )

    val introViewModel = hiltViewModel<IntroViewModel>()
    val introUiState by introViewModel.uiState.collectAsStateWithLifecycle()

    val onIntroUiAction: (IntroUiAction) -> Unit = remember(onFinishIntro) {
        { action ->
            introViewModel.onAction(action, onFinishIntro)
        }
    }
    val introBackgroundColor = colorResource(R.color.intro_background)

    BindBackStackWithViewModel(
        currentRoute = { introBackstack.lastOrNull() },
        navIntents = { introViewModel.navIntents },
        onRouteVisible = { route ->
            if (route is Route) {
                onIntroUiAction(IntroUiAction.OnRouteVisible(route))
            }
        },
        navigateTo = { route -> introBackstack.navigateTo(route) },
        popBack = { introBackstack.removeAt(introBackstack.lastIndex) },
        canPopBack = { introBackstack.size > 1 },
    )

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
                        onIntroAction = onIntroUiAction,
                    )
                }
                entry<Route.Intro.RestoreBackup> {
                    val viewModel = hiltViewModel<RestoreBackupViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = onIntroUiAction,
                    ) { modifier ->
                        RestoreBackupScreen(
                            uiState = uiState,
                            onAction = viewModel::onAction,
                            onIntroAction = onIntroUiAction,
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
                        onAction = onIntroUiAction,
                        scrollable = false,
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
                            LocationScreen(
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
                        onAction = onIntroUiAction,
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
                            )
                        }
                    }
                }
            },
    )
    if (introUiState.showSkipDialog) {
        TimedDangerDialog(
            title = stringResource(R.string.attention_title),
            text = stringResource(R.string.skip_dialog_body),
            confirmLabel = stringResource(R.string.skip_confirm),
            cancelLabel = stringResource(R.string.cancel),
            seconds = 3,
            onConfirm = { onIntroUiAction(IntroUiAction.OnSkipConfirmed) },
            onDismissRequest = { onIntroUiAction(IntroUiAction.OnSkipDismiss) },
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
                    .drawVerticalScrollbar(scrollState)
                    .verticalScroll(scrollState)
            } else {
                baseModifier
            }

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
            TertiaryButton(
                onClick = {
                    onAction(IntroUiAction.OnNextClick)
                },
            ) {
                if (uiState.isLastStep) {
                    Text(stringResource(R.string.finish_button))
                } else {
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

private fun <R> MutableList<R>.navigateTo(route: R) {
    val index = indexOf(route)
    if (index < 0) {
        add(route)
        return
    }
    while (lastIndex > index) {
        removeAt(lastIndex)
    }
}
