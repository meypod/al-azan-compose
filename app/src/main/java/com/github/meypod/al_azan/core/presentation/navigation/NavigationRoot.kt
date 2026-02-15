package com.github.meypod.al_azan.core.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.github.meypod.al_azan.intro.IntroNavigation
import com.github.meypod.al_azan.main.MainNavigation
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
    appIntroDone: Boolean,
) {
    val rootBackStack =
        rememberNavBackStack(
            configuration =
                SavedStateConfiguration {
                    serializersModule = SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(Route.Intro::class, Route.Intro.serializer())
                            subclass(Route.Main::class, Route.Main.serializer())
                        }
                    }
                },
            if (appIntroDone) Route.Main else Route.Intro,
        )

    NavDisplay(
        modifier = modifier,
        backStack = rootBackStack,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Route.Intro> { IntroNavigation() }

                entry<Route.Main> { MainNavigation() }
            },
    )
}
