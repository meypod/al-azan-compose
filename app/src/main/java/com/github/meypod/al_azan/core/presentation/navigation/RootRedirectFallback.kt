package com.github.meypod.al_azan.core.presentation.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey

/**
 * `entryProvider` fallback for when a route this graph doesn't host reaches its back stack — e.g. a deep
 * link (or any stray [NavigationController.navigateTo]) targeting a Main screen while the intro is
 * showing, since both graphs share one [NavigationController]. Instead of crashing with "Unknown screen",
 * it drops the offending key and falls back to [root]. The transient [NavEntry] renders nothing; its
 * effect fixes the back stack, and the next frame shows the valid top (the previous entry, or [root]).
 */
fun <T : NavKey> rootRedirectFallback(
    backStack: NavBackStack<T>,
    root: T,
): (T) -> NavEntry<T> =
    { key ->
        NavEntry(key) {
            LaunchedEffect(key) {
                backStack.remove(key)
                if (backStack.isEmpty()) backStack.add(root)
            }
        }
    }
