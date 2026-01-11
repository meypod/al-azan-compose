package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.favorite_locations.FavoriteLocationsStore
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FavoriteLocationsRepositoryImpl(
    private val favoriteLocationsDatastore: MMKVDataStore<FavoriteLocationsStore>
) : FavoriteLocationsRepository {
  override val data: Flow<FavoriteLocationsStore> =
      favoriteLocationsDatastore.data

  override suspend fun fetch(): FavoriteLocationsStore {
    return data.first()
  }

  override suspend fun update(transform: suspend (t: FavoriteLocationsStore) -> FavoriteLocationsStore) {
    favoriteLocationsDatastore.update(transform)
  }
}
