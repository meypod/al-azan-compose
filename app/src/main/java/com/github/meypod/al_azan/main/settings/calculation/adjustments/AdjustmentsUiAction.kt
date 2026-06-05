package com.github.meypod.al_azan.main.settings.calculation.adjustments

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer

sealed interface AdjustmentsUiAction {
    object OnBackClick : AdjustmentsUiAction
    data class OnPrayerChange(val prayer: Prayer, val delta: Int) : AdjustmentsUiAction
    data class OnPrayerSet(val prayer: Prayer, val value: Int) : AdjustmentsUiAction
    data class OnLunarDayChange(val delta: Int) : AdjustmentsUiAction
    data class OnLunarDaySet(val value: Int) : AdjustmentsUiAction
}
