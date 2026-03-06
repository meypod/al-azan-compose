package com.github.meypod.al_azan.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.github.meypod.al_azan.core.presentation.navigation.BindBackStackWithViewModel
import com.github.meypod.al_azan.core.presentation.navigation.Route
import com.github.meypod.al_azan.core.presentation.navigation.navigateTo
import com.github.meypod.al_azan.core.presentation.navigation.rememberHorizontalSlideDirections
import com.github.meypod.al_azan.main.home.HomeScreen
import com.github.meypod.al_azan.main.home.HomeViewModel
import com.github.meypod.al_azan.main.location.LocationScreen
import com.github.meypod.al_azan.main.location.LocationScreenContent
import com.github.meypod.al_azan.main.location.LocationViewModel
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
    val slideDirections = rememberHorizontalSlideDirections()

    val mainBackstack =
        rememberNavBackStack(
            configuration =
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(
                                Route.Main.Home::class,
                                Route.Main.Home.serializer(),
                            )
                            subclass(
                                Route.Main.Location::class,
                                Route.Main.Location.serializer(),
                            )
                        }
                    }
                },
            Route.Main.Home,
        )

    NavDisplay(
        backStack = mainBackstack,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
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
                entry<Route.Main.Home> {
                    val viewModel = hiltViewModel<HomeViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    BindBackStackWithViewModel(
                        currentRoute = { mainBackstack.lastOrNull() },
                        navIntents = { viewModel.navIntents },
                        navigateTo = { route -> mainBackstack.navigateTo(route) },
                        popBack = { mainBackstack.removeAt(mainBackstack.lastIndex) },
                        canPopBack = { mainBackstack.size > 1 },
                    )

                    HomeScreen(uiState, viewModel::onAction)
                }
                entry<Route.Main.Location> {
                    val viewModel = hiltViewModel<LocationViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    BindBackStackWithViewModel(
                        currentRoute = { mainBackstack.lastOrNull() },
                        navIntents = { viewModel.navIntents },
                        navigateTo = { route -> mainBackstack.navigateTo(route) },
                        popBack = { mainBackstack.removeAt(mainBackstack.lastIndex) },
                        canPopBack = { mainBackstack.size > 1 },
                    )

                    LocationScreen(
                        uiState,
                        viewModel::onAction,
                        getCountries = viewModel::getCountries,
                        getCities = viewModel::getCities,
                    )
                }
            },
    )
}
