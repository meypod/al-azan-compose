package com.github.meypod.al_azan.core.data.repository.old

import com.github.meypod.al_azan.core.data.model.old.OldCounterStore
import com.github.meypod.al_azan.core.data.model.old.toCounter
import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class OldCounterRepositoryImpl(
    oldCounterStoreDatastore: MMKVDataStore<OldCounterStore>,
) : CounterRepository {
    override val data: Flow<List<Counter>> =
        oldCounterStoreDatastore.data.map { oldCounterStore ->
            oldCounterStore.state.counters.map {
                it.toCounter()
            }
        }

    override suspend fun fetch(): List<Counter> = data.first()

    override suspend fun update(transform: suspend (t: List<Counter>) -> List<Counter>): Unit =
        throw RuntimeException("Unsupported operation")
}
