package com.github.meypod.al_azan.core.presentation.mapper

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.meypod.al_azan.R
import kotlinx.datetime.DayOfWeek

@StringRes
fun DayOfWeek.stringRes(): Int =
    when (this) {
        DayOfWeek.SUNDAY -> R.string.weekday_sunday
        DayOfWeek.MONDAY -> R.string.weekday_monday
        DayOfWeek.TUESDAY -> R.string.weekday_tuesday
        DayOfWeek.WEDNESDAY -> R.string.weekday_wednesday
        DayOfWeek.THURSDAY -> R.string.weekday_thursday
        DayOfWeek.FRIDAY -> R.string.weekday_friday
        DayOfWeek.SATURDAY -> R.string.weekday_saturday
    }

@Composable
fun DayOfWeek.localized(): String = stringResource(stringRes())
