package com.github.meypod.al_azan.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

sealed interface NavIntent<out R> {
    data class To<R>(
        val route: R,
    ) : NavIntent<R>

    data object Back : NavIntent<Nothing>
}

@Composable
fun <R> BindBackStackWithViewModel(
    currentRoute: () -> R?,
    navIntents: () -> Flow<NavIntent<R>>,
    onRouteVisible: (R) -> Unit,
    navigateTo: (R) -> Unit,
    popBack: () -> Unit,
    canPopBack: () -> Boolean,
) {
    LaunchedEffect(currentRoute) {
        snapshotFlow { currentRoute() }
            .filterNotNull()
            .distinctUntilChanged()
            .collectLatest(onRouteVisible)
    }

    LaunchedEffect(navIntents) {
        navIntents().collect { intent ->
            when (intent) {
                is NavIntent.To -> navigateTo(intent.route)
                NavIntent.Back -> if (canPopBack()) popBack()
            }
        }
    }
}
