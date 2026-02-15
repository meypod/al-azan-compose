package com.github.meypod.al_azan.intro

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
fun IntroNavigation(modifier: Modifier = Modifier) {
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
                        }
                    }
                },
            Route.Intro.LanguageSelection,
        )

    NavDisplay(
        backStack = introBackstack,
        modifier = modifier,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Route.Intro.LanguageSelection> {
                    // todo
                    Text("Intro")
                }
            },
    )
}
