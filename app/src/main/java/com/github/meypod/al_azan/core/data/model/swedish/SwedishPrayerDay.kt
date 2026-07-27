package com.github.meypod.al_azan.core.data.model.swedish

import kotlinx.serialization.Serializable

@Serializable
data class SwedishPrayerDay(
    val day: Int,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

@Serializable
data class SwedishPrayerMonth(
    val month: Int,
    val days: List<SwedishPrayerDay>
)
