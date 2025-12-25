package com.github.meypod.al_azan.core.data.model.old

import com.github.meypod.al_azan.core.util.serialization.EmptyStringAsNullSerializer
import kotlinx.serialization.Serializable

@Serializable
data class OldCounterSettings(
    val state: OldCounterSettingsState,
    val version: Int,
)

@Serializable
data class OldCounterSettingsState(
    val counters: List<OldCounter> = emptyList(),
)

@Serializable
data class OldCounter(
    val id: String,
    @Serializable(with = EmptyStringAsNullSerializer::class) val label: String? = null,
    val count: Int,
    val lastCount: Int? = null,
    val lastModified: Long? = null,
)
