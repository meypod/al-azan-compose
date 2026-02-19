package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FavoriteLocationsRepositoryImpl(
    private val favoriteLocationsDatastore: MMKVDataStore<List<FavoriteLocation>>,
) : FavoriteLocationsRepository {
    override val data: Flow<List<FavoriteLocation>> =
        favoriteLocationsDatastore.data

    override suspend fun fetch(): List<FavoriteLocation> = data.first()

    override suspend fun update(transform: (t: List<FavoriteLocation>) -> List<FavoriteLocation>) {
        favoriteLocationsDatastore.update(transform)
    }
}
