package com.github.meypod.al_azan.core.util.storage

import kotlinx.coroutines.flow.StateFlow

/**
 * T must be [kotlinx.serialization.Serializable]
 */
interface SimpleJsonDataStore<T> {
  val data: StateFlow<T>
  suspend fun update(transform: suspend (T) -> T)
  suspend fun getVersionedDataJsonString(): String
}
