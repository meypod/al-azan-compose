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
    private val updater = OptimisticCommitUpdater(state)

    override val data: StateFlow<T> = state.asStateFlow()

    private fun loadSync(): T {
        val raw = mmkv.decodeString(key, null) ?: return defaultValue
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            throw RuntimeException("Unknown error when decoding stored data", e)
        }
    }

    private fun serializeForStorage(value: T): String {
        return json.encodeToString(serializer, value)
    }

    override suspend fun update(transform: (T) -> T) {
        updater.update(transform) { newValue ->
            withContext(Dispatchers.IO) {
                val success = mmkv.encode(key, serializeForStorage(newValue))
                if (!success) {
                    throw IllegalStateException("MMKV.encode() returned false for key=$key")
                }
                mmkv.sync()
            }
        }
    }

    override suspend fun getStoredJsonString(): String =
        withContext(Dispatchers.IO) {
            mmkv.decodeString(key) ?: json.encodeToString(serializer, defaultValue)
        }
}
