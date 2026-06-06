package com.github.meypod.al_azan.core.presentation.navigation

import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey
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
            data object Muezzin : Route

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

        @Serializable
        data object Location : Route

        @Serializable
        data object CalendarView : Route

        @Serializable
        data object MonthlyView : Route

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
            data object SoundAndNotifications : Route {
                @Serializable
                data object ScheduleAndMuezzin : Route

                @Serializable
                data object Muezzin : Route

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
        data object About : Route
    }
}

internal val deepLinkPatterns: List<DeepLinkPattern<out Route>> by lazy {
    listOf(
        // "al-azan://home"
        DeepLinkPattern(Route.Main.Home.serializer(), Route.Main.Home.toUriString().toUri()),
    )
}
