package com.github.meypod.al_azan.core.data.repository.old

import com.github.meypod.al_azan.core.data.model.old.OldCalculationSettings
import com.github.meypod.al_azan.core.data.model.old.OldFavoriteLocationsStore
import com.github.meypod.al_azan.core.data.model.old.toFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class OldFavoriteLocationsRepositoryImpl(
    oldFavoriteLocationsDatastore: MMKVDataStore<OldFavoriteLocationsStore>,
    oldCalcSettingsDatastore: MMKVDataStore<OldCalculationSettings>,
) : FavoriteLocationsRepository {
    override val data: Flow<List<FavoriteLocation>> =
        combine(oldCalcSettingsDatastore.data, oldFavoriteLocationsDatastore.data) { calcSettings, favoriteLocationsStore ->
            val oldDefault = listOf(calcSettings.state.location?.toFavoriteLocation())
            (oldDefault + favoriteLocationsStore.state.locations.map { it.toFavoriteLocation() }).filterNotNull()
        }

    override suspend fun fetch(): List<FavoriteLocation> = data.first()

    override suspend fun update(transform: (t: List<FavoriteLocation>) -> List<FavoriteLocation>): Unit =
        throw RuntimeException("Unsupported operation")
}
