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
  private val _state: MutableStateFlow<T> = MutableStateFlow(loadSync())

  override val data: StateFlow<T> = _state.asStateFlow()

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
        mmkv.encode(key, jsonString)
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

  override suspend fun update(transform: suspend (T) -> T) {
    val newValue = transform(_state.value)
    withContext(Dispatchers.IO) {
      mmkv.encode(key, serializeForStorage(newValue))
      _state.value = newValue
    }
  }

  override suspend fun getVersionedDataJsonString(): String {
    return withContext(Dispatchers.IO) {
      mmkv.decodeString(key) ?: serializeForStorage(defaultValue)
    }
  }
}
