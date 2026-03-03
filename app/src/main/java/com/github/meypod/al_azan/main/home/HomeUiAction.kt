package com.github.meypod.al_azan.main.home

import android.app.Activity
import android.content.Context

sealed interface HomeUiAction {
    object OnNextDayClick : HomeUiAction
    object OnPrevDayClick : HomeUiAction
    object OnShowTodayClick : HomeUiAction
    object OnLocationTextClick : HomeUiAction
    object OnCalendarDateClick : HomeUiAction
    object OnMenuIconClick : HomeUiAction
}
