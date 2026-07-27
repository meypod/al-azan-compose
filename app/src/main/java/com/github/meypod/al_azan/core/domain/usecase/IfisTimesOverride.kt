package com.github.meypod.al_azan.core.domain.usecase

import com.github.meypod.al_azan.core.data.network.SwedishDownloader
import com.github.meypod.al_azan.core.domain.model.adhan.ShariaTimes
import com.github.meypod.al_azan.core.domain.util.addDaysTimeZoneAware
import com.github.meypod.al_azan.core.domain.util.toLocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

object IfisTimesOverride {
    fun applyOverride(shariaTimes: ShariaTimes, swedishCityId: String, instant: Instant): ShariaTimes {
        val localDate = instant.toLocalDate()
        @Suppress("DEPRECATION")
        val month = localDate.monthNumber
        @Suppress("DEPRECATION")
        val day = localDate.dayOfMonth
        val year = localDate.year

        val swedishData = SwedishDownloader.getPrayerTimesSync(swedishCityId, month, year)
        if (swedishData != null) {
            val todayData = swedishData.days.find { it.day == day }
            if (todayData != null) {
                fun parseTime(timeStr: String, datePrefix: String): Instant {
                    val localDateTime = kotlinx.datetime.LocalDateTime.parse("$datePrefix$timeStr:00")
                    val timeZone = TimeZone.of("Europe/Stockholm")
                    val kotlinxInstant = localDateTime.toInstant(timeZone)
                    return Instant.fromEpochMilliseconds(kotlinxInstant.toEpochMilliseconds())
                }

                val dayStartStr = "${year}-${String.format("%02d", month)}-${String.format("%02d", day)}T"
                val ifisMaghrib = parseTime(todayData.maghrib, dayStartStr)

                // Fetch tomorrow's IFIS Fajr for correct night duration
                var tomorrowFajr = shariaTimes.fajr + 24.hours // fallback if missing
                val tomorrowInstant = addDaysTimeZoneAware(instant, 1)
                val tomorrowDate = tomorrowInstant.toLocalDate()
                val tomorrowMonth = tomorrowDate.monthNumber
                val tomorrowDay = tomorrowDate.dayOfMonth
                val tomorrowYear = tomorrowDate.year

                val tomorrowSwedishData = SwedishDownloader.getPrayerTimesSync(swedishCityId, tomorrowMonth, tomorrowYear)
                if (tomorrowSwedishData != null) {
                    val tomorrowData = tomorrowSwedishData.days.find { it.day == tomorrowDay }
                    if (tomorrowData != null) {
                        val tDayStartStr = "${tomorrowYear}-${String.format("%02d", tomorrowMonth)}-${String.format("%02d", tomorrowDay)}T"
                        tomorrowFajr = parseTime(tomorrowData.fajr, tDayStartStr)
                    }
                }

                val nightDuration = tomorrowFajr - ifisMaghrib
                val ifisMidnight = ifisMaghrib + (nightDuration / 2)
                val ifisTahajjud = ifisMaghrib + (nightDuration * 2 / 3)

                return shariaTimes.copy(
                    fajr = parseTime(todayData.fajr, dayStartStr),
                    sunrise = parseTime(todayData.sunrise, dayStartStr),
                    dhuhr = parseTime(todayData.dhuhr, dayStartStr),
                    asr = parseTime(todayData.asr, dayStartStr),
                    maghrib = ifisMaghrib,
                    isha = parseTime(todayData.isha, dayStartStr),
                    midnight = ifisMidnight,
                    tahajjud = ifisTahajjud,
                )
            }
        }
        return shariaTimes
    }
}
