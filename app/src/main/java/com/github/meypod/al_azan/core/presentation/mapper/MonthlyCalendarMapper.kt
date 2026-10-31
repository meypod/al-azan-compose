package com.github.meypod.al_azan.core.presentation.mapper

import androidx.annotation.StringRes
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.reminder.MonthlyCalendar

@StringRes
fun MonthlyCalendar.stringRes(): Int =
    when (this) {
        is MonthlyCalendar.Civil -> calendar.stringRes()
        MonthlyCalendar.Lunar -> R.string.calendar_lunar
    }
