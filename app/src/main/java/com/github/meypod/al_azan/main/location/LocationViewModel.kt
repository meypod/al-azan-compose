package com.github.meypod.al_azan.main.location

import androidx.lifecycle.ViewModel
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.domain.repository.GeoInfoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class LocationViewModel
@Inject
constructor(
    private val geoInfoRepository: GeoInfoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState = _uiState.asStateFlow()

    fun onAction(action: LocationUiAction) {
        when (action) {
            is LocationUiAction.OnNewLocationClick -> onNewLocationClick()
            is LocationUiAction.OnNewLocationDismiss -> onNewLocationDismiss()
            is LocationUiAction.OnNewLocationConfirm -> onNewLocationConfirm(action.state)
            is LocationUiAction.OnNewLocationFindLocationClick -> onNewLocationFindLocationClick()
            is LocationUiAction.OnMoveLocation -> onMoveLocation(action.fromIndex, action.toIndex)
            is LocationUiAction.OnSetAsDefaultClick -> onSetAsDefault(action.locationId)
            is LocationUiAction.OnDeleteLocationClick -> onDeleteLocation(action.locationId)
        }
    }

    suspend fun getCountries(): List<CountryGeoInfo> = geoInfoRepository.getCountries()
    suspend fun getCities(countryCode: String): List<CityGeoInfo> = geoInfoRepository.getCities(countryCode)

    private fun onMoveLocation(
        fromIndex: Int,
        toIndex: Int,
    ) {
        if (fromIndex == toIndex) return

        _uiState.update { state ->
            val current = state.locations
            if (fromIndex !in current.indices || toIndex !in current.indices) return@update state

            val mutable = current.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)

            state.copy(locations = mutable)
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
        // todo: persist/validate/use 'state'
    }

    private fun onNewLocationFindLocationClick() {
        // todo
    }

    private fun onSetAsDefault(locationId: String) {
        // todo
    }

    private fun onDeleteLocation(locationId: String) {
        // todo
    }
}
