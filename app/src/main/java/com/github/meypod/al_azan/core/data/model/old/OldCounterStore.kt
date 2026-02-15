package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.domain.model.counter.Counter
import com.github.meypod.al_azan.core.util.serialization.EmptyStringAsNullSerializer
import kotlinx.serialization.Serializable

@Serializable
data class OldCounterStore(
    val state: OldCounterStoreState,
    val version: Int,
)

@Serializable
data class OldCounterStoreState(
    val counters: List<OldCounter> = emptyList(),
)

@Serializable
data class OldCounter(
    val id: String,
    @Serializable(with = EmptyStringAsNullSerializer::class) val label: String? = null,
    val count: Int,
    val lastModified: Long? = null,
    val lastCount: Int? = null,
)

fun OldCounter.toCounter(): Counter =
    Counter(
        this.id,
        this.label,
        this.count,
        this.lastModified,
        this.lastCount,
    )
