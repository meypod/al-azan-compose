package com.github.meypod.al_azan.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppFunctionsEntryPoint {
    fun prayerTimesAppFunctions(): PrayerTimesAppFunctions
}

/**
 * Entry point the system binds to run the app's AppFunctions. The compiler generates the concrete
 * [PrayerTimesAppFunctionService] declared in the manifest, and the function list it reads from assets.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AppFunctionServiceEntryPoint(
    serviceName = "PrayerTimesAppFunctionService",
    appFunctionXmlFileName = "prayer_times_app_functions",
)
abstract class BasePrayerTimesAppFunctionService : AppFunctionService() {
    private val prayerTimes by lazy {
        EntryPointAccessors.fromApplication(applicationContext, AppFunctionsEntryPoint::class.java).prayerTimesAppFunctions()
    }

    /**
     * Returns the Islamic prayer times (adhan times) for a given Gregorian date,
     * computed for the user's configured location and calculation method.
     *
     * Use this to answer questions such as "what time is Fajr tomorrow" or "when is
     * Maghrib on 2026-06-20". The result includes Fajr, Sunrise, Dhuhr, Asr, Sunset,
     * Maghrib, Isha, Midnight and Tahajjud, each with a local clock time and a full
     * UTC timestamp.
     *
     * @param date The Gregorian date to compute prayer times for, in ISO yyyy-MM-dd
     *   format, for example 2026-06-20.
     * @return The prayer times for that date. Fails if the user has not yet configured
     *   a location and calculation method, or if the date is malformed.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getPrayerTimes(date: String): PrayerTimesResult = prayerTimes.getPrayerTimes(date)
}
