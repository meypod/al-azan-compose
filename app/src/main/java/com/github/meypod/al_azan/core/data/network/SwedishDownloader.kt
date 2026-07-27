package com.github.meypod.al_azan.core.data.network
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.github.meypod.al_azan.core.data.model.swedish.SwedishCity
import com.github.meypod.al_azan.core.data.model.swedish.SwedishPrayerMonth
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class SwedishDownloader @Inject constructor(
    private val client: HttpClient,
    @ApplicationContext private val context: Context
) {
    private val baseUrl = "https://raw.githubusercontent.com/Ahmed-Zamouche/ifis-scraper/main/data"
    private val jsonConfig = Json { ignoreUnknownKeys = true }

    private val _downloadSignal = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val downloadSignal = _downloadSignal.asSharedFlow()

    suspend fun getCities(): List<SwedishCity> = withContext(Dispatchers.IO) {
        val localFile = File(context.filesDir, "cities.json")
        try {
            val response: HttpResponse = client.get("$baseUrl/cities.json")
            val jsonString = response.bodyAsText()
            localFile.writeText(jsonString)
            return@withContext jsonConfig.decodeFromString<List<SwedishCity>>(jsonString)
        } catch (e: Exception) {
            if (localFile.exists()) {
                val jsonString = localFile.readText()
                return@withContext jsonConfig.decodeFromString<List<SwedishCity>>(jsonString)
            }
            try {
                val jsonString = context.assets.open("cities.json").bufferedReader().use { it.readText() }
                return@withContext jsonConfig.decodeFromString<List<SwedishCity>>(jsonString)
            } catch (e: Exception) {
                return@withContext emptyList()
            }
        }
    }
    suspend fun getPrayerTimes(cityId: String, month: Int, year: Int): SwedishPrayerMonth? = withContext(Dispatchers.IO) {
        val fileName = "${year}_${cityId}_${month}.json"
        val remoteUrlPath = "$year/$cityId/$month/time.json"
        val localFile = File(context.filesDir, fileName)
        // 1. Check local cache FIRST for offline support and speed
        if (localFile.exists()) {
            try {
                val jsonString = localFile.readText()
                return@withContext jsonConfig.decodeFromString<SwedishPrayerMonth>(jsonString)
            } catch (e: Exception) {
                // proceed to network if local file is corrupted
            }
        }
        // 2. Fetch from network if not cached
        try {
            val response: HttpResponse = client.get("$baseUrl/$remoteUrlPath")
            val jsonString = response.bodyAsText()
            localFile.writeText(jsonString)
            _downloadSignal.tryEmit(Unit)
            return@withContext jsonConfig.decodeFromString<SwedishPrayerMonth>(jsonString)
        } catch (e: Exception) {
            // 3. Fallback to assets if network fails and not cached (e.g. pre-packaged times)
            try {
                val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
                return@withContext jsonConfig.decodeFromString<SwedishPrayerMonth>(jsonString)
            } catch (e: Exception) {
                return@withContext null
            }
        }
    }
    suspend fun prefetchYear(cityId: String, year: Int) = withContext(Dispatchers.IO) {
        val deferreds = (1..12).map { month ->
            async { getPrayerTimes(cityId, month, year) }
        }
        deferreds.awaitAll()
    }

    fun getCitiesSync(): List<SwedishCity> {
        val ctx = appContext ?: return emptyList()
        val localFile = File(ctx.filesDir, "cities.json")
        if (localFile.exists()) {
            val jsonString = localFile.readText()
            return jsonConfig.decodeFromString<List<SwedishCity>>(jsonString)
        }
        try {
            val jsonString = ctx.assets.open("cities.json").bufferedReader().use { it.readText() }
            return jsonConfig.decodeFromString<List<SwedishCity>>(jsonString)
        } catch (e: Exception) {
            return emptyList()
        }
    }
    companion object {
        var appContext: Context? = null

        fun getPrayerTimesSync(cityId: String, month: Int, year: Int): SwedishPrayerMonth? {
            val ctx = appContext ?: return null
            val fileName = "${year}_${cityId}_${month}.json"
            val localFile = File(ctx.filesDir, fileName)
            if (localFile.exists()) {
                val jsonString = localFile.readText()
                val jsonConfig = Json { ignoreUnknownKeys = true }
                return jsonConfig.decodeFromString<SwedishPrayerMonth>(jsonString)
            }
            try {
                val jsonString = ctx.assets.open(fileName).bufferedReader().use { it.readText() }
                val jsonConfig = Json { ignoreUnknownKeys = true }
                return jsonConfig.decodeFromString<SwedishPrayerMonth>(jsonString)
            } catch (e: Exception) {
                return null
            }
        }
    }
    init {
        appContext = context
    }
}
