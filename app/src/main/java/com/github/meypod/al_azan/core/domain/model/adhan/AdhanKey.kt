package com.github.meypod.al_azan.core.domain.model.adhan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AdhanKey {
  @SerialName("default") Default,
  @SerialName("fajr") Fajr,
  @SerialName("sunrise") Sunrise,
  @SerialName("dhuhr") Dhuhr,
  @SerialName("asr") Asr,
  @SerialName("sunset") Sunset,
  @SerialName("maghrib") Maghrib,
  @SerialName("isha") Isha,
  @SerialName("midnight") Midnight,
  @SerialName("tahajjud") Tahajjud,
}

fun AdhanKey.toPrayer(): Prayer? = when (this) {
  AdhanKey.Default -> null
  AdhanKey.Fajr -> Prayer.Fajr
  AdhanKey.Sunrise -> Prayer.Sunrise
  AdhanKey.Dhuhr -> Prayer.Dhuhr
  AdhanKey.Asr -> Prayer.Asr
  AdhanKey.Sunset -> Prayer.Sunset
  AdhanKey.Maghrib -> Prayer.Maghrib
  AdhanKey.Isha -> Prayer.Isha
  AdhanKey.Midnight -> Prayer.Midnight
  AdhanKey.Tahajjud -> Prayer.Tahajjud
}

fun Prayer.toAdhanKey(): AdhanKey = when (this) {
  Prayer.Fajr -> AdhanKey.Fajr
  Prayer.Sunrise -> AdhanKey.Sunrise
  Prayer.Dhuhr -> AdhanKey.Dhuhr
  Prayer.Asr -> AdhanKey.Asr
  Prayer.Sunset -> AdhanKey.Sunset
  Prayer.Maghrib -> AdhanKey.Maghrib
  Prayer.Isha -> AdhanKey.Isha
  Prayer.Midnight -> AdhanKey.Midnight
  Prayer.Tahajjud -> AdhanKey.Tahajjud
}
