package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.favorite_locations.FavoriteLocationsStore
import kotlinx.coroutines.flow.Flow

interface FavoriteLocationsRepository {
  val data: Flow<FavoriteLocationsStore>

  suspend fun fetch(): FavoriteLocationsStore

  suspend fun update(transform: suspend (t: FavoriteLocationsStore) -> FavoriteLocationsStore)
}
