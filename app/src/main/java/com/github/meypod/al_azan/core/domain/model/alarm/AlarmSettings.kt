package com.github.meypod.al_azan.core.domain.model.alarm

import com.github.meypod.al_azan.core.domain.model.adhan.SHARIA_TIMES_IN_ORDER
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import kotlinx.serialization.Serializable
import kotlin.time.Instant

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
    // per-prayer vibration override; null = fall back to the global [vibrationMode]
    val fajrVibration: VibrationMode? = null,
    val sunriseVibration: VibrationMode? = null,
    val dhuhrVibration: VibrationMode? = null,
    val asrVibration: VibrationMode? = null,
    val sunsetVibration: VibrationMode? = null,
    val maghribVibration: VibrationMode? = null,
    val ishaVibration: VibrationMode? = null,
    val midnightVibration: VibrationMode? = null,
    val tahajjudVibration: VibrationMode? = null,
) {
    fun shouldNotifyFor(
        instant: Instant,
        prayers: List<Prayer>? = null,
    ) = (prayers ?: SHARIA_TIMES_IN_ORDER).any { getNotifSettings(it).shouldFireFor(instant) }

    fun shouldNotifyFor(
        instant: Instant,
        prayer: Prayer,
    ) = getNotifSettings(prayer).shouldFireFor(instant)

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

    /** per-prayer override, or null when the global [vibrationMode] should be used */
    fun getVibrationSettings(prayer: Prayer): VibrationMode? =
        when (prayer) {
            Prayer.Fajr -> this.fajrVibration
            Prayer.Sunrise -> this.sunriseVibration
            Prayer.Dhuhr -> this.dhuhrVibration
            Prayer.Asr -> this.asrVibration
            Prayer.Sunset -> this.sunsetVibration
            Prayer.Maghrib -> this.maghribVibration
            Prayer.Isha -> this.ishaVibration
            Prayer.Midnight -> this.midnightVibration
            Prayer.Tahajjud -> this.tahajjudVibration
        }

    fun setVibrationSettings(
        prayer: Prayer,
        value: VibrationMode?,
    ): AlarmSettings =
        when (prayer) {
            Prayer.Fajr -> copy(fajrVibration = value)
            Prayer.Sunrise -> copy(sunriseVibration = value)
            Prayer.Dhuhr -> copy(dhuhrVibration = value)
            Prayer.Asr -> copy(asrVibration = value)
            Prayer.Sunset -> copy(sunsetVibration = value)
            Prayer.Maghrib -> copy(maghribVibration = value)
            Prayer.Isha -> copy(ishaVibration = value)
            Prayer.Midnight -> copy(midnightVibration = value)
            Prayer.Tahajjud -> copy(tahajjudVibration = value)
        }

    /** effective vibration for a prayer: per-prayer override, else the global default */
    fun getEffectiveVibrationMode(prayer: Prayer): VibrationMode = getVibrationSettings(prayer) ?: vibrationMode
}
