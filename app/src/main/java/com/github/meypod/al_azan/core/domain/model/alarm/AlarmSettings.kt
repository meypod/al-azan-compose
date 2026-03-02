package com.github.meypod.al_azan.core.domain.model.alarm

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import kotlinx.serialization.Serializable

@Serializable
data class AlarmSettings(
    val showNextPrayerTime: Boolean = false,
    val dontNotifyUpcoming: Boolean = false,
    val preAlarmMinutesBefore: Int = 60,
    val dontTurnOnScreen: Boolean = false,
    val vibrationMode: VibrationMode = VibrationMode.Once,
    //
    val fajrSound: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val fajrNotify: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val sunriseSound: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val sunriseNotify: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val dhuhrSound: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val dhuhrNotify: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val asrSound: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val asrNotify: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val sunsetSound: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val sunsetNotify: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val maghribSound: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val maghribNotify: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val ishaSound: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val ishaNotify: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val midnightSound: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val midnightNotify: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val tahajjudSound: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
    val tahajjudNotify: PrayerAlarmSettings = PrayerAlarmSettings.Bool(false),
) {

    fun getNotifSettings(prayer: Prayer): PrayerAlarmSettings =
        when (prayer) {
            Prayer.Fajr -> this.fajrNotify
            Prayer.Sunrise -> this.sunriseNotify
            Prayer.Dhuhr -> this.dhuhrNotify
            Prayer.Asr -> this.asrNotify
            Prayer.Sunset -> this.sunsetNotify
            Prayer.Maghrib -> this.maghribNotify
            Prayer.Isha -> this.ishaNotify
            Prayer.Midnight -> this.midnightNotify
            Prayer.Tahajjud -> this.tahajjudNotify
        }

    fun getSoundSettings(prayer: Prayer): PrayerAlarmSettings =
        when (prayer) {
            Prayer.Fajr -> this.fajrSound
            Prayer.Sunrise -> this.sunriseSound
            Prayer.Dhuhr -> this.dhuhrSound
            Prayer.Asr -> this.asrSound
            Prayer.Sunset -> this.sunsetSound
            Prayer.Maghrib -> this.maghribSound
            Prayer.Isha -> this.ishaSound
            Prayer.Midnight -> this.midnightSound
            Prayer.Tahajjud -> this.tahajjudSound
        }

    fun setNotifSettings(
        prayer: Prayer,
        value: PrayerAlarmSettings,
    ): AlarmSettings =
        when (prayer) {
            Prayer.Fajr -> copy(fajrNotify = value)
            Prayer.Sunrise -> copy(sunriseNotify = value)
            Prayer.Dhuhr -> copy(dhuhrNotify = value)
            Prayer.Asr -> copy(asrNotify = value)
            Prayer.Sunset -> copy(sunsetNotify = value)
            Prayer.Maghrib -> copy(maghribNotify = value)
            Prayer.Isha -> copy(ishaNotify = value)
            Prayer.Midnight -> copy(midnightNotify = value)
            Prayer.Tahajjud -> copy(tahajjudNotify = value)
        }

    fun setSoundSettings(
        prayer: Prayer,
        value: PrayerAlarmSettings,
    ): AlarmSettings =
        when (prayer) {
            Prayer.Fajr -> copy(fajrSound = value)
            Prayer.Sunrise -> copy(sunriseSound = value)
            Prayer.Dhuhr -> copy(dhuhrSound = value)
            Prayer.Asr -> copy(asrSound = value)
            Prayer.Sunset -> copy(sunsetSound = value)
            Prayer.Maghrib -> copy(maghribSound = value)
            Prayer.Isha -> copy(ishaSound = value)
            Prayer.Midnight -> copy(midnightSound = value)
            Prayer.Tahajjud -> copy(tahajjudSound = value)
        }
}
