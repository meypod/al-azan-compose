package com.github.meypod.al_azan.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModule

/**
 * Saved-state configuration shared by every back stack in the app.
 *
 * Back stacks save their keys polymorphically as [NavKey], so each key's concrete type has to be
 * resolvable. [Route] is sealed and its own serializer already covers the whole hierarchy, so it is
 * registered as the polymorphic default instead of route by route: nothing to keep in sync when a
 * route is added, and no graph can be handed a key it cannot save.
 *
 * The last part matters because routes cross graphs: deep links and [NavigationController] are not
 * graph-aware, and [rootRedirectFallback] only evicts a foreign key on the next frame, after a state
 * save may already have happened.
 */
val routeSavedStateConfiguration =
    SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphicDefaultSerializer(NavKey::class) { value ->
                @Suppress("UNCHECKED_CAST")
                (Route.serializer() as SerializationStrategy<NavKey>).takeIf { value is Route }
            }
            polymorphicDefaultDeserializer(NavKey::class) { Route.serializer() }
        }
    }
