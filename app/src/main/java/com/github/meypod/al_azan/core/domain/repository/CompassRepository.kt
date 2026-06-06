package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.compass.CompassReading
import kotlinx.coroutines.flow.Flow

interface CompassRepository {

    /**
     * Emits compass readings while collected; registers the sensor on subscribe and unregisters on
     * cancellation.
     *
     * When [latitude]/[longitude] are provided, headings are corrected to true north using the
     * magnetic declination at that location; otherwise raw magnetic headings are emitted.
     */
    fun headings(
        latitude: Double? = null,
        longitude: Double? = null,
    ): Flow<CompassReading>
}
