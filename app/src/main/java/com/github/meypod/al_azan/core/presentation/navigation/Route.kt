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

        @Serializable
        data object Adhan : Route

        @Serializable
        data object Troubleshoot : Route
    }

    @Serializable
    data object Main : Route {
        @Serializable
        data object Home : Route

        @Serializable
        data object Location : Route

        @Serializable
        data object CalendarView : Route

        @Serializable
        data object Reminder : Route

        @Serializable
        data object Qibla : Route

        @Serializable
        data object Counter : Route

        @Serializable
        data object Settings : Route {

            @Serializable
            data object InterfaceSettings : Route

            @Serializable
            data object SoundAndNotifications : Route

            @Serializable
            data object Calculations : Route

            @Serializable
            data object Troubleshoot : Route

            @Serializable
            data object WidgetSettings : Route

            @Serializable
            data object BackupAndRestore : Route

            @Serializable
            data object Developer : Route
        }

        @Serializable
        data object About : Route
    }
}
