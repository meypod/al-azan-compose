package com.github.meypod.al_azan.core.util.storage

import kotlinx.serialization.Serializable

@Serializable
data class VersionedData<V>(
    val data: V,
    val version: Int,
)
