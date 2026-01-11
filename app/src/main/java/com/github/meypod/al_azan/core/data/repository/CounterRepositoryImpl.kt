package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.counter.CounterStore
import com.github.meypod.al_azan.core.domain.repository.CounterStoreRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CounterRepositoryImpl(
    private val counterStoreDatastore: MMKVDataStore<CounterStore>
) : CounterStoreRepository {
  override val data: Flow<CounterStore> =
      counterStoreDatastore.data

  override suspend fun fetch(): CounterStore {
    return data.first()
  }

  override suspend fun update(transform: suspend (t: CounterStore) -> CounterStore) {
    counterStoreDatastore.update(transform)
  }
}

