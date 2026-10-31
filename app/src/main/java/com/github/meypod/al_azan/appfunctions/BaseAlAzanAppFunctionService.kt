package com.github.meypod.al_azan.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppFunctionsEntryPoint {
    fun prayerTimesAppFunctions(): PrayerTimesAppFunctions

    fun calendarAppFunctions(): CalendarAppFunctions
}

/**
 * Entry point the system binds to run the app's AppFunctions. The compiler generates the concrete
 * [AlAzanAppFunctionService] declared in the manifest, and the function list it reads from assets.
 */
@RequiresApi(Build.VERSION_CODES.BAKLAVA)
@AppFunctionServiceEntryPoint(
    serviceName = "AlAzanAppFunctionService",
    appFunctionXmlFileName = "app_functions",
)
abstract class BaseAlAzanAppFunctionService : AppFunctionService() {
    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(applicationContext, AppFunctionsEntryPoint::class.java)
    }

    /**
     * Returns the Islamic prayer times (adhan times) for a given Gregorian date,
     * computed for the user's configured location and calculation method.
     *
     * Use this to answer questions such as "what time is Fajr tomorrow" or "when is
     * Maghrib on 2026-06-20". The result includes Fajr, Sunrise, Dhuhr, Asr, Sunset,
     * Maghrib, Isha, Midnight and Tahajjud, each with a local clock time and a full
     * UTC timestamp. For a date the user gives in another calendar, call convertDate
     * first and pass its gregorianDate.
     *
     * @param date The Gregorian date to compute prayer times for, in ISO yyyy-MM-dd
     *   format, for example 2026-06-20.
     * @return The prayer times for that date. Fails if the user has not yet configured
     *   a location and calculation method, or if the date is malformed.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun getPrayerTimes(date: String): PrayerTimesResult = entryPoint.prayerTimesAppFunctions().getPrayerTimes(date)

    /**
     * Converts a date from one calendar to another, for example a Persian or Hijri date the
     * user mentions into the Gregorian date other functions take, or today's Gregorian date
     * into the Hijri calendar.
     *
     * The hijri calendar follows the Islamic calendar type and day adjustment the user chose
     * in the app, so results match the Hijri dates the app shows.
     *
     * @param fromCalendar The calendar of the given date: gregorian, persian (Solar Hijri),
     *   ethiopic, buddhist or hijri (Islamic lunar).
     * @param year The year in fromCalendar, e.g. 1405 for Persian or 1448 for Hijri.
     * @param month The month in fromCalendar, starting at 1 (1 is Farvardin in Persian and
     *   Muharram in Hijri; Ethiopic has a 13th month).
     * @param day The day of the month in fromCalendar, starting at 1.
     * @param toCalendar The calendar to convert to, from the same list as fromCalendar.
     * @return The date in toCalendar, with its month name and the matching Gregorian date.
     *   Fails if the date does not exist in fromCalendar, e.g. day 30 of a 29-day month.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true)
    suspend fun convertDate(
        @AppFunctionStringValueConstraint(enumValues = ["gregorian", "persian", "ethiopic", "buddhist", "hijri"])
        fromCalendar: String,
        year: Int,
        month: Int,
        day: Int,
        @AppFunctionStringValueConstraint(enumValues = ["gregorian", "persian", "ethiopic", "buddhist", "hijri"])
        toCalendar: String,
    ): ConvertedDate = entryPoint.calendarAppFunctions().convertDate(fromCalendar, year, month, day, toCalendar)
}
