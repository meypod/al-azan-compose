package com.github.meypod.al_azan

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.github.meypod.al_azan.di.MigrationEntryPoint
import com.tencent.mmkv.MMKV
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltAndroidApp
class App :
    Application(),
    Configuration.Provider {
    companion object {
        private const val TAG = "App"
        private const val SETTINGS_STORAGE = "SETTINGS_STORAGE"
        private const val MIGRATED_TO_V2 = "MIGRATED_TO_V2"
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        val mmkv = MMKV.defaultMMKV()
        if (mmkv.contains(SETTINGS_STORAGE) && !mmkv.contains(MIGRATED_TO_V2)) {
            Log.i(TAG, "Legacy settings detected. Starting v2 migration.")
            val migrationResult = runCatching {
                runBlocking {
                    val migrationEntryPoint =
                        EntryPointAccessors.fromApplication(this@App, MigrationEntryPoint::class.java)
                    migrationEntryPoint.repositoryMigrationRunner().run()
                }
            }
            migrationResult.onSuccess {
                Log.i(TAG, "v2 migration completed successfully.")
            }
            migrationResult.onFailure { error ->
                Log.e(TAG, "v2 migration failed. Will start with fresh data.", error)
            }
            mmkv.encode(MIGRATED_TO_V2, true)
        }
//        createNotificationChannels(applicationContext)
    }
}
