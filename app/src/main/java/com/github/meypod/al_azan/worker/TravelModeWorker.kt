package com.github.meypod.al_azan.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.TravelingFavoriteLocation
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.util.android.LocationUtils
import com.github.meypod.al_azan.core.util.lang.ListUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

const val TRAVEL_MODE_WORK_NAME = "travel_mode_worker"

@HiltWorker
class TravelModeWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!LocationUtils.isLocationEnabled(applicationContext)) {
            // TODO let user know
        } else if (
            applicationContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO let user know
        } else {
            val newLocation = LocationUtils.requestCurrentLocation(applicationContext, 60_000).getOrNull()
            if (newLocation != null) {
                favoriteLocationsRepository.update { list ->
                    val newTravelLocation = TravelingFavoriteLocation(
                        CalculationLocationDetail(
                            lat = newLocation.latitude,
                            long = newLocation.longitude,
                        ),
                    )
                    val travelLocationIndex = list.indexOfFirst { it is TravelingFavoriteLocation }
                    if (travelLocationIndex != -1) {
                        ListUtils.replaceInImmutableList(list, travelLocationIndex, newTravelLocation)
                    } else {
                        listOf(newTravelLocation) + list
                    }
                }
                return Result.success()
            }
        }

        return Result.failure()
    }
}
