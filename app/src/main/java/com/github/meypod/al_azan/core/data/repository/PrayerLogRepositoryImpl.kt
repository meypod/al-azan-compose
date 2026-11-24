package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.prayer_log.DailyPrayerRecord
import com.github.meypod.al_azan.core.domain.model.prayer_log.PrayerRecordStatus
import com.github.meypod.al_azan.core.domain.repository.PrayerLogRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate

class PrayerLogRepositoryImpl(
    private val prayerLogDatastore: MMKVDataStore<List<DailyPrayerRecord>>,
) : PrayerLogRepository {

    private val mutex = Mutex()

    override val data: Flow<List<DailyPrayerRecord>> = prayerLogDatastore.data

    override suspend fun fetch(): List<DailyPrayerRecord> = data.first()

    override suspend fun update(transform: (List<DailyPrayerRecord>) -> List<DailyPrayerRecord>) {
        mutex.withLock {
            prayerLogDatastore.update(transform)
        }
    }

    override suspend fun getRecordForDate(date: LocalDate): DailyPrayerRecord? =
        fetch().firstOrNull { it.date == date }

    override suspend fun setPrayerStatus(
        date: LocalDate,
        prayer: Prayer,
        status: PrayerRecordStatus?,
    ) {
        mutex.withLock {
            prayerLogDatastore.update { list ->
                val existing = list.firstOrNull { it.date == date }
                if (existing != null) {
                    val updated = existing.withStatus(prayer, status)
                    if (updated.statuses.isEmpty()) {
                        list.filterNot { it.date == date }
                    } else {
                        list.map { if (it.date == date) updated else it }
                    }
                } else if (status != null) {
                    list + DailyPrayerRecord(date = date).withStatus(prayer, status)
                } else {
                    list
                }
            }
        }
    }

    override suspend fun recordIfUnrecorded(
        date: LocalDate,
        prayer: Prayer,
        status: PrayerRecordStatus,
    ): Boolean =
        mutex.withLock {
            var newlyRecorded = false
            prayerLogDatastore.update { list ->
                val existing = list.firstOrNull { it.date == date }
                if (existing != null) {
                    if (existing.statusFor(prayer) == null) {
                        newlyRecorded = true
                        val updated = existing.withStatus(prayer, status)
                        list.map { if (it.date == date) updated else it }
                    } else {
                        list
                    }
                } else {
                    newlyRecorded = true
                    list + DailyPrayerRecord(date = date).withStatus(prayer, status)
                }
            }
            newlyRecorded
        }
}
