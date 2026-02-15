package com.github.meypod.al_azan.core.domain.model.adhan

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Prayer {
    @SerialName("fajr")
    Fajr,

    @SerialName("sunrise")
    Sunrise,

    @SerialName("dhuhr")
    Dhuhr,

    @SerialName("asr")
    Asr,

    @SerialName("sunset")
    Sunset,

    @SerialName("maghrib")
    Maghrib,

    @SerialName("isha")
    Isha,

    /** middle of the night */
    @SerialName("midnight")
    Midnight,

    /** last third of the night */
    @SerialName("tahajjud")
    Tahajjud,
}

val PRAYERS_IN_ORDER =
    listOf<Prayer>(
        Prayer.Fajr,
        Prayer.Sunrise,
        Prayer.Dhuhr,
        Prayer.Asr,
        Prayer.Sunset,
        Prayer.Maghrib,
        Prayer.Isha,
        /** middle of the night */
        Prayer.Midnight,
        /** last third of the night */
        Prayer.Tahajjud,
    )

val NON_PRAYERS_IN_ORDER =
    listOf<Prayer>(
        Prayer.Sunrise,
        Prayer.Sunset,
        Prayer.Midnight,
        Prayer.Tahajjud,
    )
