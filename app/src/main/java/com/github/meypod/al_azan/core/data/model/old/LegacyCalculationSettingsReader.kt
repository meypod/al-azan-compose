package com.github.meypod.al_azan.core.data.model.old

import com.tencent.mmkv.MMKV
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Reads the old React-Native app's calculation settings back out of MMKV.
 *
 * The first-launch migration consumes these keys but never deletes them, so on an upgraded device the
 * original values are still there to be re-read. See [LegacyStorageKeys].
 *
 * Undecodable or absent data reads as null rather than throwing: callers are repairing settings that
 * already work well enough to show times, so failing to read the legacy store must leave the user alone,
 * not break them.
 */
fun interface LegacyCalculationSettingsReader {
    fun read(): OldCalculationSettingsState?
}

@Singleton
class MmkvLegacyCalculationSettingsReader
@Inject
constructor(
    private val mmkv: MMKV,
    @param:Named("storage") private val storageJson: Json,
) : LegacyCalculationSettingsReader {
    override fun read(): OldCalculationSettingsState? =
        mmkv.decodeString(LegacyStorageKeys.CALC_SETTINGS)?.let { stored ->
            runCatching {
                storageJson.decodeFromString(OldCalculationSettings.serializer(), stored).state
            }.getOrNull()
        }
}
