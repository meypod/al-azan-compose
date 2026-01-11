package com.github.meypod.al_azan.core.data.repository.old

import com.github.meypod.al_azan.core.data.model.old.OldCounterStore
import com.github.meypod.al_azan.core.data.model.old.toCounter
import com.github.meypod.al_azan.core.domain.model.counter.CounterStore
import com.github.meypod.al_azan.core.domain.repository.CounterStoreRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OldCounterStoreRepositoryImpl(
    private val oldCounterStoreDatastore: MMKVDataStore<OldCounterStore>
) : CounterStoreRepository {
  override val data: Flow<CounterStore> =
      oldCounterStoreDatastore.data.map {
        CounterStore(
            counters = it.state.counters.map { it.toCounter() },
        )
      }

  override suspend fun fetch(): CounterStore {
    return data.first()
  }

  override suspend fun update(transform: suspend (t: CounterStore) -> CounterStore) {
    throw RuntimeException("Unsupported operation")
  }
}

