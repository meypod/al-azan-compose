package com.github.meypod.al_azan.main

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.github.meypod.al_azan.core.presentation.navigation.Route
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
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
                        }
                    }
                },
            Route.Main.Home,
        )

    NavDisplay(
        backStack = mainBackstack,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Route.Main.Home> {
                    // todo
                    Text("Home")
                }
            },
    )
}
