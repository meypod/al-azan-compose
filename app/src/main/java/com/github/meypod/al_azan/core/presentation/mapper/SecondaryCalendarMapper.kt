package com.github.meypod.al_azan.core.presentation.mapper

import androidx.annotation.StringRes
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.settings.SecondaryCalendar

@StringRes
fun SecondaryCalendar.stringRes(): Int =
    when (this) {
        SecondaryCalendar.Gregorian -> R.string.calendar_gregorian
        SecondaryCalendar.Persian -> R.string.calendar_persian
        SecondaryCalendar.Ethiopic -> R.string.calendar_ethiopic
        SecondaryCalendar.Buddhist -> R.string.calendar_buddhist
    }
