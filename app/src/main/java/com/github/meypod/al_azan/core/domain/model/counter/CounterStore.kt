package com.github.meypod.al_azan.core.domain.model.counter

import kotlinx.serialization.Serializable

@Serializable
data class CounterStore(val counters: List<Counter>)
