package com.github.meypod.al_azan.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.TextResource
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.TravelingFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.notification.AndroidNotificationConfig
import com.github.meypod.al_azan.core.domain.model.notification.NotificationConfig
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.NotificationRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase.Companion.PERMISSION_REVOKED_CHANNEL_ID
import com.github.meypod.al_azan.core.domain.usecase.EnsureNotificationChannelsUseCase.Companion.TRAVEL_MODE_CHANNEL_ID
import com.github.meypod.al_azan.core.util.android.LocationUtils
import com.github.meypod.al_azan.core.util.lang.ListUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Clock

const val TRAVEL_MODE_WORK_NAME = "travel_mode_worker"

@HiltWorker
class TravelModeWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!LocationUtils.isLocationEnabled(applicationContext)) {
            notificationRepository.notify(
                NotificationConfig(
                    title = TextResource.StringResId(R.string.location_is_disabled_notif_title),
                    body = TextResource.StringResId(R.string.location_is_disabled_notif_body),
                    android = AndroidNotificationConfig(
                        channelId = TRAVEL_MODE_CHANNEL_ID,
                        onlyAlertOnce = true,
                    ),
                ),
            )
        } else if (
            applicationContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationRepository.notify(
                NotificationConfig(
                    title = TextResource.StringResId(R.string.worker_location_permission_revoked_title),
                    body = TextResource.StringResId(R.string.worker_location_permission_revoked_body),
                    android = AndroidNotificationConfig(
                        channelId = TRAVEL_MODE_CHANNEL_ID,
                        onlyAlertOnce = true,
                    ),
                ),
            )
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
                settingsRepository.update {
                    it.copy(travelModeLastUpdateMillis = Clock.System.now().toEpochMilliseconds())
                }
                return Result.success()
            }
        }

        return Result.failure()
    }
}
