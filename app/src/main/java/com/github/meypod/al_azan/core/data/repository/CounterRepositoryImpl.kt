package com.github.meypod.al_azan.core.data.repository

import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.domain.repository.CounterRepository
import com.github.meypod.al_azan.core.util.storage.MMKVDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class CounterRepositoryImpl(
    private val counterStoreDatastore: MMKVDataStore<List<Counter>>,
) : CounterRepository {
    override val data: Flow<List<Counter>> =
        counterStoreDatastore.data

    override suspend fun fetch(): List<Counter> = data.first()

    override suspend fun update(transform: suspend (t: List<Counter>) -> List<Counter>) {
        counterStoreDatastore.update(transform)
    }
}
