package com.github.meypod.al_azan.core.presentation.navigation

import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey
import com.github.meypod.al_azan.BuildConfig
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.navigation.DeepLinkableRoute
import com.github.meypod.al_azan.core.presentation.navigation.deeplink.DeepLinkPattern
import kotlinx.serialization.Serializable

@Immutable
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
        data object Calculation : Route {
            @Serializable
            data object Adjustments : Route

            @Serializable
            data object AdvancedCalculation : Route
        }

        @Serializable
        data object Adhan : Route {
            @Serializable
            data class PrayerSchedule(
                val prayer: Prayer,
            ) : Route
        }

        @Serializable
        data object Troubleshoot : Route {
            @Serializable
            data object AdvancedTroubleshoot : Route
        }
    }

    @Serializable
    data object Main : Route {
        @Serializable
        data object Home : Route, DeepLinkableRoute

        /** Status + manual-end screen for an active "Dismiss & silent" window. Reached from the control
         *  notification and from the system DND-rule configuration entry point. */
        @Serializable
        data object SilenceStatus : Route, DeepLinkableRoute

        @Serializable
        data object Location : Route

        @Serializable
        data object CalendarView : Route

        @Serializable
        data object MonthlyView : Route, DeepLinkableRoute

        @Serializable
        data object Reminder : Route, DeepLinkableRoute

        @Serializable
        data object Qibla : Route

        @Serializable
        data object QiblaCompass : Route, DeepLinkableRoute

        @Serializable
        data object Counter : Route, DeepLinkableRoute

        @Serializable
        data object Settings : Route {

            @Serializable
            data object InterfaceSettings : Route, DeepLinkableRoute

            @Serializable
            data object SoundAndNotifications : Route, DeepLinkableRoute {
                @Serializable
                data object ScheduleAndMuezzin : Route, DeepLinkableRoute

                @Serializable
                data class PrayerSchedule(
                    val prayer: Prayer,
                ) : Route
            }

            @Serializable
            data object Calculations : Route {
                @Serializable
                data object Adjustments : Route

                @Serializable
                data object AdvancedCalculation : Route
            }

            @Serializable
            data object Troubleshoot : Route {
                @Serializable
                data object AdvancedTroubleshoot : Route
            }

            @Serializable
            data object WidgetSettings : Route

            @Serializable
            data object BackupAndRestore : Route

            @Serializable
            data object Developer : Route
        }

        @Serializable
        data object UpcomingAlarms : Route

        @Serializable
        data object About : Route
    }
}

internal val deepLinkPatterns: List<DeepLinkPattern<out Route>> by lazy {
    buildList {
        // "al-azan://home"
        add(DeepLinkPattern(Route.Main.Home.serializer(), Route.Main.Home.toUriString().toUri()))
        // "al-azan://SilenceStatus"
        add(DeepLinkPattern(Route.Main.SilenceStatus.serializer(), Route.Main.SilenceStatus.toUriString().toUri()))
        // Debug-only: extra screens for tooling (fastlane screenshot scripts) to open directly via adb.
        if (BuildConfig.DEBUG) {
            add(DeepLinkPattern(Route.Main.MonthlyView.serializer(), Route.Main.MonthlyView.toUriString().toUri()))
            add(DeepLinkPattern(Route.Main.Reminder.serializer(), Route.Main.Reminder.toUriString().toUri()))
            add(DeepLinkPattern(Route.Main.QiblaCompass.serializer(), Route.Main.QiblaCompass.toUriString().toUri()))
            add(DeepLinkPattern(Route.Main.Counter.serializer(), Route.Main.Counter.toUriString().toUri()))
            add(
                DeepLinkPattern(
                    Route.Main.Settings.InterfaceSettings.serializer(),
                    Route.Main.Settings.InterfaceSettings.toUriString().toUri(),
                ),
            )
            add(
                DeepLinkPattern(
                    Route.Main.Settings.SoundAndNotifications.serializer(),
                    Route.Main.Settings.SoundAndNotifications.toUriString().toUri(),
                ),
            )
            add(
                DeepLinkPattern(
                    Route.Main.Settings.SoundAndNotifications.ScheduleAndMuezzin.serializer(),
                    Route.Main.Settings.SoundAndNotifications.ScheduleAndMuezzin.toUriString().toUri(),
                ),
            )
        }
    }
}
