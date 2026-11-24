package com.github.meypod.al_azan.prayer_checkin

import com.github.meypod.al_azan.core.domain.model.counter.COUNTER_ID_ASR
import com.github.meypod.al_azan.core.domain.model.counter.COUNTER_ID_FAJR
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.model.counter.adjustCounter
import com.github.meypod.al_azan.core.domain.model.prayer_log.PrayerRecordStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrayerStatusEditorTest {
    @Test
    fun `entering missed adds one and leaving missed takes one back`() {
        assertEquals(1, qadaDelta(null, PrayerRecordStatus.Missed))
        assertEquals(1, qadaDelta(PrayerRecordStatus.Prayed, PrayerRecordStatus.Missed))
        assertEquals(-1, qadaDelta(PrayerRecordStatus.Missed, PrayerRecordStatus.Prayed))
        assertEquals(-1, qadaDelta(PrayerRecordStatus.Missed, null))
    }

    @Test
    fun `unchanged or non-missed transitions leave qada alone`() {
        assertEquals(0, qadaDelta(PrayerRecordStatus.Missed, PrayerRecordStatus.Missed))
        assertEquals(0, qadaDelta(null, PrayerRecordStatus.Prayed))
        assertEquals(0, qadaDelta(PrayerRecordStatus.Prayed, null))
    }

    @Test
    fun `tap cycle goes pending prayed missed pending`() {
        assertEquals(PrayerRecordStatus.Prayed, nextPrayerStatus(null))
        assertEquals(PrayerRecordStatus.Missed, nextPrayerStatus(PrayerRecordStatus.Prayed))
        assertNull(nextPrayerStatus(PrayerRecordStatus.Missed))
    }

    @Test
    fun `full tap cycle returns qada counter to where it started`() {
        var counters = listOf(Counter(id = COUNTER_ID_ASR, count = 3))
        var status: PrayerRecordStatus? = null
        repeat(3) {
            val next = nextPrayerStatus(status)
            counters = adjustCounter(counters, COUNTER_ID_ASR, qadaDelta(status, next), 1L)
            status = next
        }
        assertNull(status)
        assertEquals(3, counters.first { it.id == COUNTER_ID_ASR }.count)
    }

    @Test
    fun `adjustCounter never goes below zero and only touches the target`() {
        val result = adjustCounter(emptyList(), COUNTER_ID_FAJR, -1, 5L)
        assertEquals(0, result.first { it.id == COUNTER_ID_FAJR }.count)
        assertNull(result.first { it.id == COUNTER_ID_FAJR }.lastModified)

        val up = adjustCounter(result, COUNTER_ID_FAJR, 1, 9L)
        assertEquals(1, up.first { it.id == COUNTER_ID_FAJR }.count)
        assertEquals(9L, up.first { it.id == COUNTER_ID_FAJR }.lastModified)
        assertEquals(0, up.first { it.id == COUNTER_ID_ASR }.count)
    }
}
