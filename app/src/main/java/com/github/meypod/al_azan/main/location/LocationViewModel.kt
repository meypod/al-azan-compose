package com.github.meypod.al_azan.main.location

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.meypod.al_azan.core.domain.model.calculation.CalculationLocationDetail
import com.github.meypod.al_azan.core.domain.model.favorite_location.StaticFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.favorite_location.TravelingFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.domain.repository.CalculationSettingsRepository
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.domain.repository.GeoInfoRepository
import com.github.meypod.al_azan.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel
@Inject constructor(
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val calculationSettingsRepository: CalculationSettingsRepository,
    private val settingsRepository: SettingsRepository,
    private val geoInfoRepository: GeoInfoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(calculationSettingsRepository.data, favoriteLocationsRepository.data) { calcSettings, locations ->
                _uiState.update { state ->
                    state.copy(
                        locations = locations,
                        selectedLocationId = calcSettings.locationId,
                        travelMode =
                            locations.firstOrNull { loc -> loc is TravelingFavoriteLocation }?.id?.let { it == calcSettings.locationId } ==
                                true,
                    )
                }
            }.collect()
        }
    }

    fun onAction(action: LocationUiAction) {
        when (action) {
            is LocationUiAction.OnNewLocationClick -> onNewLocationClick()
            is LocationUiAction.OnNewLocationDismiss -> onNewLocationDismiss()
            is LocationUiAction.OnNewLocationConfirm -> onNewLocationConfirm(action.state)
            is LocationUiAction.OnNewLocationFindLocationClick -> onNewLocationFindLocationClick()
            is LocationUiAction.OnMoveLocation -> onMoveLocation(action.fromIndex, action.toIndex)
            is LocationUiAction.OnSetAsDefaultClick -> onSetAsDefault(action.locationId)
            is LocationUiAction.OnDeleteLocationClick -> onDeleteLocation(action.locationId)
            is LocationUiAction.OnDeleteLocationDismiss -> onDeleteLocationDismiss()
            is LocationUiAction.OnDeleteLocationConfirm -> onDeleteLocationConfirm(action.locationId)
            is LocationUiAction.OnTravelModeChange -> onTravelModeChange(action.value)
        }
    }

    suspend fun getCountries(): List<CountryGeoInfo> = geoInfoRepository.getCountries()
    suspend fun getCities(countryCode: String): List<CityGeoInfo> = geoInfoRepository.getCities(countryCode)

    private fun onMoveLocation(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (fromIndex == toIndex) return

        viewModelScope.launch {
            favoriteLocationsRepository.update { current ->
                if (fromIndex !in current.indices || toIndex !in current.indices) return@update current
                val mutable = current.toMutableList()
                val item = mutable.removeAt(fromIndex)
                mutable.add(toIndex, item)
                mutable
            }
        }
    }

    private fun onNewLocationClick() {
        _uiState.update { state ->
            state.copy(isNewLocationDialogOpen = true)
        }
    }

    private fun onNewLocationDismiss() {
        _uiState.update { state ->
            state.copy(isNewLocationDialogOpen = false)
        }
    }

    private fun onNewLocationConfirm(state: NewLocationDialogUiState) {
        _uiState.update { it.copy(isNewLocationDialogOpen = false) }
        val parsedLat = state.latitude.toDoubleOrNull() ?: return
        val parsedLong = state.longitude.toDoubleOrNull() ?: return
        viewModelScope.launch {
            val newLocationId = SystemClock.elapsedRealtime().toString() + "$parsedLat"
            favoriteLocationsRepository.update {
                it + StaticFavoriteLocation(
                    id = newLocationId,
                    locationDetail = CalculationLocationDetail(
                        lat = parsedLat,
                        long = parsedLong,
                        city = state.selectedCity,
                        country = state.selectedCountry,
                        label = state.label,
                    ),
                )
            }
            if (calculationSettingsRepository.fetch().locationId == null) {
                calculationSettingsRepository.update { it.copy(locationId = newLocationId) }
            }
        }
    }

    private fun onNewLocationFindLocationClick() {
        // todo
    }

    private fun onSetAsDefault(locationId: String) {
        viewModelScope.launch {
            calculationSettingsRepository.update { it.copy(locationId = locationId) }
        }
    }

    private fun onDeleteLocation(locationId: String) {
        _uiState.update { state ->
            val deleting = state.locations.firstOrNull { it.id == locationId } ?: return@update state
            state.copy(deleteLocationDialogLocation = deleting)
        }
    }

    private fun onDeleteLocationDismiss() {
        _uiState.update { it.copy(deleteLocationDialogLocation = null) }
    }

    private fun onDeleteLocationConfirm(locationId: String) {
        _uiState.update { it.copy(deleteLocationDialogLocation = null) }
        viewModelScope.launch {
            if (locationId == calculationSettingsRepository.fetch().locationId) {
                val nextLocationId = favoriteLocationsRepository.fetch().firstOrNull {
                    it !is TravelingFavoriteLocation &&
                        it.id != locationId
                }?.id
                calculationSettingsRepository.update {
                    it.copy(
                        locationId = nextLocationId,
                    )
                }
            }
            if (settingsRepository.fetch().locationIdBeforeTravel == locationId) {
                settingsRepository.update { it.copy(locationIdBeforeTravel = null) }
            }
            favoriteLocationsRepository.update { current ->
                current.filterNot { it.id == locationId }
            }
        }
    }

    private fun onTravelModeChange(value: Boolean) {
        viewModelScope.launch {
            favoriteLocationsRepository.update { locations ->
                if (value && locations.indexOfFirst { it is TravelingFavoriteLocation } == -1) {
                    // we just need to make sure to add it to the beginning of locations list once
                    // then we will hide it from user onwards and show it only when travel mode is enabled
                    listOf(TravelingFavoriteLocation(CalculationLocationDetail(0.0, 0.0))) + locations
                } else {
                    locations
                }
            }
            val nextLocationId = if (value) {
                val currentLocationId = calculationSettingsRepository.fetch().locationId
                if (currentLocationId != null) {
                    settingsRepository.update { it.copy(locationIdBeforeTravel = currentLocationId) }
                }
                TravelingFavoriteLocation.LOCATION_ID
            } else {
                settingsRepository.fetch().locationIdBeforeTravel ?: (
                    favoriteLocationsRepository.fetch()
                        .firstOrNull { it is StaticFavoriteLocation }?.id
                    )
            }
            calculationSettingsRepository.update { calcSettings ->
                calcSettings.copy(locationId = nextLocationId)
            }
        }
    }
}
