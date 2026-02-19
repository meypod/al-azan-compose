package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.favorite_location.FavoriteLocation
import kotlinx.coroutines.flow.Flow

interface FavoriteLocationsRepository {
    val data: Flow<List<FavoriteLocation>>

    suspend fun fetch(): List<FavoriteLocation>

    suspend fun update(transform: (t: List<FavoriteLocation>) -> List<FavoriteLocation>)
}
