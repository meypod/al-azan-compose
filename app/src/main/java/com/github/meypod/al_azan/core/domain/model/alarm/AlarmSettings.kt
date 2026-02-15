package com.github.meypod.al_azan.core.domain.model.alarm

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
)
