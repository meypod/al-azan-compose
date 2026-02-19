package com.github.meypod.al_azan.intro

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
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
import com.github.meypod.al_azan.core.presentation.patternedBackground
import com.github.meypod.al_azan.core.presentation.rememberPatternImageBitmap
import com.github.meypod.al_azan.intro.components.IntroSkipButton
import com.github.meypod.al_azan.intro.languageselection.LanguageSelectionScreen
import com.github.meypod.al_azan.intro.languageselection.LanguageSelectionViewModel
import com.github.meypod.al_azan.intro.restorebackup.RestoreBackupScreen
import com.github.meypod.al_azan.intro.restorebackup.RestoreBackupViewModel
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
                    IntroStepScaffold(
                        uiState = introUiState,
                        onAction = onIntroUiAction,
                    ) {
                        // TODO
                    }
                }
            },
    )
    if (introUiState.showSkipDialog) {
        TimedDangerDialog(
            title = stringResource(R.string.skip_warning_title),
            text = stringResource(R.string.skip_warning_body),
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
    content: @Composable (Modifier) -> Unit,
) {
    val patternImage = rememberPatternImageBitmap(R.drawable.pattern)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            IntroBottomBar(
                uiState = uiState,
                onAction = onAction,
            )
        },
        containerColor = colorResource(R.color.intro_background),
    ) { paddingValues ->
        content(
            Modifier
                .fillMaxSize()
                .patternedBackground(
                    pattern = patternImage,
                    backgroundColor = colorResource(R.color.intro_background),
                    patternAlpha = 0.03f,
                )
                .verticalScroll(rememberScrollState())
                .padding(paddingValues),
        )
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
