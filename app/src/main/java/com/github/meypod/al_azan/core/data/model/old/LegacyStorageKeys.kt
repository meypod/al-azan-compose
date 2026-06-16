package com.github.meypod.al_azan.core.data.model.old

/**
 * MMKV keys of the old React-Native app's stores. The first-launch migration reads these but never
 * deletes them, so on an upgraded device they still hold the original data. They are what the legacy
 * backup export dumps verbatim, and their presence is what gates that export being offered at all.
 */
object LegacyStorageKeys {
    const val SETTINGS = "SETTINGS_STORAGE"
    const val CALC_SETTINGS = "CALC_SETTINGS_STORAGE"
    const val ALARM_SETTINGS = "ALARM_SETTINGS_STORAGE"
    const val COUNTER = "COUNTER_STORAGE"
    const val REMINDER = "REMINDER_STORAGE"
    const val FAVORITE_LOCATIONS = "FAVORITE_LOCATIONS_STORAGE"

    /** In the same order the old app's stores were exported. */
    val ALL = listOf(SETTINGS, CALC_SETTINGS, ALARM_SETTINGS, COUNTER, REMINDER, FAVORITE_LOCATIONS)
}
