package com.github.meypod.al_azan.intro

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.github.meypod.al_azan.core.presentation.navigation.BindBackStackWithViewModel
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.core.presentation.patternedBackground
import com.github.meypod.al_azan.core.presentation.rememberPatternImageBitmap
import com.github.meypod.al_azan.intro.languageselection.LanguageSelectionScreen
import com.github.meypod.al_azan.intro.languageselection.LanguageSelectionViewModel
import com.github.meypod.al_azan.intro.restorebackup.RestoreBackupScreen
import com.github.meypod.al_azan.intro.restorebackup.RestoreBackupViewModel
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun IntroNavigation(onFinishIntro: () -> Unit) {
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
                        }
                    }
                },
            Route.Intro.LanguageSelection,
        )

    val introViewModel = hiltViewModel<IntroViewModel>()
    val introUiState by introViewModel.uiState.collectAsStateWithLifecycle()

    val onIntroAction: (IntroUiAction) -> Unit = { action ->
        when (action) {
            IntroUiAction.OnFinishClick -> onFinishIntro()
            else -> introViewModel.onAction(action)
        }
    }

    BindBackStackWithViewModel(
        currentRoute = { introBackstack.lastOrNull() },
        navIntents = { introViewModel.navIntents },
        onRouteVisible = { route ->
            if (route is Route) {
                introViewModel.onAction(IntroUiAction.OnRouteVisible(route))
            }
        },
        navigateTo = { route -> introBackstack.navigateTo(route) },
        popBack = { introBackstack.removeAt(introBackstack.lastIndex) },
        canPopBack = { introBackstack.size > 1 },
    )

    val patternImage = rememberPatternImageBitmap(R.drawable.pattern)
    val patternModifier = remember(introUiState.step > 0) {
        Modifier
            .fillMaxSize()
            .let { base ->
                if (introUiState.step > 0) {
                    base.patternedBackground(
                        pattern = patternImage,
                        backgroundColor = Color(0xFF00585A),
                        patternAlpha = 0.03f,
                    )
                } else {
                    base
                }
            }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (introUiState.step > 0) {
                Row(horizontalArrangement = Arrangement.Center) {
                    // TODO
                }
            }
        },
        containerColor = Color(0xFF00585A),
    ) { paddingValues ->
        NavDisplay(
            backStack = introBackstack,
            modifier = patternModifier
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
            transitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { fullWidth -> fullWidth },
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { fullWidth -> -fullWidth / 3 },
                    ) + fadeOut(animationSpec = tween(300))
            },
            popTransitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { fullWidth -> -fullWidth / 3 },
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { fullWidth -> fullWidth },
                    ) + fadeOut(animationSpec = tween(300))
            },
            predictivePopTransitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = { fullWidth -> -fullWidth / 3 },
                ) + fadeIn(animationSpec = tween(300)) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { fullWidth -> fullWidth },
                    ) + fadeOut(animationSpec = tween(300))
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
                            onIntroAction = onIntroAction,
                        )
                    }
                    entry<Route.Intro.RestoreBackup> {
                        val viewModel = hiltViewModel<RestoreBackupViewModel>()
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        RestoreBackupScreen(
                            uiState = uiState,
                            onAction = viewModel::onAction,
                            onIntroAction = onIntroAction,
                            busy = introUiState.busy,
                        )
                    }
                },
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
