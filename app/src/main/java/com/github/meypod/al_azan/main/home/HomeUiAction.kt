package com.github.meypod.al_azan.main.home

sealed interface HomeUiAction {
    object OnNextDayClick : HomeUiAction
    object OnPrevDayClick : HomeUiAction
    object OnShowTodayClick : HomeUiAction
    object OnLocationTextClick : HomeUiAction
    object OnCalculationLinkClick : HomeUiAction
    object OnCalendarDateClick : HomeUiAction
    object OnReminderLinkClick : HomeUiAction
    object OnQiblaLinkClick : HomeUiAction
    object OnCounterLinkClick : HomeUiAction
    object OnSettingsLinkClick : HomeUiAction
    object OnUpcomingAlarmsClick : HomeUiAction
    object OnAboutLinkClick : HomeUiAction
    object OnMonthlyViewClick : HomeUiAction
    object OnDeveloperLinkClick : HomeUiAction
}
