package com.github.meypod.al_azan.core.domain.repository

import android.net.Uri

/**
 * Reads and writes the app's full state as a single JSON backup file at a user-chosen content [Uri].
 */
interface BackupRepository {
    /** Serializes every store into one JSON document and writes it to [uri]. */
    suspend fun exportTo(uri: Uri)

    /**
     * Whether the old React-Native app's data is still present on this device. Only then can a legacy
     * export be produced, so the UI uses this to decide whether to offer it.
     */
    fun hasLegacyData(): Boolean

    /**
     * Dumps the old React-Native app's still-present MMKV stores verbatim into its backup format and
     * writes it to [uri]. A recovery hatch: lets a user who upgraded unknowingly produce a file the old
     * app can import, so they can roll back. Only valid when [hasLegacyData] is true.
     */
    suspend fun exportLegacyTo(uri: Uri)

    /**
     * Overwrites all stores with the contents of the backup file at [uri].
     *
     * Accepts both the current v2 format and the legacy React-Native format (auto-detected), and
     * applies the restored app locale. Throws if the file can't be read or isn't a recognized backup.
     */
    suspend fun restoreFrom(uri: Uri)
}
