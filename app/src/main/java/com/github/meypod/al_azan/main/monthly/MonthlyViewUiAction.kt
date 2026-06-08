package com.github.meypod.al_azan.main.monthly

sealed interface MonthlyViewUiAction {
    object OnPrevMonthClick : MonthlyViewUiAction
    object OnNextMonthClick : MonthlyViewUiAction
    object OnShowThisMonthClick : MonthlyViewUiAction
    object OnToggleCalendarClick : MonthlyViewUiAction
}
