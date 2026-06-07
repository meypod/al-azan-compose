package com.github.meypod.al_azan.core.domain.util

import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Instant

/**
 * Instrumented (real ICU) tests for [isRamadanNoticeDue]. Anchors are built from the same Hijri
 * calendar the function reads, so the assertions hold regardless of which islamic-* variant ICU
 * defaults to on the device.
 */
@RunWith(AndroidJUnit4::class)
class RamadanNoticeTest {

    private val calendar = "islamic"

    // ICU islamic months are 0-based: Rajab=6, Sha'ban=7, Ramadan=8, Shawwal=9.
    private fun hijri(
        month: Int,
        day: Int,
    ): Instant {
        val cal = Calendar.getInstance(TimeZone.getDefault(), ULocale("@calendar=$calendar"))
        cal.clear()
        cal.set(Calendar.YEAR, 1445)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        return Instant.fromEpochMilliseconds(cal.timeInMillis)
    }

    @Test
    fun lateRamadan_isDue() {
        assertTrue(isRamadanNoticeDue(hijri(month = 8, day = 25), calendar))
    }

    @Test
    fun lateShaaban_isDue() {
        assertTrue(isRamadanNoticeDue(hijri(month = 7, day = 25), calendar))
    }

    @Test
    fun earlyRamadan_isNotDue() {
        assertFalse(isRamadanNoticeDue(hijri(month = 8, day = 10), calendar))
    }

    @Test
    fun lateRajab_isNotDue() {
        assertFalse(isRamadanNoticeDue(hijri(month = 6, day = 25), calendar))
    }

    @Test
    fun lateShawwal_isNotDue() {
        assertFalse(isRamadanNoticeDue(hijri(month = 9, day = 25), calendar))
    }
}
