package com.github.meypod.al_azan

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.tencent.mmkv.MMKV
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
        val mmkv = MMKV.defaultMMKV()
        if (mmkv.contains("SETTINGS_STORAGE") && !mmkv.contains("MIGRATED_TO_V2")) {
            // TODO
        }
//        createNotificationChannels(applicationContext)
    }
}
