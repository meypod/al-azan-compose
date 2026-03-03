package com.github.meypod.al_azan.core.data.repository

import android.content.Context
import android.os.SystemClock
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.geo.CityGeoInfo
import com.github.meypod.al_azan.core.domain.model.geo.CountryGeoInfo
import com.github.meypod.al_azan.core.domain.repository.GeoInfoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

class GeoInfoRepositoryImpl
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
) : GeoInfoRepository {

    private companion object {
        const val IDLE_EVICT_AFTER_MS: Long = 10 * 60 * 1000L
    }

    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val accessMutex = Mutex()
    private var lastAccessElapsedMs: Long = System.currentTimeMillis()
    private var evictionJob: Job? = null

    private val countriesMutex = Mutex()
    private val citiesMutex = Mutex()

    @Volatile
    private var cachedCountries: List<CountryGeoInfo>? = null

    @Volatile
    private var cachedAllCities: List<CityGeoInfo>? = null

    private suspend fun touch() {
        accessMutex.withLock {
            lastAccessElapsedMs = System.currentTimeMillis()

            evictionJob?.cancel()
            evictionJob =
                cacheScope.launch {
                    delay(IDLE_EVICT_AFTER_MS)

                    val shouldEvict =
                        accessMutex.withLock {
                            System.currentTimeMillis() - lastAccessElapsedMs >= IDLE_EVICT_AFTER_MS
                        }
                    if (shouldEvict) {
                        clearCaches()
                    }
                }
        }
    }

    private suspend fun clearCaches() {
        countriesMutex.withLock {
            cachedCountries = null
        }
        citiesMutex.withLock {
            cachedAllCities = null
        }
    }

    private suspend fun loadCountriesOnce(): List<CountryGeoInfo> {
        cachedCountries?.let { return it }
        return countriesMutex.withLock {
            cachedCountries?.let { return@withLock it }
            val loaded =
                withContext(Dispatchers.IO) {
                    context.resources
                        .openRawResource(R.raw.countries_haystack)
                        .bufferedReader()
                        .useLines { lines ->
                            lines.mapNotNull { line ->
                                val pipeIndex = line.indexOf('|')
                                if (pipeIndex <= 0 || pipeIndex == line.lastIndex) return@mapNotNull null

                                val code = line.substring(0, pipeIndex)
                                val names = line.substring(pipeIndex + 1)

                                CountryGeoInfo(
                                    code = code,
                                    names = names,
                                    name = names.substringBefore(','),
                                )
                            }.toList()
                        }
                }
            cachedCountries = loaded
            loaded
        }
    }

    private suspend fun loadAllCitiesOnce(): List<CityGeoInfo> {
        cachedAllCities?.let { return it }
        return citiesMutex.withLock {
            cachedAllCities?.let { return@withLock it }
            val loaded =
                withContext(Dispatchers.IO) {
                    context.resources
                        .openRawResource(R.raw.cities_haystack)
                        .bufferedReader()
                        .useLines { lines ->
                            lines.mapNotNull { line ->
                                val p1 = line.indexOf('|')
                                if (p1 <= 0) return@mapNotNull null
                                val p2 = line.indexOf('|', startIndex = p1 + 1)
                                if (p2 <= p1 + 1) return@mapNotNull null
                                val p3 = line.indexOf('|', startIndex = p2 + 1)
                                if (p3 <= p2 + 1 || p3 == line.lastIndex) return@mapNotNull null

                                val names = line.substring(0, p1)
                                val lat = line.substring(p1 + 1, p2).toDouble()
                                val lng = line.substring(p2 + 1, p3).toDouble()
                                val country = line.substring(p3 + 1)

                                CityGeoInfo(
                                    name = names.substringBefore(','),
                                    names = names,
                                    lat = lat,
                                    long = lng,
                                    country = country,
                                )
                            }.toList()
                        }
                }
            cachedAllCities = loaded
            loaded
        }
    }

    override suspend fun getCountries(): List<CountryGeoInfo> = touch().let { loadCountriesOnce() }

    override suspend fun getCities(): List<CityGeoInfo> = touch().let { loadAllCitiesOnce() }

    override suspend fun getCities(countryCode: String): List<CityGeoInfo> =
        touch().let {
            val code = countryCode.trim().uppercase(Locale.ROOT)
            if (code.isEmpty()) return@let emptyList()
            loadAllCitiesOnce().filter { it.country == code }
        }
}
