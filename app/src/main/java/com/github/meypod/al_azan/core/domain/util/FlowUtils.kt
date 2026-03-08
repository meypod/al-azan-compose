package com.github.meypod.al_azan.core.domain.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Creates a flow that emits the current time every second.
 *
 * @param initialValue Optional initial value to emit immediately before starting the interval
 * @return Flow<Instant> that emits the current time at regular intervals
 */
fun tickFlow(initialValue: Instant? = Clock.System.now()): Flow<Instant> =
    flow {
        // Emit initial value if provided
        initialValue?.let { emit(it) }

        while (true) {
            val currentTime = Clock.System.now()
            emit(currentTime)
            delay(1000L) // 1-second delay between emissions
        }
    }
