package com.github.meypod.al_azan.core.domain.model.prayer_log

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyPrayerRecordTest {

    @Test
    fun `default record has empty statuses`() {
        val record = DailyPrayerRecord(date = LocalDate(2026, 11, 23))
        assertEquals(0, record.prayedCount())
        assertEquals(0, record.missedCount())
        assertFalse(record.isAllPrayed())
        assertNull(record.statusFor(Prayer.Fajr))
    }

    @Test
    fun `withStatus updates and clears individual prayer status`() {
        val initial = DailyPrayerRecord(date = LocalDate(2026, 11, 23))

        val prayed = initial.withStatus(Prayer.Fajr, PrayerRecordStatus.Prayed)
        assertEquals(PrayerRecordStatus.Prayed, prayed.statusFor(Prayer.Fajr))
        assertEquals(1, prayed.prayedCount())
        assertEquals(0, prayed.missedCount())

        val missed = prayed.withStatus(Prayer.Dhuhr, PrayerRecordStatus.Missed)
        assertEquals(PrayerRecordStatus.Missed, missed.statusFor(Prayer.Dhuhr))
        assertEquals(1, missed.prayedCount())
        assertEquals(1, missed.missedCount())

        val cleared = missed.withStatus(Prayer.Fajr, null)
        assertNull(cleared.statusFor(Prayer.Fajr))
        assertEquals(0, cleared.prayedCount())
        assertEquals(1, cleared.missedCount())
    }

    @Test
    fun `isAllPrayed is true only when all five obligatory prayers are prayed`() {
        var record = DailyPrayerRecord(date = LocalDate(2026, 11, 23))
        for (prayer in LOGGABLE_DAILY_PRAYERS) {
            record = record.withStatus(prayer, PrayerRecordStatus.Prayed)
        }
        assertEquals(5, record.prayedCount())
        assertTrue(record.isAllPrayed())

        record = record.withStatus(Prayer.Isha, PrayerRecordStatus.Missed)
        assertFalse(record.isAllPrayed())
    }

    @Test
    fun `round trips through kotlinx serialization`() {
        val record = DailyPrayerRecord(
            date = LocalDate(2026, 11, 23),
            statuses = mapOf(
                Prayer.Fajr to PrayerRecordStatus.Prayed,
                Prayer.Dhuhr to PrayerRecordStatus.Missed,
            ),
        )
        val json = Json.encodeToString(DailyPrayerRecord.serializer(), record)
        val decoded = Json.decodeFromString(DailyPrayerRecord.serializer(), json)
        assertEquals(record, decoded)
    }
}
