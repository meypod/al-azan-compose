package com.github.meypod.al_azan.core.domain.model.widget

import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure grid helpers behind the builder's row-count control and per-row drag add/reorder/move. */
class CustomWidgetLayoutOpsTest {

    @Test
    fun `reducing to one row keeps the first row and drops the rest`() {
        val grid = listOf(listOf(Prayer.Fajr, Prayer.Dhuhr), listOf(Prayer.Asr, Prayer.Maghrib))
        // Asr/Maghrib become unplaced again (return to the palette), not merged into row 1.
        assertEquals(listOf(listOf(Prayer.Fajr, Prayer.Dhuhr)), grid.withRowCount(1))
    }

    @Test
    fun `increasing to two rows keeps existing rows and adds an empty one`() {
        val grid = listOf(listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr))
        assertEquals(
            listOf(listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr), emptyList<Prayer>()),
            grid.withRowCount(2),
        )
    }

    @Test
    fun `place appends to the target row`() {
        val grid = listOf(listOf(Prayer.Fajr), listOf(Prayer.Asr))
        assertEquals(
            listOf(listOf(Prayer.Fajr), listOf(Prayer.Asr, Prayer.Dhuhr)),
            grid.withPrayerPlaced(Prayer.Dhuhr, rowIndex = 1),
        )
    }

    @Test
    fun `place inserts before the target prayer within a row`() {
        val grid = listOf(listOf(Prayer.Fajr, Prayer.Asr))
        assertEquals(
            listOf(listOf(Prayer.Fajr, Prayer.Dhuhr, Prayer.Asr)),
            grid.withPrayerPlaced(Prayer.Dhuhr, rowIndex = 0, before = Prayer.Asr),
        )
    }

    @Test
    fun `place moves a prayer between rows, removing it from the old one`() {
        val grid = listOf(listOf(Prayer.Fajr, Prayer.Dhuhr), listOf(Prayer.Asr))
        assertEquals(
            listOf(listOf(Prayer.Dhuhr), listOf(Prayer.Fajr, Prayer.Asr)),
            grid.withPrayerPlaced(Prayer.Fajr, rowIndex = 1, before = Prayer.Asr),
        )
    }

    @Test
    fun `remove drops a prayer from whichever row holds it`() {
        val grid = listOf(listOf(Prayer.Fajr, Prayer.Dhuhr), listOf(Prayer.Asr))
        assertEquals(
            listOf(listOf(Prayer.Fajr), listOf(Prayer.Asr)),
            grid.withPrayerRemoved(Prayer.Dhuhr),
        )
    }
}
