package com.github.meypod.al_azan.core.data.repository.old

import com.github.meypod.al_azan.core.data.model.old.OldFavoriteLocationsStore
import com.github.meypod.al_azan.core.data.model.old.toFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OldFavoriteLocationsRepositoryImpl(
    oldFavoriteLocationsDatastore: MMKVDataStore<OldFavoriteLocationsStore>
) : FavoriteLocationsRepository {
  override val data: Flow<List<FavoriteLocation>> =
      oldFavoriteLocationsDatastore.data.map { store ->
        store.state.locations.map { it.toFavoriteLocation() }
      }

  override suspend fun fetch(): List<FavoriteLocation> {
    return data.first()
  }

  override suspend fun update(transform: suspend (t: List<FavoriteLocation>) -> List<FavoriteLocation>) {
    throw RuntimeException("Unsupported operation")
  }
}
