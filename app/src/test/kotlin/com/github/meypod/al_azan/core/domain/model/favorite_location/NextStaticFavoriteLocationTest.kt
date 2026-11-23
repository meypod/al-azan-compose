package com.github.meypod.al_azan.core.domain.model.favorite_location

import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextStaticFavoriteLocationTest {

    private val istanbul = StaticFavoriteLocation(
        id = "loc_istanbul",
        locationDetail = CalculationLocationDetail(lat = 41.0082, long = 28.9784, label = "Istanbul"),
    )
    private val ankara = StaticFavoriteLocation(
        id = "loc_ankara",
        locationDetail = CalculationLocationDetail(lat = 39.9334, long = 32.8597, label = "Ankara"),
    )
    private val izmir = StaticFavoriteLocation(
        id = "loc_izmir",
        locationDetail = CalculationLocationDetail(lat = 38.4237, long = 27.1428, label = "Izmir"),
    )
    private val travel = TravelingFavoriteLocation(
        locationDetail = CalculationLocationDetail(lat = 40.0, long = 30.0),
    )

    @Test
    fun `returns null when fewer than 2 static locations exist even if travel location is present`() {
        assertNull(nextStaticFavoriteLocation(emptyList(), currentLocationId = null))
        assertNull(nextStaticFavoriteLocation(listOf(istanbul), currentLocationId = istanbul.id))
        assertNull(nextStaticFavoriteLocation(listOf(travel, istanbul), currentLocationId = istanbul.id))
    }

    @Test
    fun `rotates sequentially through static locations and wraps around`() {
        val list = listOf(istanbul, ankara, izmir)
        assertEquals(ankara, nextStaticFavoriteLocation(list, currentLocationId = istanbul.id))
        assertEquals(izmir, nextStaticFavoriteLocation(list, currentLocationId = ankara.id))
        assertEquals(istanbul, nextStaticFavoriteLocation(list, currentLocationId = izmir.id))
    }

    @Test
    fun `skips TravelingFavoriteLocation when it exists in the locations list`() {
        val list = listOf(travel, istanbul, ankara)
        assertEquals(ankara, nextStaticFavoriteLocation(list, currentLocationId = istanbul.id))
        assertEquals(istanbul, nextStaticFavoriteLocation(list, currentLocationId = ankara.id))
    }

    @Test
    fun `switches to next static city when travel mode is currently selected`() {
        val list = listOf(travel, istanbul, ankara, izmir)
        assertEquals(
            ankara,
            nextStaticFavoriteLocation(
                locations = list,
                currentLocationId = TravelingFavoriteLocation.LOCATION_ID,
                locationIdBeforeTravel = istanbul.id,
            ),
        )
        assertEquals(
            istanbul,
            nextStaticFavoriteLocation(
                locations = list,
                currentLocationId = TravelingFavoriteLocation.LOCATION_ID,
                locationIdBeforeTravel = null,
            ),
        )
    }
}
