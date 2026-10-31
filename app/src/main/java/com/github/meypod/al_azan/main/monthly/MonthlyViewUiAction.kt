package com.github.meypod.al_azan.main.monthly

import kotlinx.datetime.LocalDate

sealed interface MonthlyViewUiAction {
    object OnPrevMonthClick : MonthlyViewUiAction
    object OnNextMonthClick : MonthlyViewUiAction
    object OnShowThisMonthClick : MonthlyViewUiAction
    /** Shows the month of [date] in [mode], as picked in the date picker. */
    data class OnDatePicked(
        val date: LocalDate,
        val mode: MonthlyCalendarMode,
    ) : MonthlyViewUiAction
}
