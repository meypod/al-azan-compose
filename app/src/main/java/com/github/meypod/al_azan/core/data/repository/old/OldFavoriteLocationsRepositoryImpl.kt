package com.github.meypod.al_azan.core.data.repository.old

import com.github.meypod.al_azan.core.data.model.old.OldFavoriteLocationsStore
import com.github.meypod.al_azan.core.data.model.old.toFavoriteLocation
import com.github.meypod.al_azan.core.domain.model.favorite_locations.FavoriteLocationsStore
import com.github.meypod.al_azan.core.domain.repository.FavoriteLocationsRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OldFavoriteLocationsRepositoryImpl(
    private val oldFavoriteLocationsDatastore: MMKVDataStore<OldFavoriteLocationsStore>
) : FavoriteLocationsRepository {
  override val data: Flow<FavoriteLocationsStore> =
      oldFavoriteLocationsDatastore.data.map { store ->
        FavoriteLocationsStore(
            locations = store.state.locations.map { it.toFavoriteLocation() },
        )
      }

  override suspend fun fetch(): FavoriteLocationsStore {
    return data.first()
  }

  override suspend fun update(transform: suspend (t: FavoriteLocationsStore) -> FavoriteLocationsStore) {
    throw RuntimeException("Unsupported operation")
  }
}
