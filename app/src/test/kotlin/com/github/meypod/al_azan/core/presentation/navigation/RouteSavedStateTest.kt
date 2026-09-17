package com.github.meypod.al_azan.core.presentation.navigation

import android.app.Application
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A route that cannot be saved crashes the app on the next state save, so this covers the whole
 * hierarchy rather than the routes any one graph happens to host.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RouteSavedStateTest {

    /** Sample values for the routes that carry arguments, keyed by type through [KClass.isInstance]. */
    private val parameterizedRoutes = listOf(
        Route.Intro.Adhan.PrayerSchedule(Prayer.Fajr),
        Route.Main.Settings.SoundAndNotifications.PrayerSchedule(Prayer.Isha),
    )

    @Test
    fun `every route survives a back stack save and restore`() {
        val routes = Route::class.sealedSubclasses.map { subclass ->
            checkNotNull(subclass.objectInstance ?: parameterizedRoutes.firstOrNull(subclass::isInstance)) {
                "no sample instance for ${subclass.qualifiedName}, add one to parameterizedRoutes"
            }
        }

        // the sealed serializer's own subtype list, so a route missing from reflection is caught too
        val subtypeCount = Route.serializer().descriptor.getElementDescriptor(1).elementsCount
        assertEquals(subtypeCount, routes.size)

        val backStack = NavBackStack<NavKey>(*routes.toTypedArray())
        val saved = encodeToSavedState(NavBackStackSerializer(), backStack, routeSavedStateConfiguration)
        val restored = decodeFromSavedState(NavBackStackSerializer<NavKey>(), saved, routeSavedStateConfiguration)

        assertEquals(routes, restored.toList())
    }
}
