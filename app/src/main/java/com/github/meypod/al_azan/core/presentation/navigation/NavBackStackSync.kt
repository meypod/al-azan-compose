package com.github.meypod.al_azan.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
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
fun <R : NavKey> BindBackStackWithController(
    currentRoute: () -> R?,
    navigateTo: (NavKey) -> Unit,
    popBack: () -> Unit,
    canPopBack: () -> Boolean,
    onRouteVisible: ((R) -> Unit)? = null,
) {
    LaunchedEffect(currentRoute, onRouteVisible) {
        if (onRouteVisible != null) {
            snapshotFlow { currentRoute() }
                .filterNotNull()
                .distinctUntilChanged()
                .collectLatest(onRouteVisible)
        }
    }

    LaunchedEffect(Unit) {
        NavigationController.navIntents.collect { intent ->
            when (intent) {
                is NavIntent.To -> navigateTo(intent.route)
                NavIntent.Back -> if (canPopBack()) popBack()
            }
        }
    }
}

fun <R : NavKey> NavBackStack<R>.navigateTo(route: R) {
    val index = indexOf(route)
    if (index < 0) {
        add(route)
        return
    }
    while (lastIndex > index) {
        removeAt(lastIndex)
    }
}
