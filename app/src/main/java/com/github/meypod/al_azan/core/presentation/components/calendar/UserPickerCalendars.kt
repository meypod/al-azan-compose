package com.github.meypod.al_azan.core.presentation.components.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calendar.CalendarSystem
import com.github.meypod.al_azan.core.domain.model.calendar.lunarCalendarSystem
import com.github.meypod.al_azan.core.domain.model.calendar.secondaryCalendarSystem
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.presentation.mapper.stringRes

/** The user's secondary calendar, then their lunar calendar (with the hijri date adjustment). */
@Composable
fun rememberUserPickerCalendars(
    settings: Settings,
    hijriDateAdjustment: Int,
): List<PickerCalendar> =
    rememberUserPickerCalendars(
        secondaryCalendar = settings.selectedSecondaryCalendar,
        secondary = settings.secondaryCalendarSystem(),
        lunar = settings.lunarCalendarSystem(hijriDateAdjustment),
    )

/** As above, for screens holding the resolved calendars rather than [Settings]. */
@Composable
fun rememberUserPickerCalendars(
    secondaryCalendar: SecondaryCalendar,
    secondary: CalendarSystem,
    lunar: CalendarSystem,
): List<PickerCalendar> {
    val resources = LocalResources.current
    val secondaryLabel = resources.getString(secondaryCalendar.stringRes())
    val lunarLabel = resources.getString(R.string.calendar_lunar)
    return remember(secondary, lunar, secondaryLabel, lunarLabel) {
        listOf(PickerCalendar(secondary, secondaryLabel), PickerCalendar(lunar, lunarLabel))
    }
}
