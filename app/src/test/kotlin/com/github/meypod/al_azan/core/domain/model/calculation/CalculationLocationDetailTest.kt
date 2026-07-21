package com.github.meypod.al_azan.core.domain.model.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculationLocationDetailTest {

    @Test
    fun `valid coordinates pass through unchanged`() {
        val coords = CalculationLocationDetail(lat = 35.6895, long = 139.6917).toCoordinates()
        assertEquals(35.6895, coords.latitude, 0.0)
        assertEquals(139.6917, coords.longitude, 0.0)
    }

    @Test
    fun `out-of-range coordinates are clamped instead of crashing`() {
        // A swapped/mistyped favorite (e.g. longitude 200) used to throw in Coordinates' bounds
        // check and crash the background scheduler; it must clamp to the nearest valid bound now.
        val tooHigh = CalculationLocationDetail(lat = 120.0, long = 200.0).toCoordinates()
        assertEquals(90.0, tooHigh.latitude, 0.0)
        assertEquals(180.0, tooHigh.longitude, 0.0)

        val tooLow = CalculationLocationDetail(lat = -120.0, long = -200.0).toCoordinates()
        assertEquals(-90.0, tooLow.latitude, 0.0)
        assertEquals(-180.0, tooLow.longitude, 0.0)
    }

    @Test
    fun `non-finite coordinates fall back to zero`() {
        val coords = CalculationLocationDetail(lat = Double.NaN, long = Double.POSITIVE_INFINITY).toCoordinates()
        assertEquals(0.0, coords.latitude, 0.0)
        assertEquals(0.0, coords.longitude, 0.0)
    }

    @Test
    fun `hasValidCoordinates reflects geographic bounds`() {
        assertTrue(CalculationLocationDetail(lat = 0.0, long = 0.0).hasValidCoordinates)
        assertTrue(CalculationLocationDetail(lat = -90.0, long = 180.0).hasValidCoordinates)
        assertFalse(CalculationLocationDetail(lat = 90.1, long = 0.0).hasValidCoordinates)
        assertFalse(CalculationLocationDetail(lat = 0.0, long = -180.1).hasValidCoordinates)
        assertFalse(CalculationLocationDetail(lat = Double.NaN, long = 0.0).hasValidCoordinates)
    }
}
