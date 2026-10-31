package com.github.meypod.al_azan.main.home

import kotlinx.datetime.LocalDate

sealed interface HomeUiAction {
    object OnNextDayClick : HomeUiAction
    object OnPrevDayClick : HomeUiAction
    object OnShowTodayClick : HomeUiAction

    /** Shows [date], as picked from the header's date picker showing [calendarIndex]. */
    data class OnDatePicked(
        val date: LocalDate,
        val calendarIndex: Int,
    ) : HomeUiAction
    object OnLocationTextClick : HomeUiAction
    object OnCalculationLinkClick : HomeUiAction
    object OnCalendarDateClick : HomeUiAction
    object OnReminderLinkClick : HomeUiAction
    object OnQiblaLinkClick : HomeUiAction
    object OnQiblaShortcutClick : HomeUiAction
    object OnCounterLinkClick : HomeUiAction
    object OnSettingsLinkClick : HomeUiAction
    object OnUpcomingAlarmsClick : HomeUiAction
    object OnAboutLinkClick : HomeUiAction
    object OnMonthlyViewClick : HomeUiAction
    object OnDeveloperLinkClick : HomeUiAction
}
