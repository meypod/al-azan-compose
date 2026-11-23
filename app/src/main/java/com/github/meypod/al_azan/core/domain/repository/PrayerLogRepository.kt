package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.prayer_log.DailyPrayerRecord
import com.github.meypod.al_azan.core.domain.model.prayer_log.PrayerRecordStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface PrayerLogRepository {
    val data: Flow<List<DailyPrayerRecord>>

    suspend fun fetch(): List<DailyPrayerRecord>

    suspend fun update(transform: (List<DailyPrayerRecord>) -> List<DailyPrayerRecord>)

    suspend fun getRecordForDate(date: LocalDate): DailyPrayerRecord?

    suspend fun setPrayerStatus(
        date: LocalDate,
        prayer: Prayer,
        status: PrayerRecordStatus?,
    )

    /**
     * Records [status] only if no status has been recorded yet for [prayer] on [date].
     * Returns true if a new status was written, false if an entry already existed.
     */
    suspend fun recordIfUnrecorded(
        date: LocalDate,
        prayer: Prayer,
        status: PrayerRecordStatus,
    ): Boolean
}
