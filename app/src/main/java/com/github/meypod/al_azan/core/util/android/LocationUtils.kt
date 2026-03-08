package com.github.meypod.al_azan.core.util.android

import android.Manifest.permission
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.provider.Settings
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object LocationUtils {

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = getLocationManager(context)
        return if (locationManager != null) {
            LocationManagerCompat.isLocationEnabled(locationManager)
        } else {
            false
        }
    }

    suspend fun requestCurrentLocation(
        context: Context,
        timeoutMillis: Long = 12_000,
    ): Result<Location> {
        val canAccessCoarseLocation = ActivityCompat.checkSelfPermission(
            context,
            permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!canAccessCoarseLocation) return Result.failure(SecurityException("ACCESS_COARSE_LOCATION permission not granted"))

        return withContext(Dispatchers.IO) {
            val locationManager =
                getLocationManager(context) ?: return@withContext Result.failure(SecurityException("LocationManager not available"))

            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (lastKnown != null && isLocationValid(lastKnown)) {
                return@withContext Result.success(lastKnown)
            }

            val gpsFlow = createSingleLocationFlow(locationManager, LocationManager.GPS_PROVIDER, timeoutMillis)
            val networkFlow = createSingleLocationFlow(locationManager, LocationManager.NETWORK_PROVIDER, timeoutMillis)
            val delayedNetworkFlow = flow {
                delay(7000)
                emitAll(networkFlow)
            }
            try {
                val result = withTimeout(timeoutMillis) {
                    merge(gpsFlow, delayedNetworkFlow).firstOrNull()
                }
                if (result == null) return@withContext Result.failure(Exception("Location unavailable"))

                return@withContext Result.success(result)
            } catch (e: TimeoutCancellationException) {
                return@withContext Result.failure(e)
            }
        }
    }

    private fun isLocationValid(location: Location): Boolean {
        val now = System.currentTimeMillis()
        val age = (now - location.time).toDuration(DurationUnit.MILLISECONDS)

        // If location is older than 15 minutes or accuracy is terrible (> 500m), ignore it
        return age < 15.toDuration(DurationUnit.MINUTES) && location.accuracy > 0 && location.accuracy < 500f
    }

    @OptIn(FlowPreview::class)
    @RequiresPermission(anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
    private fun createSingleLocationFlow(
        locationManager: LocationManager,
        providerName: String,
        timeoutMillis: Long = 12_000,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Flow<Location> =
        channelFlow {
            val signal = CancellationSignal()

            try {
                // This is the magic call. It requests one shot, not continuous updates.
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    providerName,
                    signal,
                    dispatcher.asExecutor(),
                ) { location ->
                    if (location == null) {
                        cancel(CancellationException("Failed to retrieve location using '$providerName' provider"))
                    } else {
                        if (trySend(location).isFailure) {
                            cancel(CancellationException("Failed to send retrieved location over channel"))
                        }
                    }
                }
            } catch (e: SecurityException) {
                cancel(CancellationException("Location permission was not granted", e))
            } catch (e: Exception) {
                cancel(CancellationException("Unexpected error when retrieving location", e))
            }
            delay(timeoutMillis)
            signal.cancel()
            cancel()
        }

    private fun getLocationManager(context: Context): LocationManager? = context.getSystemService(LOCATION_SERVICE) as LocationManager?
}
