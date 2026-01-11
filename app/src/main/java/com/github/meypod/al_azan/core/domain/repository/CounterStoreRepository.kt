package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.counter.CounterStore
import kotlinx.coroutines.flow.Flow

interface CounterStoreRepository {
  val data: Flow<CounterStore>

  suspend fun fetch(): CounterStore

  suspend fun update(transform: suspend (t: CounterStore) -> CounterStore)
}
