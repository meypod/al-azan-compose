package com.github.meypod.al_azan.core.util.storage

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * A datastore-like api for mmkv
 * Does not detect cross-process changes
 *
 * Make sure to instantiate this inside [Dispatchers.IO] if you don't want your main thread to be
 * blocked briefly as it loads the data synchronously the first time it gets initialized
 */
class MMKVDataStore<T>(
    private val mmkv: MMKV,
    private val key: String,
    private val serializer: KSerializer<T>,
    private val defaultValue: T,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SimpleJsonDataStore<T> {
    private val state: MutableStateFlow<T> = MutableStateFlow(loadSync())

    override val data: StateFlow<T> = state.asStateFlow()

    private fun loadSync(): T {
        val raw = mmkv.decodeString(key, null) ?: return defaultValue
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            throw RuntimeException("Unknown error when decoding stored data", e)
        }
    }

    override suspend fun update(transform: suspend (T) -> T) {
        val newValue = transform(state.value)
        withContext(Dispatchers.IO) {
            val serialized = json.encodeToString(serializer, newValue)
            mmkv.encode(key, serialized)
            state.value = newValue
        }
    }

    override suspend fun getStoredJsonString(): String =
        withContext(Dispatchers.IO) {
            mmkv.decodeString(key) ?: json.encodeToString(serializer, defaultValue)
        }
}
