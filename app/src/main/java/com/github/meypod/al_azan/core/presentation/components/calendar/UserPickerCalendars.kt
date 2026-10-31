package com.github.meypod.al_azan.core.presentation.components.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.calendar.lunarCalendarSystem
import com.github.meypod.al_azan.core.domain.model.calendar.secondaryCalendarSystem
import com.github.meypod.al_azan.core.domain.model.settings.Settings
import com.github.meypod.al_azan.core.presentation.mapper.stringRes

/** The user's secondary calendar, then their lunar calendar (with the hijri date adjustment). */
@Composable
fun rememberUserPickerCalendars(
    settings: Settings,
    hijriDateAdjustment: Int,
): List<PickerCalendar> {
    val resources = LocalResources.current
    val secondaryLabel = resources.getString(settings.selectedSecondaryCalendar.stringRes())
    val lunarLabel = resources.getString(R.string.calendar_lunar)
    val secondary = settings.secondaryCalendarSystem()
    val lunar = settings.lunarCalendarSystem(hijriDateAdjustment)
    return remember(secondary, lunar, secondaryLabel, lunarLabel) {
        listOf(PickerCalendar(secondary, secondaryLabel), PickerCalendar(lunar, lunarLabel))
    }
}
