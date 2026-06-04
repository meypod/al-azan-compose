package com.github.meypod.al_azan.main.monthly

import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.core.presentation.navigation.NavigationController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MonthlyViewViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(MonthlyViewUiState(monthLabel = "", rows = placeholderRows()))
    val uiState = _uiState.asStateFlow()

    fun onAction(action: MonthlyViewUiAction) {
        when (action) {
            MonthlyViewUiAction.OnBackClick -> NavigationController.navigateBack()
            MonthlyViewUiAction.OnPrevMonthClick -> _uiState.update { it.copy(isCurrentMonth = false) }
            MonthlyViewUiAction.OnNextMonthClick -> _uiState.update { it.copy(isCurrentMonth = false) }
            MonthlyViewUiAction.OnShowThisMonthClick -> _uiState.update { it.copy(isCurrentMonth = true) }
            MonthlyViewUiAction.OnToggleCalendarClick -> _uiState.update {
                it.copy(
                    calendarMode = when (it.calendarMode) {
                        MonthlyCalendarMode.SECONDARY -> MonthlyCalendarMode.LUNAR
                        MonthlyCalendarMode.LUNAR -> MonthlyCalendarMode.SECONDARY
                    },
                )
            }
        }
    }

    private fun placeholderRows(): List<MonthlyDayRow> =
        (1..31).map { MonthlyDayRow(it, "03:59", "03:59", "03:59", "03:59", "03:59", isToday = it == 15) }
}
