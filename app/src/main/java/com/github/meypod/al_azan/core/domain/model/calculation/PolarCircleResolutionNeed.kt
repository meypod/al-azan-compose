package com.github.meypod.al_azan.core.domain.model.calculation

import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.HighLatitudeRule
import io.github.meypod.adhan_kotlin.PolarCircleResolution
import kotlin.math.abs

/** Poleward of this the sun can stay below or above the horizon all day: 90 degrees minus Earth's axial tilt. */
private const val POLAR_CIRCLE_LATITUDE = 66.56

/**
 * Whether part of the year at this location has no prayer times under [parameters], so the user
 * should be told before those days arrive rather than finding blank times when they do.
 *
 * Without a sunrise and a sunset there is nothing to derive the day's times from; only a
 * [PolarCircleResolution] or the takdir rule ([HighLatitudeRule.PROPORTIONAL_DEPRESSION], which
 * floors the day at five hours) defines them anyway.
 */
fun CalculationLocationDetail.needsPolarCircleResolution(parameters: CalculationParameters): Boolean =
    abs(lat) >= POLAR_CIRCLE_LATITUDE &&
        parameters.polarCircleResolution == PolarCircleResolution.Unresolved &&
        parameters.effectiveHighLatitudeRule(toCoordinates()) != HighLatitudeRule.PROPORTIONAL_DEPRESSION
