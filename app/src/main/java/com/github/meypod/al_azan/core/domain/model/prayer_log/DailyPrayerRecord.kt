package com.github.meypod.al_azan.core.domain.model.prayer_log

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** The five daily obligatory prayers tracked in the prayer log and check-in flow. */
val LOGGABLE_DAILY_PRAYERS: List<Prayer> = listOf(
    Prayer.Fajr,
    Prayer.Dhuhr,
    Prayer.Asr,
    Prayer.Maghrib,
    Prayer.Isha,
)

/** Prayer completion statuses for a single local calendar day. */
@Serializable
data class DailyPrayerRecord(
    val date: LocalDate,
    val statuses: Map<Prayer, PrayerRecordStatus> = emptyMap(),
) {
    fun statusFor(prayer: Prayer): PrayerRecordStatus? = statuses[prayer]

    fun withStatus(
        prayer: Prayer,
        status: PrayerRecordStatus?,
    ): DailyPrayerRecord {
        val updated = if (status == null) statuses - prayer else statuses + (prayer to status)
        return copy(statuses = updated)
    }

    fun prayedCount(): Int = LOGGABLE_DAILY_PRAYERS.count { statuses[it] == PrayerRecordStatus.Prayed }

    fun missedCount(): Int = LOGGABLE_DAILY_PRAYERS.count { statuses[it] == PrayerRecordStatus.Missed }

    fun isAllPrayed(): Boolean = LOGGABLE_DAILY_PRAYERS.all { statuses[it] == PrayerRecordStatus.Prayed }
}
