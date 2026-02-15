package com.github.meypod.al_azan.core.domain.repository

import com.github.meypod.al_azan.core.domain.model.counter.Counter
import kotlinx.coroutines.flow.Flow

interface CounterRepository {
    val data: Flow<List<Counter>>

    suspend fun fetch(): List<Counter>

    suspend fun update(transform: suspend (t: List<Counter>) -> List<Counter>)
}
