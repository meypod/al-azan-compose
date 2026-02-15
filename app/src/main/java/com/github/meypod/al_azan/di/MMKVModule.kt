package com.github.meypod.al_azan.di

import com.tencent.mmkv.MMKV
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MMKVModule {
    @Provides
    @Singleton
    fun provideMmkv(): MMKV = MMKV.defaultMMKV()

    @Provides
    @Singleton
    @Named("storage")
    fun provideStorageJson(): Json = Json { ignoreUnknownKeys = true }
}
