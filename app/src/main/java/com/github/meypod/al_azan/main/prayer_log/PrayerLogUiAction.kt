package com.github.meypod.al_azan.main.prayer_log

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.prayer_log.PrayerRecordStatus
import kotlinx.datetime.LocalDate

sealed interface PrayerLogUiAction {
    object OnPrevDayClick : PrayerLogUiAction
    object OnNextDayClick : PrayerLogUiAction
    object OnTodayClick : PrayerLogUiAction
    data class OnDatePicked(val date: LocalDate) : PrayerLogUiAction
    data class OnSetPrayerStatus(val prayer: Prayer, val status: PrayerRecordStatus?) : PrayerLogUiAction
    data class OnTogglePrayerStatus(val prayer: Prayer) : PrayerLogUiAction
    object OnOpenCheckInSettingsClick : PrayerLogUiAction
}
