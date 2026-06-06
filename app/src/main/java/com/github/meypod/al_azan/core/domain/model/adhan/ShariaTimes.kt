package com.github.meypod.al_azan.core.domain.model.adhan

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.alarm.AlarmSettings
import io.github.meypod.adhan_kotlin.PrayerTimes
import io.github.meypod.adhan_kotlin.SunnahTimes
import io.github.meypod.adhan_kotlin.data.DateComponents
import kotlin.time.Instant

@Immutable
data class ShariaTimes(
    val forInstant: Instant,
    val forDate: DateComponents,
    val fajr: Instant,
    val sunrise: Instant,
    val dhuhr: Instant,
    val asr: Instant,
    val sunset: Instant,
    val maghrib: Instant,
    val isha: Instant,
    val midnight: Instant,
    val tahajjud: Instant,
) {
    fun forPrayer(prayer: Prayer): Instant =
        when (prayer) {
            Prayer.Fajr -> fajr
            Prayer.Sunrise -> sunrise
            Prayer.Dhuhr -> dhuhr
            Prayer.Asr -> asr
            Prayer.Sunset -> sunset
            Prayer.Maghrib -> maghrib
            Prayer.Isha -> isha
            Prayer.Midnight -> midnight
            Prayer.Tahajjud -> tahajjud
        }

    fun nextPrayer(
        instant: Instant,
        excluding: Set<Prayer> = emptySet(),
    ): Prayer? =
        SHARIA_TIMES_IN_ORDER.firstOrNull {
            it !in excluding && instant <= forPrayer(it)
        }

    fun currentPrayer(
        instant: Instant,
        excluding: Set<Prayer> = emptySet(),
    ): Prayer? =
        SHARIA_TIMES_IN_ORDER.lastOrNull {
            it !in excluding && instant >= forPrayer(it)
        }

    fun nextPrayerForAlarm(
        instant: Instant,
        alarmSettings: AlarmSettings?,
        excluding: Set<Prayer> = emptySet(),
    ): Prayer? =
        SHARIA_TIMES_IN_ORDER.firstOrNull {
            if (it in excluding) return@firstOrNull false
            val should = alarmSettings?.shouldNotifyFor(instant, it) ?: true
            if (!should) return@firstOrNull false
            instant <= forPrayer(it)
        }

    companion object {
        fun from(
            forInstant: Instant,
            prayerTimes: PrayerTimes,
            sunnahTimes: SunnahTimes,
        ) = ShariaTimes(
            forInstant = forInstant,
            forDate = prayerTimes.dateComponents,
            fajr = prayerTimes.fajr,
            sunrise = prayerTimes.sunrise,
            dhuhr = prayerTimes.dhuhr,
            asr = prayerTimes.asr,
            sunset = prayerTimes.sunset,
            maghrib = prayerTimes.maghrib,
            isha = prayerTimes.isha,
            midnight = sunnahTimes.middleOfTheNight,
            tahajjud = sunnahTimes.lastThirdOfTheNight,
        )
    }
}
