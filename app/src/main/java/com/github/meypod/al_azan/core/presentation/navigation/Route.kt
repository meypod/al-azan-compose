package com.github.meypod.al_azan.core.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object Intro : Route {
        @Serializable
        data object LanguageSelection : Route

        @Serializable
        data object RestoreBackup : Route

        @Serializable
        data object Location : Route

        @Serializable
        data object Calculation : Route
    }

    @Serializable
    data object Main : Route {
        @Serializable
        data object Home : Route
    }
}
