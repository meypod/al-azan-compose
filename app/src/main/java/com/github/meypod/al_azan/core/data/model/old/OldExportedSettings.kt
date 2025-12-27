package com.github.meypod.al_azan.core.data.model.old

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OldExportedSettings(
    @SerialName("SETTINGS_STORAGE")
    var settingsStorage: OldSettings,
    @SerialName("CALC_SETTINGS_STORAGE")
    var calcSettingsStorage: OldCalculationSettings,
    @SerialName("ALARM_SETTINGS_STORAGE")
    var alarmSettingsStorage: OldAlarmSettings,
    @SerialName("COUNTER_STORAGE")
    var counterSettingsStorage: OldCounterSettings,
    @SerialName("REMINDER_STORAGE")
    var reminderSettingsStorage: OldReminderSettings,
    @SerialName("FAVORITE_LOCATIONS_STORAGE")
    var favoriteLocationsSettingsStorage: OldFavoriteLocationsSettings,
)
