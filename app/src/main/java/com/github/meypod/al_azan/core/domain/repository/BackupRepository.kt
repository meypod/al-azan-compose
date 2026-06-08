package com.github.meypod.al_azan.core.domain.repository

import android.net.Uri

/**
 * Reads and writes the app's full state as a single JSON backup file at a user-chosen content [Uri].
 */
interface BackupRepository {
    /** Serializes every store into one JSON document and writes it to [uri]. */
    suspend fun exportTo(uri: Uri)

    /**
     * Overwrites all stores with the contents of the backup file at [uri].
     *
     * Accepts both the current v2 format and the legacy React-Native format (auto-detected), and
     * applies the restored app locale. Throws if the file can't be read or isn't a recognized backup.
     */
    suspend fun restoreFrom(uri: Uri)
}
