package com.github.meypod.al_azan.core.domain.model.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A destination the user can surface as a shortcut icon button in the home top app bar. */
@Serializable
enum class HomeShortcut(
    @param:StringRes val labelRes: Int,
) {
    @SerialName("qibla")
    Qibla(R.string.qibla),

    @SerialName("counter")
    Counter(R.string.counter),

    @SerialName("reminders")
    Reminders(R.string.reminders_title),

    @SerialName("monthly_view")
    MonthlyView(R.string.monthly_view_title),

    @SerialName("upcoming_alarms")
    UpcomingAlarms(R.string.upcoming_alarms),
}

@Composable
fun HomeShortcut.i18n() = stringResource(labelRes)

val HOME_SHORTCUTS_IN_ORDER: List<HomeShortcut> = HomeShortcut.entries.toList()

/** The home top app bar stays uncluttered: at most this many shortcut buttons at once. */
const val MAX_HOME_SHORTCUTS = 2

/** Hiding the title bar calendar frees space for more shortcut buttons. */
const val MAX_HOME_SHORTCUTS_TOOLBAR_HIDDEN = 4

/** Max shortcuts allowed in the home top app bar, given whether the title bar calendar is hidden. */
fun maxHomeShortcuts(toolbarCalendarHidden: Boolean): Int =
    if (toolbarCalendarHidden) MAX_HOME_SHORTCUTS_TOOLBAR_HIDDEN else MAX_HOME_SHORTCUTS
