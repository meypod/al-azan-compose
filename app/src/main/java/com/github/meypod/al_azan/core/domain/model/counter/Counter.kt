package com.github.meypod.al_azan.core.domain.model.counter

import kotlinx.serialization.Serializable

@Serializable
data class Counter(
    val id: String,
    val label: String? = null,
    val count: Int,
    val lastModified: Long? = null,
    val lastCount: Int? = null,
)
