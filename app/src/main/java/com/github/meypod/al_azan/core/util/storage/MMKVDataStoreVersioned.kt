package com.github.meypod.al_azan.core.util.storage

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

/**
 * same as [MMKVDataStore], but calls the passed migrate function
 * with the stored version of the data. does not call migrate if no data is present,
 * instead it initializes with default value and current version
 */
class MMKVDataStoreVersioned<T>(
    private val mmkv: MMKV,
    private val key: String,
    private val serializer: KSerializer<T>,
    private val defaultValue: T,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val version: Int = 1,
    private val migrate: (storedVersion: Int, currentVersion: Int, storedData: JsonObject) -> JsonObject,
) : SimpleJsonDataStore<T> {
    private val state: MutableStateFlow<T> = MutableStateFlow(loadSync())
    private val updater =
        OptimisticCommitUpdater(state) { newValue ->
            val success = mmkv.encode(key, serializeForStorage(newValue))
            if (!success) {
                throw IllegalStateException("MMKV.encode() returned false for key=$key")
            }
        }

    override val data: StateFlow<T> = state.asStateFlow()

    private fun loadSync(): T {
        var jsonString = mmkv.decodeString(key, null) ?: return defaultValue
        return try {
            val element = json.decodeFromString<JsonElement>(jsonString)
            if (element !is JsonObject) {
                return defaultValue
            }
            val storedData = element["state"]
            val storedVersion: Int = element["version"]?.jsonPrimitive?.int ?: version
            if (storedData !is JsonObject) {
                return defaultValue
            }
            if (storedVersion < version) {
                val newState = migrate(storedVersion, version, storedData)
                jsonString = json.encodeToString(newState)
                val success = mmkv.encode(key, jsonString)
                if (!success) {
                    throw IllegalStateException("MMKV.encode() returned false for key=$key")
                }
                mmkv.sync()
                json.decodeFromJsonElement(serializer, newState)
            } else {
                json.decodeFromJsonElement(serializer, storedData)
            }
        } catch (e: Exception) {
            throw RuntimeException("Unknown error when decoding stored data", e)
        }
    }

    private fun serializeForStorage(value: T): String {
        val versionedStore = VersionedData(value, version)
        val serialized = json.encodeToString(VersionedData.serializer(serializer), versionedStore)
        return serialized
    }

    override suspend fun update(transform: (T) -> T) {
        updater.update(transform)
    }

    override suspend fun getStoredJsonString(): String =
        withContext(Dispatchers.IO) {
            mmkv.decodeString(key) ?: serializeForStorage(defaultValue)
        }
}
